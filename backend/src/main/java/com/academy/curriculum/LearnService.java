package com.academy.curriculum;

import com.academy.curriculum.LearnDtos.*;
import com.academy.progress.ConceptProgress;
import com.academy.progress.ConceptProgressRepository;
import com.academy.quiz.QuizAttemptRepository;
import com.academy.quiz.QuizQuestionRepository;
import com.academy.user.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class LearnService {

    private final PathRepository paths;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final ConceptRepository concepts;
    private final ConceptProgressRepository progress;
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final ActivityService activity;
    private final com.academy.lesson.LessonService lessons;

    public LearnService(PathRepository paths, SubjectRepository subjects, ChapterRepository chapters,
                        ConceptRepository concepts, ConceptProgressRepository progress,
                        QuizQuestionRepository questions, QuizAttemptRepository attempts, ActivityService activity,
                        com.academy.lesson.LessonService lessons) {
        this.attempts = attempts;
        this.lessons = lessons;
        this.paths = paths;
        this.subjects = subjects;
        this.chapters = chapters;
        this.concepts = concepts;
        this.progress = progress;
        this.questions = questions;
        this.activity = activity;
    }

    /** Whole curriculum with the user's progress. A fixed number of queries regardless of size (no N+1). */
    public List<PathNode> tree(Long userId) {
        Map<Long, ConceptProgress> prog = progressByConcept(userId);
        Set<Long> quizzed = new HashSet<>(attempts.findQuizzedConceptIds(userId));
        Map<Long, Long> qCounts = questionCounts();
        Map<Long, List<ConceptSummary>> byChapter = concepts.findAllSummaries().stream()
                .collect(Collectors.groupingBy(ConceptSummary::chapterId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Chapter>> chaptersBySubject = chapters.findAllByOrderBySortOrder().stream()
                .collect(Collectors.groupingBy(Chapter::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Subject>> subjectsByPath = subjects.findAllByOrderBySortOrder().stream()
                .collect(Collectors.groupingBy(Subject::getPathId, LinkedHashMap::new, Collectors.toList()));

        List<PathNode> result = new ArrayList<>();
        for (LearningPath p : paths.findAllByOrderBySortOrder()) {
            List<SubjectNode> subjectNodes = new ArrayList<>();
            for (Subject s : subjectsByPath.getOrDefault(p.getId(), List.of())) {
                int completed = 0, published = 0, total = 0;
                List<ChapterNode> chapterNodes = new ArrayList<>();
                for (Chapter c : chaptersBySubject.getOrDefault(s.getId(), List.of())) {
                    List<ConceptNode> conceptNodes = new ArrayList<>();
                    for (ConceptSummary cs : byChapter.getOrDefault(c.getId(), List.of())) {
                        ConceptProgress cp = prog.get(cs.id());
                        ProgressView pv = ProgressView.of(cp);
                        total++;
                        if (cs.published()) published++;
                        if (pv.status() == ConceptProgress.Status.COMPLETED) completed++;
                        conceptNodes.add(new ConceptNode(cs.slug(), cs.title(), cs.difficulty(), cs.estMinutes(),
                                cs.published(), pv.status(), pv.mastery(), pv.bookmarked(),
                                qCounts.getOrDefault(cs.id(), 0L).intValue(), quizzed.contains(cs.id()),
                                cp == null ? null : cp.getLastViewedAt()));
                    }
                    chapterNodes.add(new ChapterNode(c.getSlug(), c.getTitle(), conceptNodes));
                }
                subjectNodes.add(new SubjectNode(s.getSlug(), s.getTitle(), s.getDescription(),
                        completed, published, total, chapterNodes));
            }
            result.add(new PathNode(p.getSlug(), p.getTitle(), p.getDescription(), subjectNodes));
        }
        return result;
    }

    public ConceptDetail concept(Long userId, String slug) {
        Concept c = concepts.findBySlug(slug)
                .filter(x -> x.getStatus() != Concept.Status.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concept not found"));
        Chapter chapter = chapters.findById(c.getChapterId()).orElseThrow();
        Subject subject = subjects.findById(chapter.getSubjectId()).orElseThrow();

        // Prev/next follow the global curriculum order.
        List<ConceptSummary> all = concepts.findAllSummaries();
        int idx = indexOf(all, c.getId());
        Ref prev = idx > 0 ? ref(all.get(idx - 1)) : null;
        Ref next = idx >= 0 && idx < all.size() - 1 ? ref(all.get(idx + 1)) : null;

        Map<String, ConceptSummary> bySlug = all.stream().collect(Collectors.toMap(ConceptSummary::slug, Function.identity()));
        List<RelatedRef> related = c.relatedSlugList().stream()
                .map(bySlug::get).filter(Objects::nonNull)
                .map(r -> new RelatedRef(r.slug(), r.title(), r.published()))
                .toList();

        ProgressView pv = ProgressView.of(progress.findByUserIdAndConceptId(userId, c.getId()).orElse(null));
        LastQuiz lastQuiz = attempts.findFirstByUserIdAndConceptIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(userId, c.getId())
                .map(a -> new LastQuiz(a.getScore(), a.getTotal(), a.getFinishedAt()))
                .orElse(null);
        return new ConceptDetail(c.getSlug(), c.getTitle(), c.getDifficulty(), c.getEstMinutes(), c.getSummary(),
                c.getBodyMd(), c.getStatus() == Concept.Status.PUBLISHED,
                new Ref(subject.getSlug(), subject.getTitle()), new Ref(chapter.getSlug(), chapter.getTitle()),
                pv, prev, next, related, (int) questions.countByConceptIdAndActiveTrue(c.getId()), lastQuiz,
                lessons.syncView(c));
    }

    @Transactional
    public ProgressView markViewed(Long userId, String slug) {
        ConceptProgress p = progressFor(userId, requirePublished(slug).getId());
        p.markViewed();
        activity.touchToday(userId);
        return ProgressView.of(progress.save(p));
    }

    @Transactional
    public ProgressView update(Long userId, String slug, ProgressUpdate req) {
        ConceptProgress p = progressFor(userId, requirePublished(slug).getId());
        if (req.bookmarked() != null) p.setBookmarked(req.bookmarked());
        if (req.completed() != null) {
            boolean wasCompleted = p.getStatus() == ConceptProgress.Status.COMPLETED;
            if (!wasCompleted && req.completed()) p.markViewed();
            p.setCompleted(req.completed());
            if (!wasCompleted && req.completed()) activity.recordCompletion(userId);
        }
        return ProgressView.of(progress.save(p));
    }

    // ---------------------------------------------------------------- helpers shared with other services

    public Concept requirePublished(String slug) {
        return concepts.findBySlug(slug)
                .filter(c -> c.getStatus() == Concept.Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concept not found"));
    }

    public ConceptProgress progressFor(Long userId, Long conceptId) {
        return progress.findByUserIdAndConceptId(userId, conceptId).orElseGet(() -> new ConceptProgress(userId, conceptId));
    }

    public Map<Long, ConceptProgress> progressByConcept(Long userId) {
        return progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(ConceptProgress::getConceptId, Function.identity()));
    }

    private Map<Long, Long> questionCounts() {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] row : questions.countActiveByConcept()) m.put((Long) row[0], (Long) row[1]);
        return m;
    }

    private static int indexOf(List<ConceptSummary> all, Long id) {
        for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(id)) return i;
        return -1;
    }

    private static Ref ref(ConceptSummary s) {
        return new Ref(s.slug(), s.title());
    }
}
