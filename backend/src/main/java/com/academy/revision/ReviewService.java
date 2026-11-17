package com.academy.revision;

import com.academy.common.AppTime;
import com.academy.common.ProgressEvent;
import com.academy.curriculum.Concept;
import com.academy.curriculum.ConceptRepository;
import com.academy.curriculum.CurriculumIndex;
import com.academy.curriculum.CurriculumIndex.Topic;
import com.academy.interview.InterviewItem;
import com.academy.interview.InterviewItemRepository;
import com.academy.interview.KeyPoints;
import com.academy.lesson.LessonParser;
import com.academy.progress.ConceptProgress;
import com.academy.progress.ConceptProgressRepository;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.academy.revision.RevisionDtos.*;
import com.academy.revision.Scheduler.Grade;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Spaced repetition: creates cards from what you study (quiz questions, wrong answers, completed lessons,
 * interview practice), schedules them with SM-2, serves the daily queue, and keeps the mistake journal.
 */
@Service
public class ReviewService {

    public static final int DAILY_GOAL = 20;
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private final ReviewCardRepository cards;
    private final ReviewLogRepository logs;
    private final MistakeRepository mistakes;
    private final ConceptRepository concepts;
    private final QuizQuestionRepository questions;
    private final InterviewItemRepository interviewItems;
    private final ConceptProgressRepository progress;
    private final CurriculumIndex index;
    private final AppTime time;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;

    public ReviewService(ReviewCardRepository cards, ReviewLogRepository logs, MistakeRepository mistakes,
                         ConceptRepository concepts, QuizQuestionRepository questions,
                         InterviewItemRepository interviewItems, ConceptProgressRepository progress,
                         CurriculumIndex index, AppTime time, ApplicationEventPublisher events, ObjectMapper json) {
        this.cards = cards;
        this.logs = logs;
        this.mistakes = mistakes;
        this.concepts = concepts;
        this.questions = questions;
        this.interviewItems = interviewItems;
        this.progress = progress;
        this.index = index;
        this.time = time;
        this.events = events;
        this.json = json;
    }

    // ================================================================ hooks from other features

    /** Every quiz answer feeds revision: wrong → card back tomorrow + mistake journal; right on a new card → scheduled. */
    @Transactional
    public void onQuizAnswered(Long userId, QuizQuestion q, boolean correct, String givenAnswer) {
        ReviewCard card = ensure(userId, ReviewCard.Type.QUIZ, "Q:" + q.getId(), q.getConceptId());
        card.setQuestionId(q.getId());
        if (!correct) {
            Scheduler.State s = Scheduler.next(card.state(), Grade.AGAIN);
            card.apply(s, time.now(), time.startOf(time.today().plusDays(1)));
            if (!q.isSelfGraded()) {
                mistakes.findByUserIdAndQuestionId(userId, q.getId()).ifPresentOrElse(
                        m -> m.repeat(givenAnswer),
                        () -> mistakes.save(new Mistake(userId, q.getId(), q.getConceptId(), givenAnswer)));
            }
        } else if (card.isNew()) {
            schedule(card, Grade.GOOD);
        }
    }

    /** Finishing a lesson adds an "explain this concept" card for tomorrow. */
    @Transactional
    public void onConceptCompleted(Long userId, Long conceptId) {
        ReviewCard card = ensure(userId, ReviewCard.Type.EXPLAIN, "E:" + conceptId, conceptId);
        if (card.isNew()) card.setDueAt(time.startOf(time.today().plusDays(1)));
    }

    /** Explaining an interview question schedules it by how many key points you covered. */
    @Transactional
    public void onInterviewPracticed(Long userId, InterviewItem item, int scorePct) {
        ReviewCard card = ensure(userId, ReviewCard.Type.INTERVIEW, "I:" + item.getId(), item.getConceptId());
        card.setInterviewItemId(item.getId());
        schedule(card, Scheduler.gradeForScore(scorePct));
    }

    /** Self-graded quiz questions (EXPLAIN / INTERVIEW type) practised in interview mode. */
    @Transactional
    public void onQuizQuestionExplained(Long userId, QuizQuestion q, int scorePct) {
        ReviewCard card = ensure(userId, ReviewCard.Type.QUIZ, "Q:" + q.getId(), q.getConceptId());
        card.setQuestionId(q.getId());
        schedule(card, Scheduler.gradeForScore(scorePct));
    }

    // ================================================================ queue & grading

    @Transactional(readOnly = true)
    public ReviewQueue queue(Long userId, int limit) {
        CurriculumIndex.Snapshot idx = index.load();
        Set<Long> bookmarked = progress.findByUserId(userId).stream()
                .filter(ConceptProgress::isBookmarked).map(ConceptProgress::getConceptId).collect(Collectors.toSet());
        List<ReviewCard> due = cards.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, time.now(), Limit.of(300)).stream()
                .filter(c -> idx.topic(c.getConceptId()) != null && idx.topic(c.getConceptId()).published())
                .sorted(Comparator.comparing((ReviewCard c) -> !bookmarked.contains(c.getConceptId()))
                        .thenComparing(ReviewCard::getDueAt))
                .limit(Math.max(1, Math.min(limit, 100)))
                .toList();

