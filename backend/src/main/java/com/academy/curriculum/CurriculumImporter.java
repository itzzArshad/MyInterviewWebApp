package com.academy.curriculum;

import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Loads the curriculum from {@code classpath:curriculum/} into the database at startup.
 *
 * <pre>
 * curriculum/curriculum.yml                  hierarchy: paths → subjects → chapters → concepts
 * curriculum/{subject}/{concept}.md          lesson (YAML front matter + Markdown body)
 * curriculum/{subject}/{concept}.quiz.yml    quiz questions
 * </pre>
 *
 * Idempotent: rows are matched by slug / question key, unchanged lessons are skipped by content hash,
 * and nothing that user progress refers to is ever deleted (removed items are archived / deactivated).
 * Any malformed content fails startup loudly rather than silently showing broken lessons.
 */
@Component
public class CurriculumImporter implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CurriculumImporter.class);
    private static final String ROOT = "classpath:curriculum/";

    private final ResourceLoader resources;
    private final PathRepository paths;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final ConceptRepository concepts;
    private final QuizQuestionRepository questions;
    private final ObjectMapper json;

    public CurriculumImporter(ResourceLoader resources, PathRepository paths, SubjectRepository subjects,
                              ChapterRepository chapters, ConceptRepository concepts,
                              QuizQuestionRepository questions, ObjectMapper json) {
        this.resources = resources;
        this.paths = paths;
        this.subjects = subjects;
        this.chapters = chapters;
        this.concepts = concepts;
        this.questions = questions;
        this.json = json;
    }

    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        Map<String, Object> root = yaml(read(ROOT + "curriculum.yml"));
        int pathOrder = 0, subjectOrder = 0, chapterOrder = 0, conceptOrder = 0, published = 0;
        Set<String> seenConcepts = new HashSet<>();

        for (Map<String, Object> p : list(root, "paths")) {
            LearningPath path = paths.findBySlug(str(p, "slug")).orElseGet(() -> new LearningPath(str(p, "slug")));
            path.setTitle(str(p, "title"));
            path.setDescription(optStr(p, "description"));
            path.setSortOrder(pathOrder++);
            path = paths.save(path);

            for (Map<String, Object> s : list(p, "subjects")) {
                String subjectSlug = str(s, "slug");
                Subject subject = subjects.findBySlug(subjectSlug).orElseGet(() -> new Subject(subjectSlug));
                subject.setPathId(path.getId());
                subject.setTitle(str(s, "title"));
                subject.setDescription(optStr(s, "description"));
                subject.setSortOrder(subjectOrder++);
                subject = subjects.save(subject);

                for (Map<String, Object> c : list(s, "chapters")) {
                    Long subjectId = subject.getId();
                    Chapter chapter = chapters.findBySubjectIdAndSlug(subjectId, str(c, "slug"))
                            .orElseGet(() -> new Chapter(subjectId, str(c, "slug")));
                    chapter.setTitle(str(c, "title"));
                    chapter.setSortOrder(chapterOrder++);
                    chapter = chapters.save(chapter);

                    for (Object entry : (List<?>) c.getOrDefault("concepts", List.of())) {
                        // Either "slug: Title" shorthand or a map {slug, title}
                        Map<String, Object> cm = asConceptEntry(entry);
                        String slug = str(cm, "slug");
                        if (!seenConcepts.add(slug)) throw new IllegalStateException("Duplicate concept slug: " + slug);
                        boolean isPublished = importConcept(chapter.getId(), subjectSlug, slug, str(cm, "title"), conceptOrder++);
                        if (isPublished) published++;
                    }
                }
            }
        }

        // Concepts removed from curriculum.yml are archived, not deleted (progress may reference them).
        for (Concept c : concepts.findAllByStatusNot(Concept.Status.ARCHIVED)) {
            if (!seenConcepts.contains(c.getSlug())) {
                c.setStatus(Concept.Status.ARCHIVED);
                log.info("Archived concept '{}' (no longer in curriculum.yml)", c.getSlug());
            }
        }
        log.info("Curriculum loaded: {} concepts ({} published)", seenConcepts.size(), published);
    }

    /** @return true when the concept has a published lesson */
    private boolean importConcept(Long chapterId, String subjectSlug, String slug, String title, int order) {
        Concept concept = concepts.findBySlug(slug).orElseGet(() -> new Concept(slug));
        concept.setChapterId(chapterId);
        concept.setTitle(title);
        concept.setSortOrder(order);

        Resource lesson = resources.getResource(ROOT + subjectSlug + "/" + slug + ".md");
        if (!lesson.exists()) {
            concept.setStatus(Concept.Status.PLANNED);
            concepts.save(concept);
            return false;
        }

        String raw = read(lesson);
        Resource quizRes = resources.getResource(ROOT + subjectSlug + "/" + slug + ".quiz.yml");
        String quizRaw = quizRes.exists() ? read(quizRes) : "";
        String hash = sha256(raw + "\u0000" + quizRaw);

        if (hash.equals(concept.getContentHash()) && concept.getStatus() == Concept.Status.PUBLISHED) {
            concepts.save(concept); // order/title/chapter may still have changed
            return true;
        }

        String[] parts = splitFrontMatter(raw, slug);
        Map<String, Object> meta = yaml(parts[0]);
        concept.setTitle(Objects.requireNonNullElse(optStr(meta, "title"), title));
        concept.setDifficulty(Concept.Difficulty.valueOf(Objects.requireNonNullElse(optStr(meta, "difficulty"), "BEGINNER")));
        concept.setEstMinutes(meta.get("minutes") instanceof Number n ? n.intValue() : 15);
        concept.setSummary(optStr(meta, "summary"));
        Object related = meta.get("related");
        concept.setRelatedSlugs(related instanceof List<?> l ? String.join(",", l.stream().map(String::valueOf).toList()) : null);
        concept.setBodyMd(parts[1].strip());
        concept.setStatus(Concept.Status.PUBLISHED);
        concept.setContentHash(hash);
        concept.setUpdatedAt(OffsetDateTime.now());
        concept = concepts.save(concept);

        importQuiz(concept.getId(), slug, quizRaw);
        log.debug("Imported lesson '{}'", slug);
        return true;
    }

    private void importQuiz(Long conceptId, String conceptSlug, String quizRaw) {
        Map<String, QuizQuestion> existing = new HashMap<>();
        questions.findByConceptId(conceptId).forEach(q -> existing.put(q.getQuestionKey(), q));
        Set<String> seen = new HashSet<>();

        List<Map<String, Object>> items = quizRaw.isBlank() ? List.of() : list(yaml(quizRaw), "questions");
        int order = 0;
        for (Map<String, Object> m : items) {
            String key = str(m, "key");
            String where = conceptSlug + "#" + key;
            if (!seen.add(key)) throw new IllegalStateException("Duplicate question key " + where);

            QuizQuestion q = existing.getOrDefault(key, new QuizQuestion(conceptId, key));
            QuizQuestion.Type type = QuizQuestion.Type.valueOf(str(m, "type"));
            q.setType(type);
            q.setDifficulty(Concept.Difficulty.valueOf(Objects.requireNonNullElse(optStr(m, "difficulty"), "BEGINNER")));
            q.setPromptMd(str(m, "prompt"));
            q.setCodeSnippet(optStr(m, "code"));
            q.setExplanationMd(str(m, "explanation"));
            q.setSortOrder(order++);
            q.setActive(true);

            List<?> options = m.get("options") instanceof List<?> l ? l : null;
            q.setOptionsJson(options == null ? null : toJson(options.stream().map(String::valueOf).toList()));
            q.setHintsJson(m.get("hints") instanceof List<?> h ? toJson(h.stream().map(String::valueOf).toList()) : null);
            q.setCorrectAnswer(normaliseAnswer(type, options, m.get("answer"), where));
            questions.save(q);
        }
        existing.values().stream().filter(q -> !seen.contains(q.getQuestionKey())).forEach(q -> q.setActive(false));
    }

    /** Validates the declared answer against the question type and stores it in canonical form. */
    private static String normaliseAnswer(QuizQuestion.Type type, List<?> options, Object answer, String where) {
        if (type == QuizQuestion.Type.TRUE_FALSE) {
            if (!(answer instanceof Boolean b)) throw new IllegalStateException(where + ": TRUE_FALSE needs boolean answer");
            return b.toString();
        }
        if (options != null) {
            if (!(answer instanceof Integer i) || i < 0 || i >= options.size()) {
                throw new IllegalStateException(where + ": answer must be a 0-based option index");
            }
            return i.toString();
        }
        if (answer == null) {
            // Free-text questions without a checkable answer are self-graded against the model answer.
            return null;
        }
        return String.valueOf(answer).strip();
    }

    // ---------------------------------------------------------------- helpers

    private static String[] splitFrontMatter(String raw, String slug) {
        String text = raw.replace("\r\n", "\n");
        if (!text.startsWith("---\n")) throw new IllegalStateException(slug + ".md must start with YAML front matter");
        int end = text.indexOf("\n---\n", 4);
        if (end < 0) throw new IllegalStateException(slug + ".md: unterminated front matter");
        return new String[]{text.substring(4, end), text.substring(end + 5)};
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asConceptEntry(Object entry) {
        if (entry instanceof Map<?, ?> m && m.size() == 1 && !m.containsKey("slug")) {
            var e = m.entrySet().iterator().next();
            return Map.of("slug", String.valueOf(e.getKey()), "title", String.valueOf(e.getValue()));
        }
        if (entry instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalStateException("Invalid concept entry: " + entry);
    }

    private Map<String, Object> yaml(String text) {
        Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(text);
        if (!(loaded instanceof Map<?, ?> m)) throw new IllegalStateException("Expected a YAML mapping");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) m;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? List.of() : (List<Map<String, Object>>) v;
    }

    private static String str(Map<String, Object> m, String key) {
        String v = optStr(m, key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing '" + key + "' in " + m.keySet());
        return v;
    }

    private static String optStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private String read(String location) {
        return read(resources.getResource(location));
    }

    private static String read(Resource r) {
        try (InputStream in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + r, e);
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
