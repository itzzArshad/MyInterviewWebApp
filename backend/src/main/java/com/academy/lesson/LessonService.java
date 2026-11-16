package com.academy.lesson;

import com.academy.curriculum.*;
import com.academy.github.GitHubContentClient;
import com.academy.github.GitHubContentClient.GitHubException;
import com.academy.github.GitHubProperties;
import com.academy.lesson.LessonDtos.*;
import com.academy.lesson.LessonParser.ParsedLesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Lesson authoring: the master prompt for a topic, live preview of a paste, save (create/update),
 * delete, and mirroring every change to the GitHub content repo.
 *
 * The database is what the app reads; GitHub is the durable, versioned copy (and the restore source).
 * A GitHub failure never loses the save — the lesson is kept and marked "not synced" for retry.
 */
@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final ConceptRepository concepts;
    private final ChapterRepository chapters;
    private final SubjectRepository subjects;
    private final LessonParser parser;
    private final LessonWriter writer;
    private final GitHubContentClient github;
    private final ResourceLoader resources;

    public LessonService(ConceptRepository concepts, ChapterRepository chapters, SubjectRepository subjects,
                         LessonParser parser, LessonWriter writer, GitHubContentClient github, ResourceLoader resources) {
        this.concepts = concepts;
        this.chapters = chapters;
        this.subjects = subjects;
        this.parser = parser;
        this.writer = writer;
        this.github = github;
        this.resources = resources;
    }

    private record Located(Concept concept, Chapter chapter, Subject subject) {
        LessonKind kind() {
            return LessonKind.forChapter(chapter.getSlug());
        }

        String githubPath() {
            return subject.getSlug() + "/" + chapter.getSlug() + "/" + concept.getSlug() + ".md";
        }
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public Source source(String slug) {
        Located l = locate(slug);
        Concept c = l.concept();
        return new Source(c.getSlug(), c.getTitle(), l.kind().name(), c.getStatus() == Concept.Status.PUBLISHED,
                c.getContentSource(), Objects.requireNonNullElse(c.getSourceMd(), ""), syncView(c));
    }

    @Transactional(readOnly = true)
    public Prompt prompt(String slug) {
        Located l = locate(slug);
        String neighbours = concepts.findAllSummaries().stream()
                .filter(s -> s.chapterId().equals(l.chapter().getId()) && !s.slug().equals(slug))
                .map(ConceptSummary::title)
                .collect(Collectors.joining(", "));
        String text = resource("prompts/lesson-prompt.md")
                .replace("{{SECTIONS}}", resource(l.kind().sectionsResource()).strip())
                .replace("{{TITLE}}", l.concept().getTitle())
                .replace("{{SUBJECT}}", l.subject().getTitle())
                .replace("{{CHAPTER}}", l.chapter().getTitle())
                .replace("{{KIND}}", l.kind().label())
                .replace("{{NEIGHBOURS}}", neighbours.isBlank() ? "(none)" : neighbours);
        return new Prompt(slug, l.kind().name(), text);
    }

    public Preview preview(String slug, String markdown) {
        locate(slug); // 404 for unknown topics
        return toPreview(parser.parse(markdown, LessonKind.SECTIONS));
    }

    // ---------------------------------------------------------------- write

    @Transactional
    public SaveResult save(String slug, String markdown) {
        Located l = locate(slug);
        ParsedLesson parsed = parser.parse(markdown, LessonKind.SECTIONS);
        if (!parsed.valid()) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                    "The lesson has " + parsed.errors().size() + " problem(s). Fix them and save again.");
            pd.setProperty("errors", parsed.errors());
            pd.setProperty("warnings", parsed.warnings());
            throw new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, pd, null);
        }
        boolean existed = l.concept().getStatus() == Concept.Status.PUBLISHED;
        Concept saved = writer.publish(l.concept(), parsed, Concept.ContentSource.USER);
        pushToGitHub(new Located(saved, l.chapter(), l.subject()), (existed ? "Update" : "Add") + " lesson: " + saved.getTitle());
        return new SaveResult(slug, parsed.questions().size(), parsed.warnings(), syncView(saved));
    }

    @Transactional
    public void delete(String slug) {
        Located l = locate(slug);
        if (l.concept().getStatus() != Concept.Status.PUBLISHED) {
            throw new ResponseStatusException(NOT_FOUND, "This topic has no lesson to delete.");
        }
        String path = Objects.requireNonNullElse(l.concept().getGithubPath(), l.githubPath());
        writer.unpublish(l.concept());
        if (github.enabled()) {
            try {
                github.delete(path, "Delete lesson: " + l.concept().getTitle());
                l.concept().clearGithub();
            } catch (GitHubException e) {
                log.warn("Lesson '{}' deleted locally but not on GitHub: {}", slug, e.getMessage());
                l.concept().markSyncFailed("Deleted in the app but not on GitHub: " + e.getMessage());
            }
        }
    }

    @Transactional
    public SyncView sync(String slug) {
        Located l = locate(slug);
        if (l.concept().getStatus() != Concept.Status.PUBLISHED) throw new ResponseStatusException(NOT_FOUND, "No lesson to sync.");
        if (!github.enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "GitHub sync is not configured.");
        pushToGitHub(l, "Sync lesson: " + l.concept().getTitle());
        return syncView(l.concept());
    }

    // ---------------------------------------------------------------- whole-repo operations

    public GitHubStatus githubStatus() {
        GitHubProperties p = github.properties();
        return new GitHubStatus(p.enabled(), p.enabled() ? p.repo() : null, p.branch(),
                p.enabled() ? "https://github.com/" + p.repo() : null);
    }

    /** Pushes every published lesson (seed and your own) — a full backup into the repo. */
    @Transactional
    public BulkResult pushAll() {
        requireGitHub();
        int ok = 0, failed = 0;
        List<String> messages = new ArrayList<>();
        for (Concept c : concepts.findAllByStatusNot(Concept.Status.ARCHIVED)) {
            if (c.getStatus() != Concept.Status.PUBLISHED || c.getSourceMd() == null) continue;
            Located l = locate(c);
            if (c.getGithubError() == null && c.getGithubSyncedAt() != null && l.githubPath().equals(c.getGithubPath())
                    && c.getGithubSyncedAt().isAfter(c.getUpdatedAt())) {
                ok++;
                continue; // already up to date
            }
            if (pushToGitHub(l, "Backup lesson: " + c.getTitle())) ok++;
            else {
                failed++;
                messages.add(c.getSlug() + ": " + c.getGithubError());
            }
        }
        return new BulkResult(ok, failed, messages);
    }

    /** Re-imports every lesson file from the repo (disaster recovery / editing directly on GitHub). */
    @Transactional
    public BulkResult pullAll() {
        requireGitHub();
        int ok = 0, failed = 0;
        List<String> messages = new ArrayList<>();
        for (Map.Entry<String, String> f : github.listFiles(".md").entrySet()) {
            String path = f.getKey();
            String slug = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.md$", "");
            Optional<Concept> concept = concepts.findBySlug(slug).filter(c -> c.getStatus() != Concept.Status.ARCHIVED);
            if (concept.isEmpty()) {
                if (!path.equalsIgnoreCase("README.md")) messages.add(path + ": skipped (no topic with slug '" + slug + "')");
                continue;
            }
            ParsedLesson parsed = parser.parse(github.read(path), LessonKind.SECTIONS);
            if (!parsed.valid()) {
                failed++;
                messages.add(path + ": " + String.join("; ", parsed.errors()));
                continue;
            }
            Concept c = concept.get();
            if (!LessonWriter.hash(parsed.source()).equals(c.getContentHash()) || c.getStatus() != Concept.Status.PUBLISHED) {
                c = writer.publish(c, parsed, Concept.ContentSource.USER);
            }
            c.markSynced(path, f.getValue());
            ok++;
        }
        return new BulkResult(ok, failed, messages);
    }

    // ---------------------------------------------------------------- helpers

    /** @return true if the push succeeded */
    private boolean pushToGitHub(Located l, String message) {
        Concept c = l.concept();
        if (!github.enabled()) return false;
        String path = l.githubPath();
        try {
            // If the chapter changed, remove the file at the old location first.
            if (c.getGithubPath() != null && !c.getGithubPath().equals(path)) {
                github.delete(c.getGithubPath(), "Move lesson: " + c.getTitle());
                c.clearGithub();
            }
            String sha = github.put(path, c.getSourceMd(), message, c.getGithubSha());
            c.markSynced(path, sha);
            return true;
        } catch (GitHubException | IllegalArgumentException e) {
            log.warn("GitHub sync failed for '{}': {}", c.getSlug(), e.getMessage());
            c.markSyncFailed(e.getMessage());
            return false;
        }
    }

    public SyncView syncView(Concept c) {
        if (!github.enabled()) return new SyncView("DISABLED", null, null, null, null);
        String url = c.getGithubPath() == null ? null : github.properties().webUrl(c.getGithubPath());
        if (c.getGithubError() != null) return new SyncView("FAILED", c.getGithubPath(), url, c.getGithubSyncedAt(), c.getGithubError());
        if (c.getStatus() != Concept.Status.PUBLISHED) return new SyncView("NOT_SYNCED", null, null, null, null);
        boolean upToDate = c.getGithubSyncedAt() != null && !c.getGithubSyncedAt().isBefore(c.getUpdatedAt());
        return new SyncView(upToDate ? "SYNCED" : "NOT_SYNCED", c.getGithubPath(), url, c.getGithubSyncedAt(), null);
    }

    private Preview toPreview(ParsedLesson p) {
        Map<String, Long> byType = p.questions().stream()
                .collect(Collectors.groupingBy(q -> q.type().name(), TreeMap::new, Collectors.counting()));
        return new Preview(p.valid(), p.difficulty(), p.minutes(), p.summary(), p.numberedSections(), LessonKind.SECTIONS,
                p.questions().size(), byType, p.body(), p.errors(), p.warnings());
    }

    private Located locate(String slug) {
        return locate(concepts.findBySlug(slug)
                .filter(c -> c.getStatus() != Concept.Status.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Topic not found")));
    }

    private Located locate(Concept c) {
        Chapter chapter = chapters.findById(c.getChapterId()).orElseThrow();
        Subject subject = subjects.findById(chapter.getSubjectId()).orElseThrow();
        return new Located(c, chapter, subject);
    }

    private void requireGitHub() {
        if (!github.enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "GitHub sync is not configured.");
    }

    private String resource(String path) {
        try (var in = resources.getResource("classpath:" + path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing " + path, e);
        }
    }
}