        Map<Long, QuizQuestion> qs = byId(questions.findAllById(ids(due, ReviewCard::getQuestionId)), QuizQuestion::getId);
        Map<Long, InterviewItem> items = byId(interviewItems.findAllById(ids(due, ReviewCard::getInterviewItemId)), InterviewItem::getId);
        List<CardView> views = new ArrayList<>();
        for (ReviewCard c : due) {
            CardView v = view(c, idx.topic(c.getConceptId()), bookmarked.contains(c.getConceptId()), qs, items);
            if (v != null) views.add(v);
        }
        return new ReviewQueue(views, stats(userId));
    }

    @Transactional
    public GradeResult grade(Long userId, Long cardId, int gradeValue) {
        ReviewCard card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Card not found"));
        Grade g = Grade.of(gradeValue);
        schedule(card, g);
        logs.save(new ReviewLog(card.getId(), userId, g.value, card.getIntervalDays(), time.now()));
        events.publishEvent(new ProgressEvent(userId));
        return new GradeResult(card.getIntervalDays(), card.getDueAt(), label(card.getIntervalDays(), g), stats(userId));
    }

    @Transactional(readOnly = true)
    public Stats stats(Long userId) {
        Map<Long, Double> retention = retentionByConcept(userId);
        Integer overall = retention.isEmpty() ? null
                : (int) Math.round(100 * retention.values().stream().mapToDouble(Double::doubleValue).average().orElse(0));
        return new Stats(cards.countByUserIdAndDueAtLessThanEqual(userId, time.now()),
                logs.countByUserIdAndReviewedAtGreaterThanEqual(userId, time.startOf(time.today())),
                DAILY_GOAL, cards.countByUserId(userId), mistakes.countByUserIdAndResolvedFalse(userId), overall);
    }

    /** Estimated recall per concept (average over reviewed cards); concepts without reviews are absent. */
    @Transactional(readOnly = true)
    public Map<Long, Double> retentionByConcept(Long userId) {
        OffsetDateTime now = time.now();
        Map<Long, List<Double>> r = new HashMap<>();
        for (ReviewCard c : cards.findByUserId(userId)) {
            if (c.getLastReviewedAt() == null) continue;
            double days = Duration.between(c.getLastReviewedAt(), now).toMinutes() / 1440.0;
            r.computeIfAbsent(c.getConceptId(), k -> new ArrayList<>()).add(Scheduler.retrievability(days, c.getIntervalDays()));
        }
        Map<Long, Double> out = new HashMap<>();
        r.forEach((k, v) -> out.put(k, v.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        return out;
    }

    // ================================================================ mistake journal

    @Transactional(readOnly = true)
    public List<MistakeView> mistakes(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        List<Mistake> list = mistakes.findByUserIdOrderByResolvedAscLastWrongAtDesc(userId);
        Map<Long, QuizQuestion> qs = byId(questions.findAllById(list.stream().map(Mistake::getQuestionId).toList()), QuizQuestion::getId);
        List<MistakeView> out = new ArrayList<>();
        for (Mistake m : list) {
            QuizQuestion q = qs.get(m.getQuestionId());
            Topic t = idx.topic(m.getConceptId());
            if (q == null || t == null) continue;
            List<String> opts = strings(q.getOptionsJson());
            out.add(new MistakeView(m.getId(), t.slug(), t.title(), q.getPromptMd(), q.getCodeSnippet(),
                    answerLabel(q, opts, m.getGivenAnswer()), answerLabel(q, opts, q.getCorrectAnswer()),
                    q.getExplanationMd(), m.getTimes(), m.getNote(), m.isResolved(), m.getLastWrongAt()));
        }
        return out;
    }

    @Transactional
    public void updateMistake(Long userId, Long id, MistakeUpdate req) {
        Mistake m = mistakes.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Not found"));
        if (req.note() != null) m.setNote(req.note().isBlank() ? null : req.note().strip());
        if (req.resolved() != null) m.setResolved(req.resolved());
    }

    // ================================================================ rapid revision (cheat sheets)

    @Transactional(readOnly = true)
    public List<SummaryView> summaries(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        Map<Long, ConceptProgress> prog = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(ConceptProgress::getConceptId, Function.identity()));
        Map<Long, Double> retention = retentionByConcept(userId);
        List<SummaryView> out = new ArrayList<>();
        for (Concept c : concepts.findAllByStatusNot(Concept.Status.ARCHIVED)) {
            if (c.getStatus() != Concept.Status.PUBLISHED) continue;
            ConceptProgress p = prog.get(c.getId());
            if (p == null || p.getStatus() == ConceptProgress.Status.NOT_STARTED) continue;
            Topic t = idx.topic(c.getId());
            Double r = retention.get(c.getId());
            out.add(new SummaryView(c.getSlug(), c.getTitle(), t == null ? "" : t.subjectTitle(), c.getSummary(),
                    LessonParser.section(c.getBodyMd(), "summary"), r == null ? null : (int) Math.round(r * 100)));
        }
        out.sort(Comparator.comparing(s -> s.retentionPct() == null ? 101 : s.retentionPct()));
        return out;
    }

    // ================================================================ helpers

    private ReviewCard ensure(Long userId, ReviewCard.Type type, String key, Long conceptId) {
        return cards.findByUserIdAndSourceKey(userId, key)
                .orElseGet(() -> cards.save(new ReviewCard(userId, type, key, conceptId, time.now())));
    }

    private void schedule(ReviewCard card, Grade g) {
        Scheduler.State s = Scheduler.next(card.state(), g);
        OffsetDateTime due = g == Grade.AGAIN ? time.now().plusMinutes(10) : time.startOf(time.today().plusDays(s.intervalDays()));
        card.apply(s, time.now(), due);
    }

    private CardView view(ReviewCard c, Topic t, boolean bookmarked, Map<Long, QuizQuestion> qs, Map<Long, InterviewItem> items) {
        int overdue = (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(time.dayOf(c.getDueAt()), time.today()));
        Map<Integer, String> next = new LinkedHashMap<>();
        for (Grade g : Grade.values()) next.put(g.value, label(Scheduler.next(c.state(), g).intervalDays(), g));

        switch (c.getType()) {
            case QUIZ -> {
                QuizQuestion q = qs.get(c.getQuestionId());
                if (q == null || !q.isActive()) return null;
                List<String> opts = strings(q.getOptionsJson());
                if (q.getType() == QuizQuestion.Type.TRUE_FALSE) opts = List.of("True", "False");
                Integer correctOpt = null;   // (a mixed int/Integer ternary here would unbox null → NPE)
                if (q.getType() == QuizQuestion.Type.TRUE_FALSE) correctOpt = "true".equals(q.getCorrectAnswer()) ? 0 : 1;
                else if (opts != null && q.getCorrectAnswer() != null) correctOpt = Integer.valueOf(q.getCorrectAnswer());
                String answer = q.isSelfGraded() ? null : answerLabel(q, strings(q.getOptionsJson()), q.getCorrectAnswer());
                List<String> kp = q.isSelfGraded() ? KeyPoints.resolve(json, q.getKeyPointsJson(), q.getExplanationMd()) : List.of();
                return new CardView(c.getId(), "QUIZ", t.slug(), t.title(), bookmarked, overdue, q.getPromptMd(), q.getCodeSnippet(),
                        opts, correctOpt, answer, q.getExplanationMd(), kp, next);
            }
            case INTERVIEW -> {
                InterviewItem i = items.get(c.getInterviewItemId());
                if (i == null || !i.isActive()) return null;
                return new CardView(c.getId(), "INTERVIEW", t.slug(), t.title(), bookmarked, overdue, i.getQuestionMd(), null,
                        null, null, null, i.getAnswerMd(), KeyPoints.resolve(json, i.getKeyPointsJson(), i.getAnswerMd()), next);
            }
            case EXPLAIN -> {
                Concept concept = concepts.findById(c.getConceptId()).orElse(null);
                if (concept == null || concept.getStatus() != Concept.Status.PUBLISHED) return null;
                String notes = LessonParser.section(concept.getBodyMd(), "summary");
                List<String> points = notes == null ? List.of() : notes.lines()
                        .filter(l -> l.strip().matches("^[-*]\\s+.+")).map(l -> l.strip().substring(2).strip()).limit(8).toList();
                return new CardView(c.getId(), "EXPLAIN", t.slug(), t.title(), bookmarked, overdue,
                        "Explain **" + concept.getTitle() + "** out loud, as in an interview: what it is, why it exists, "
                                + "how it works inside, an example, and one pitfall.",
                        null, null, null, concept.getSummary(), notes, points, next);
            }
            default -> {
                return null;
            }
        }
    }

    static String label(int intervalDays, Grade g) {
        if (g == Grade.AGAIN) return "10m";
        if (intervalDays < 30) return intervalDays + "d";
        if (intervalDays < 365) return Math.round(intervalDays / 30.0) + "mo";
        return "1y";
    }

    private String answerLabel(QuizQuestion q, List<String> options, String raw) {
        if (raw == null) return null;
        if (q.getType() == QuizQuestion.Type.TRUE_FALSE) return "true".equalsIgnoreCase(raw.strip()) ? "True" : "False";
        if (options != null) {
            try {
                int i = Integer.parseInt(raw.strip());
                if (i >= 0 && i < options.size()) return (char) ('A' + i) + ". " + options.get(i);
            } catch (NumberFormatException ignored) {
                // free text
            }
        }
        return raw;
    }

    private List<String> strings(String jsonArray) {
        if (jsonArray == null) return null;
        try {
            return json.readValue(jsonArray, STRINGS);
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> List<Long> ids(List<ReviewCard> list, Function<ReviewCard, Long> f) {
        return list.stream().map(f).filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
        return list.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }
}
