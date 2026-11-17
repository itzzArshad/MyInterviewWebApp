package com.academy.interview;

import com.academy.common.AppTime;
import com.academy.common.ProgressEvent;
import com.academy.curriculum.CurriculumIndex;
import com.academy.curriculum.CurriculumIndex.Topic;
import com.academy.interview.InterviewDtos.*;
import com.academy.progress.ConceptProgress;
import com.academy.progress.ConceptProgressRepository;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.academy.quiz.QuizService;
import com.academy.revision.ReviewCard;
import com.academy.revision.ReviewCardRepository;
import com.academy.revision.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interview training: the question bank (lesson interview Q&As + explain/interview quiz questions),
 * explain-out-loud practice scored against key points, timed mock interviews, and readiness per area.
 */
@Service
public class InterviewService {

    static final int OPEN_LIMIT_SEC = 180;
    static final int OPTION_LIMIT_SEC = 90;
    private static final Set<QuizQuestion.Type> OPEN_TYPES = EnumSet.of(QuizQuestion.Type.EXPLAIN, QuizQuestion.Type.INTERVIEW);
    private static final Set<QuizQuestion.Type> SCENARIO_TYPES =
            EnumSet.of(QuizQuestion.Type.FIND_BUG, QuizQuestion.Type.SCENARIO, QuizQuestion.Type.PREDICT_OUTPUT);
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };
    private static final TypeReference<List<FollowUp>> FOLLOW_UPS = new TypeReference<>() {
    };
    private static final TypeReference<List<PlanEntry>> PLAN = new TypeReference<>() {
    };

    record PlanEntry(String key, int limitSec) {
    }

    private final InterviewItemRepository items;
    private final InterviewAnswerRepository answers;
    private final MockSessionRepository mocks;
    private final QuizQuestionRepository questions;
    private final ConceptProgressRepository progress;
    private final ReviewCardRepository cards;
    private final ReviewService reviews;
    private final CurriculumIndex index;
    private final AppTime time;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;

    public InterviewService(InterviewItemRepository items, InterviewAnswerRepository answers, MockSessionRepository mocks,
                            QuizQuestionRepository questions, ConceptProgressRepository progress, ReviewCardRepository cards,
                            ReviewService reviews, CurriculumIndex index, AppTime time,
                            ApplicationEventPublisher events, ObjectMapper json) {
        this.items = items;
        this.answers = answers;
        this.mocks = mocks;
        this.questions = questions;
        this.progress = progress;
        this.cards = cards;
        this.reviews = reviews;
        this.index = index;
        this.time = time;
        this.events = events;
        this.json = json;
    }

    // ================================================================ the bank

    /** A bank entry resolved to its source row. */
    private record Entry(String key, Topic topic, InterviewItem item, QuizQuestion quiz) {
        boolean autoGraded() {
            return quiz != null && !quiz.isSelfGraded();
        }

        String text() {
            return item != null ? item.getQuestionMd() : quiz.getPromptMd();
        }
    }

    private List<Entry> openPool(CurriculumIndex.Snapshot idx) {
        List<Entry> out = new ArrayList<>();
        for (InterviewItem i : items.findByActiveTrue()) {
            Topic t = idx.topic(i.getConceptId());
            if (t != null && t.published()) out.add(new Entry("QA-" + i.getId(), t, i, null));
        }
        for (QuizQuestion q : questions.findAll()) {
            Topic t = idx.topic(q.getConceptId());
            if (q.isActive() && OPEN_TYPES.contains(q.getType()) && q.isSelfGraded() && t != null && t.published()) {
                out.add(new Entry("QZ-" + q.getId(), t, null, q));
            }
        }
        out.sort(Comparator.comparingInt((Entry e) -> e.topic().order()).thenComparing(Entry::key));
        return out;
    }

    private List<Entry> scenarioPool(CurriculumIndex.Snapshot idx) {
        List<Entry> out = new ArrayList<>();
        for (QuizQuestion q : questions.findAll()) {
            Topic t = idx.topic(q.getConceptId());
            if (q.isActive() && SCENARIO_TYPES.contains(q.getType()) && q.getOptionsJson() != null && t != null && t.published()) {
                out.add(new Entry("QZ-" + q.getId(), t, null, q));
            }
        }
        return out;
    }

    private Entry resolve(String key, CurriculumIndex.Snapshot idx) {
        try {
            long id = Long.parseLong(key.substring(3));
            if (key.startsWith("QA-")) {
                InterviewItem i = items.findById(id).filter(InterviewItem::isActive).orElse(null);
                if (i != null && idx.topic(i.getConceptId()) != null) return new Entry(key, idx.topic(i.getConceptId()), i, null);
            } else if (key.startsWith("QZ-")) {
                QuizQuestion q = questions.findById(id).filter(QuizQuestion::isActive).orElse(null);
                if (q != null && idx.topic(q.getConceptId()) != null) return new Entry(key, idx.topic(q.getConceptId()), null, q);
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
    }

    private Question toQuestion(Entry e) {
        Topic t = e.topic();
        if (e.item() != null) {
            InterviewItem i = e.item();
            return new Question(e.key(), t.slug(), t.title(), t.subjectSlug(), t.subjectTitle(), i.getQuestionMd(), null, null, false,
                    i.getAnswerMd(), KeyPoints.resolve(json, i.getKeyPointsJson(), i.getAnswerMd()), followUps(i), OPEN_LIMIT_SEC);
        }
        QuizQuestion q = e.quiz();
        List<String> options = q.getType() == QuizQuestion.Type.TRUE_FALSE ? List.of("True", "False") : strings(q.getOptionsJson());
        boolean auto = !q.isSelfGraded();
        return new Question(e.key(), t.slug(), t.title(), t.subjectSlug(), t.subjectTitle(), q.getPromptMd(), q.getCodeSnippet(),
                options, auto, q.getExplanationMd(),
                auto ? List.of() : KeyPoints.resolve(json, q.getKeyPointsJson(), q.getExplanationMd()),
                List.of(), auto ? OPTION_LIMIT_SEC : OPEN_LIMIT_SEC);
    }

    @Transactional(readOnly = true)
    public List<BankItem> bank(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        Map<String, List<InterviewAnswer>> history = historyByKey(userId);
        Set<String> dueKeys = dueInterviewKeys(userId);
        return openPool(idx).stream().map(e -> {
            List<InterviewAnswer> h = history.getOrDefault(e.key(), List.of());
            Integer best = h.stream().map(InterviewAnswer::getScorePct).max(Integer::compare).orElse(null);
            Integer last = h.stream().max(Comparator.comparing(InterviewAnswer::getCreatedAt)).map(InterviewAnswer::getScorePct).orElse(null);
            Topic t = e.topic();
            return new BankItem(e.key(), t.slug(), t.title(), t.subjectSlug(), t.subjectTitle(), e.text(), h.size(), best, last,
                    dueKeys.contains(e.key()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public Question question(String key) {
        return toQuestion(resolve(key, index.load()));
    }

    /** Next question to explain: a due one first, then unpractised questions from lessons you have studied. */
    @Transactional(readOnly = true)
    public String nextExplainKey(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        List<Entry> pool = openPool(idx);
        if (pool.isEmpty()) return null;
        Set<String> due = dueInterviewKeys(userId);
        Optional<Entry> d = pool.stream().filter(e -> due.contains(e.key())).findFirst();
        if (d.isPresent()) return d.get().key();

        Map<String, List<InterviewAnswer>> history = historyByKey(userId);
        Set<Long> studied = progress.findByUserId(userId).stream()
                .filter(p -> p.getStatus() != ConceptProgress.Status.NOT_STARTED)
                .map(ConceptProgress::getConceptId).collect(Collectors.toSet());
        Comparator<Entry> order = Comparator
                .comparing((Entry e) -> !studied.contains(e.topic().id()))
                .thenComparing(e -> history.containsKey(e.key()))
                .thenComparingInt(e -> history.getOrDefault(e.key(), List.of()).stream()
                        .mapToInt(InterviewAnswer::getScorePct).max().orElse(-1));
        return pool.stream().min(order).map(Entry::key).orElse(null);
    }

    // ================================================================ practice (explain mode)

    @Transactional
    public PracticeResult practise(Long userId, PracticeRequest req) {
        Entry e = resolve(req.key(), index.load());
        InterviewAnswer a = record(userId, null, InterviewAnswer.Mode.EXPLAIN, e, req);
        events.publishEvent(new ProgressEvent(userId));
        return result(e, a);
    }

    // ================================================================ mock interviews

    /** Plans a balanced round: 2 Java, 1 Spring, 1 backend, 1 debugging scenario, 1 design — filled from what exists. */
    @Transactional
    public MockView createMock(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        Map<String, List<InterviewAnswer>> history = historyByKey(userId);
        Random rnd = new Random();
        Comparator<Entry> freshFirst = Comparator.comparingInt(e -> history.getOrDefault(e.key(), List.of()).size());

        List<Entry> open = new ArrayList<>(openPool(idx));
        Collections.shuffle(open, rnd);
        open.sort(freshFirst);
        List<Entry> scenarios = new ArrayList<>(scenarioPool(idx));
        Collections.shuffle(scenarios, rnd);
        scenarios.sort(freshFirst);

        List<Entry> plan = new ArrayList<>();
        Set<Long> usedConcepts = new HashSet<>();
        take(plan, open, "java", 2, usedConcepts);
        take(plan, open, "spring", 1, usedConcepts);
        take(plan, open, "backend", 1, usedConcepts);
        take(plan, scenarios, null, 1, usedConcepts);
        take(plan, open, "system-design", 1, usedConcepts);
        // Fill gaps (early on most lessons are Core Java) — prefer new concepts, then anything left.
        while (plan.size() < 6 && take(plan, open, null, 1, usedConcepts) + take(plan, scenarios, null, 1, usedConcepts) > 0) {
            // keep filling
        }
        usedConcepts.clear();
        while (plan.size() < 6 && take(plan, open, null, 1, usedConcepts) + take(plan, scenarios, null, 1, usedConcepts) > 0) {
            // allow repeated concepts
        }
        if (plan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Add some lessons first — the interview bank is empty.");
        }

        List<PlanEntry> entries = plan.stream().map(e -> new PlanEntry(e.key(), e.autoGraded() ? OPTION_LIMIT_SEC : OPEN_LIMIT_SEC)).toList();
        MockSession s = mocks.save(new MockSession(userId, toJson(entries)));
        return new MockView(s.getId(), s.getStartedAt(), null, plan.stream().map(this::toQuestion).toList(), List.of());
    }

    private int take(List<Entry> plan, List<Entry> pool, String subject, int n, Set<Long> usedConcepts) {
        int taken = 0;
        Iterator<Entry> it = pool.iterator();
        while (it.hasNext() && taken < n) {
            Entry e = it.next();
            if (subject != null && !subject.equals(e.topic().subjectSlug())) continue;
            if (usedConcepts.contains(e.topic().id())) continue;
            if (plan.stream().anyMatch(p -> p.key().equals(e.key()))) continue;
            plan.add(e);
            usedConcepts.add(e.topic().id());
            it.remove();
            taken++;
        }
        return taken;
    }

    @Transactional(readOnly = true)
    public MockView mock(Long userId, Long id) {
        MockSession s = requireMock(userId, id);
        CurriculumIndex.Snapshot idx = index.load();
        List<Question> qs = plan(s).stream().map(p -> toQuestion(resolve(p.key(), idx))).toList();
        List<String> answered = answers.findByMockSessionIdOrderByIdAsc(id).stream().map(this::keyOf).toList();
        return new MockView(s.getId(), s.getStartedAt(), s.getFinishedAt(), qs, answered);
    }

    @Transactional
    public PracticeResult answerMock(Long userId, Long id, PracticeRequest req) {
        MockSession s = requireMock(userId, id);
        if (s.getFinishedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "This mock interview is finished.");
        if (plan(s).stream().noneMatch(p -> p.key().equals(req.key()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is not part of this interview.");
        }
        if (answers.findByMockSessionIdOrderByIdAsc(id).stream().anyMatch(a -> keyOf(a).equals(req.key()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already answered.");
        }
        Entry e = resolve(req.key(), index.load());
        InterviewAnswer a = record(userId, id, InterviewAnswer.Mode.MOCK, e, req);
        return result(e, a);
    }

    @Transactional
    public MockReport finishMock(Long userId, Long id) {
        MockSession s = requireMock(userId, id);
        if (s.getFinishedAt() == null) {
            List<PlanEntry> plan = plan(s);
            Map<String, InterviewAnswer> given = answers.findByMockSessionIdOrderByIdAsc(id).stream()
                    .collect(Collectors.toMap(this::keyOf, Function.identity(), (a, b) -> a));
            int score = (int) Math.round(plan.stream().mapToInt(p -> given.containsKey(p.key()) ? given.get(p.key()).getScorePct() : 0)
                    .average().orElse(0));
            s.finish(score);
            events.publishEvent(new ProgressEvent(userId));
        }
        return report(userId, id);
    }

    @Transactional(readOnly = true)
    public MockReport report(Long userId, Long id) {
        MockSession s = requireMock(userId, id);
        CurriculumIndex.Snapshot idx = index.load();
        Map<String, InterviewAnswer> given = answers.findByMockSessionIdOrderByIdAsc(id).stream()
                .collect(Collectors.toMap(this::keyOf, Function.identity(), (a, b) -> a));
        List<MockItemResult> results = new ArrayList<>();
        Map<String, List<Integer>> byArea = new LinkedHashMap<>();
        Map<String, String> areaTitles = new HashMap<>();
        for (PlanEntry p : plan(s)) {
            Entry e;
            try {
                e = resolve(p.key(), idx);
            } catch (ResponseStatusException gone) {
                continue; // question was removed from its lesson since
            }
            InterviewAnswer a = given.get(p.key());
            Topic t = e.topic();
            results.add(new MockItemResult(p.key(), e.text(), t.subjectTitle(), t.slug(), a == null ? null : a.getScorePct(),
                    a == null ? 0 : a.getDurationSec(), p.limitSec(), a == null ? null : a.getFollowUpOk(), a != null));
            byArea.computeIfAbsent(t.subjectSlug(), k -> new ArrayList<>()).add(a == null ? 0 : a.getScorePct());
            areaTitles.put(t.subjectSlug(), t.subjectTitle());
        }
        List<AreaScore> areas = byArea.entrySet().stream()
                .map(en -> new AreaScore(en.getKey(), areaTitles.get(en.getKey()),
                        (int) Math.round(en.getValue().stream().mapToInt(Integer::intValue).average().orElse(0)), en.getValue().size()))
                .toList();
        return new MockReport(s.getId(), s.getStartedAt(), s.getFinishedAt(), s.getScorePct(), areas, results, advice(results));
    }

    private static List<String> advice(List<MockItemResult> results) {
        List<String> out = new ArrayList<>();
        long overtime = results.stream().filter(r -> r.answered() && r.durationSec() > r.limitSec()).count();
        long weak = results.stream().filter(r -> r.scorePct() != null && r.scorePct() < 70).count();
        long skipped = results.stream().filter(r -> !r.answered()).count();
        long followUpMissed = results.stream().filter(r -> Boolean.FALSE.equals(r.followUpOk())).count();
        if (weak > 0) out.add(weak + " answer(s) covered less than 70% of the key points — they are scheduled for revision tomorrow.");
        if (followUpMissed > 0) out.add("You struggled with " + followUpMissed + " follow-up question(s). Practise going one level deeper: internals, edge cases, trade-offs.");
        if (overtime > 0) out.add(overtime + " answer(s) ran over time. Lead with a one-sentence answer, then expand.");
        if (skipped > 0) out.add(skipped + " question(s) were not answered.");
        if (out.isEmpty()) out.add("Strong round. Next time, aim to add a concrete production example to every answer.");
        return out;
    }

    @Transactional(readOnly = true)
    public List<MockSummary> recentMocks(Long userId) {
        return mocks.findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(userId, Limit.of(5)).stream()
                .map(m -> new MockSummary(m.getId(), m.getFinishedAt(), m.getScorePct(), plan(m).size())).toList();
    }

    // ================================================================ readiness

    /**
     * Per subject: 40% coverage (lessons completed out of all topics), 30% retention (estimated recall from
     * revision), 30% delivery (explain/mock scores, last 60 days). Each part is 0 until you have evidence for it.
     */
    @Transactional(readOnly = true)
    public Readiness readiness(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        Map<Long, ConceptProgress> prog = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(ConceptProgress::getConceptId, Function.identity()));
        Map<Long, Double> retention = reviews.retentionByConcept(userId);
        List<InterviewAnswer> recent = answers.findByUserIdAndCreatedAtGreaterThanEqual(userId, time.now().minusDays(60));

        List<AreaReadiness> areas = new ArrayList<>();
        for (var subject : idx.subjects()) {
            List<Topic> topics = idx.inSubject(subject.getSlug());
            if (topics.isEmpty()) continue;
            Set<Long> ids = topics.stream().map(Topic::id).collect(Collectors.toSet());
            int completed = (int) topics.stream().filter(t -> {
                ConceptProgress p = prog.get(t.id());
                return p != null && p.getStatus() == ConceptProgress.Status.COMPLETED;
            }).count();
            double coverage = (double) completed / topics.size();
            double ret = retention.entrySet().stream().filter(en -> ids.contains(en.getKey()))
                    .mapToDouble(Map.Entry::getValue).average().orElse(0);
            double delivery = recent.stream().filter(a -> ids.contains(a.getConceptId()))
                    .mapToInt(InterviewAnswer::getScorePct).average().orElse(0) / 100.0;
            int readiness = (int) Math.round(100 * (0.4 * coverage + 0.3 * ret + 0.3 * delivery));
            String adviceText = coverage <= ret && coverage <= delivery ? "Complete more lessons in this area."
                    : ret <= delivery ? "Revise this area — recall is fading."
                    : "Practise explaining answers out loud in this area.";
            areas.add(new AreaReadiness(subject.getSlug(), subject.getTitle(), readiness, pct(coverage), pct(ret), pct(delivery),
                    completed, topics.size(), adviceText));
        }
        int overall = (int) Math.round(areas.stream().mapToInt(AreaReadiness::readinessPct).average().orElse(0));
        return new Readiness(overall, areas);
    }

    @Transactional(readOnly = true)
    public Hub hub(Long userId) {
        return new Hub(readiness(userId), openPool(index.load()).size(), nextExplainKey(userId), recentMocks(userId));
    }

    // ================================================================ helpers

    private InterviewAnswer record(Long userId, Long mockId, InterviewAnswer.Mode mode, Entry e, PracticeRequest req) {
        int total, covered;
        if (e.autoGraded()) {
            boolean ok = req.chosen() != null && QuizService.Grader.matches(e.quiz(), req.chosen());
            total = 1;
            covered = ok ? 1 : 0;
            reviews.onQuizAnswered(userId, e.quiz(), ok, req.chosen());
        } else {
            Question q = toQuestion(e);
            total = Math.max(1, q.keyPoints().size());
            covered = req.covered() == null ? 0 : (int) req.covered().stream().filter(i -> i != null && i >= 0 && i < total).distinct().count();
        }
        String source = e.item() != null ? "QA" : "QUIZ";
        Long itemId = e.item() != null ? e.item().getId() : e.quiz().getId();
        InterviewAnswer a = answers.save(new InterviewAnswer(userId, mockId, mode, source, itemId, e.topic().id(),
                req.durationSec(), total, covered, req.followUpOk(), req.note()));
        if (!e.autoGraded()) {
            if (e.item() != null) reviews.onInterviewPracticed(userId, e.item(), a.getScorePct());
            else reviews.onQuizQuestionExplained(userId, e.quiz(), a.getScorePct());
        }
        return a;
    }

    private PracticeResult result(Entry e, InterviewAnswer a) {
        String correct = null;
        if (e.autoGraded()) {
            Question q = toQuestion(e);
            String raw = e.quiz().getCorrectAnswer();
            if (e.quiz().getType() == QuizQuestion.Type.TRUE_FALSE) correct = "true".equals(raw) ? "True" : "False";
            else if (q.options() != null) correct = (char) ('A' + Integer.parseInt(raw)) + ". " + q.options().get(Integer.parseInt(raw));
        }
        String key = e.item() != null ? "I:" + e.item().getId() : "Q:" + e.quiz().getId();
        String next = cards.findByUserIdAndSourceKey(a.getUserId(), key)
                .map(c -> c.getIntervalDays() == 0 ? "later today" : "in " + c.getIntervalDays() + " day" + (c.getIntervalDays() == 1 ? "" : "s"))
                .orElse(null);
        return new PracticeResult(a.getScorePct(), a.getPointsCovered(), a.getPointsTotal(), correct, next);
    }

    private Map<String, List<InterviewAnswer>> historyByKey(Long userId) {
        return answers.findByUserId(userId).stream().collect(Collectors.groupingBy(this::keyOf));
    }

    private String keyOf(InterviewAnswer a) {
        return ("QA".equals(a.getSource()) ? "QA-" : "QZ-") + a.getItemId();
    }

    private Set<String> dueInterviewKeys(Long userId) {
        OffsetDateTime now = time.now();
        Set<String> out = new HashSet<>();
        for (ReviewCard c : cards.findByUserId(userId)) {
            if (c.getDueAt().isAfter(now)) continue;
            if (c.getType() == ReviewCard.Type.INTERVIEW && c.getInterviewItemId() != null) out.add("QA-" + c.getInterviewItemId());
        }
        return out;
    }

    private MockSession requireMock(Long userId, Long id) {
        return mocks.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mock interview not found"));
    }

    private List<PlanEntry> plan(MockSession s) {
        try {
            return json.readValue(s.getPlanJson(), PLAN);
        } catch (Exception ex) {
            throw new IllegalStateException("Corrupt mock plan", ex);
        }
    }

    private List<FollowUp> followUps(InterviewItem i) {
        if (i.getFollowUpsJson() == null) return List.of();
        try {
            return json.readValue(i.getFollowUpsJson(), FOLLOW_UPS);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> strings(String jsonArray) {
        if (jsonArray == null) return null;
        try {
            return json.readValue(jsonArray, STRINGS);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int pct(double v) {
        return (int) Math.round(v * 100);
    }
}
