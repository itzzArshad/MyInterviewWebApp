package com.academy.today;

import com.academy.common.AppTime;
import com.academy.common.ProgressEvent;
import com.academy.curriculum.CurriculumIndex;
import com.academy.curriculum.CurriculumIndex.Topic;
import com.academy.dashboard.DashboardDtos.ContinueItem;
import com.academy.dashboard.DashboardService;
import com.academy.interview.InterviewAnswerRepository;
import com.academy.interview.InterviewService;
import com.academy.progress.ConceptProgress;
import com.academy.progress.ConceptProgressRepository;
import com.academy.quiz.QuizAttemptRepository;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.academy.revision.RevisionDtos;
import com.academy.revision.ReviewService;
import com.academy.user.ActivityService;
import com.academy.user.AppUser;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The daily training session — the habit the whole app is built around:
 * <ol>
 *   <li><b>Revise</b> — clear today's due cards (or 20 of them)</li>
 *   <li><b>Learn</b> — finish a lesson or a lesson quiz</li>
 *   <li><b>Think</b> — one debugging / predict-the-output drill</li>
 *   <li><b>Explain</b> — explain one interview question out loud</li>
 * </ol>
 * The day counts toward the streak when Revise + Explain + (Learn or Think) are done. Every step is derived
 * from what you actually did, so the streak cannot be earned by just opening the app.
 */
@Service
public class TodayService {

    private static final Set<QuizQuestion.Type> DRILL_TYPES =
            EnumSet.of(QuizQuestion.Type.FIND_BUG, QuizQuestion.Type.PREDICT_OUTPUT, QuizQuestion.Type.SCENARIO);

    public record Step(String key, String title, String description, boolean done, String detail) {
    }

    public record Drill(Long questionId, String conceptSlug, String conceptTitle, String type) {
    }

    public record TodayView(LocalDate date, boolean completed, int streak, List<Step> steps,
                            RevisionDtos.Stats revision, ContinueItem learn, Drill drill,
                            String explainKey, String explainQuestion) {
    }

    record Status(boolean revise, boolean learn, boolean think, boolean explain) {
        boolean completed() {
            return revise && explain && (learn || think);
        }
    }

    private final ReviewService reviews;
    private final InterviewService interview;
    private final DashboardService dashboard;
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final ConceptProgressRepository progress;
    private final InterviewAnswerRepository interviewAnswers;
    private final ActivityService activity;
    private final CurriculumIndex index;
    private final AppTime time;

    public TodayService(ReviewService reviews, InterviewService interview, DashboardService dashboard,
                        QuizQuestionRepository questions, QuizAttemptRepository attempts,
                        ConceptProgressRepository progress, InterviewAnswerRepository interviewAnswers,
                        ActivityService activity, CurriculumIndex index, AppTime time) {
        this.reviews = reviews;
        this.interview = interview;
        this.dashboard = dashboard;
        this.questions = questions;
        this.attempts = attempts;
        this.progress = progress;
        this.interviewAnswers = interviewAnswers;
        this.activity = activity;
        this.index = index;
        this.time = time;
    }

    @Transactional
    public TodayView today(AppUser user) {
        Long userId = user.getId();
        activity.touchToday(userId);
        RevisionDtos.Stats stats = reviews.stats(userId);
        Status s = status(userId, stats);
        if (s.completed()) activity.setSessionCompleted(userId, true);

        ContinueItem next = dashboard.continueLearning(userId);
        Drill drill = drill(userId);
        String explainKey = interview.nextExplainKey(userId);
        String explainQuestion = explainKey == null ? null : interview.question(explainKey).question();

        List<Step> steps = List.of(
                new Step("revise", "Revise", "Recall what is due before you forget it.", s.revise(),
                        stats.dueNow() == 0 ? (stats.reviewedToday() > 0 ? stats.reviewedToday() + " reviewed — all caught up" : "Nothing due today")
                                : stats.dueNow() + " due · " + stats.reviewedToday() + " reviewed"),
                new Step("learn", "Learn", "Study the next lesson and take its quiz.", s.learn(),
                        next == null ? "Every published lesson is complete — add a new one" : next.title()),
                new Step("think", "Think", "One drill: find the bug, predict the output, or reason through a scenario.", s.think(),
                        drill == null ? "No drill questions yet" : drill.conceptTitle()),
                new Step("explain", "Explain", "Answer one interview question out loud, then score yourself.", s.explain(),
                        explainQuestion == null ? "The interview bank is empty" : stripMd(explainQuestion)));
        return new TodayView(time.today(), s.completed(), activity.sessionStreak(userId), steps, stats, next, drill,
                explainKey, explainQuestion);
    }

    /** Re-evaluates the day after any progress; a finished session is recorded for the streak. */
    @EventListener
    public void onProgress(ProgressEvent e) {
        if (status(e.userId(), reviews.stats(e.userId())).completed()) activity.setSessionCompleted(e.userId(), true);
    }

    Status status(Long userId, RevisionDtos.Stats stats) {
        OffsetDateTime start = time.startOf(time.today());
        boolean revise = stats.dueNow() == 0 || stats.reviewedToday() >= stats.dailyGoal();
        boolean learn = attempts.existsByUserIdAndModeInAndFinishedAtGreaterThanEqual(userId, List.of("LESSON", "RETRY"), start)
                || progress.existsByUserIdAndCompletedAtGreaterThanEqual(userId, start);
        boolean think = attempts.existsByUserIdAndModeAndFinishedAtGreaterThanEqual(userId, "DRILL", start);
        boolean explain = interviewAnswers.existsByUserIdAndCreatedAtGreaterThanEqual(userId, start);
        return new Status(revise, learn, think, explain);
    }

    /** Today's drill: stable for the whole day, preferring topics you have already studied. */
    private Drill drill(Long userId) {
        CurriculumIndex.Snapshot idx = index.load();
        Set<Long> studied = progress.findByUserId(userId).stream()
                .filter(p -> p.getStatus() != ConceptProgress.Status.NOT_STARTED)
                .map(ConceptProgress::getConceptId).collect(Collectors.toSet());
        List<QuizQuestion> all = questions.findAll().stream()
                .filter(q -> q.isActive() && q.getOptionsJson() != null && DRILL_TYPES.contains(q.getType()))
                .filter(q -> idx.topic(q.getConceptId()) != null && idx.topic(q.getConceptId()).published())
                .sorted(Comparator.comparing(QuizQuestion::getId))
                .toList();
        List<QuizQuestion> preferred = all.stream().filter(q -> studied.contains(q.getConceptId())).toList();
        List<QuizQuestion> pool = preferred.isEmpty() ? all : preferred;
        if (pool.isEmpty()) return null;
        long seed = time.today().toEpochDay() * 31 + userId;
        QuizQuestion q = pool.get((int) Math.floorMod(seed, pool.size()));
        Topic t = idx.topic(q.getConceptId());
        return new Drill(q.getId(), t.slug(), t.title(), q.getType().name());
    }

    private static String stripMd(String s) {
        String t = s.replaceAll("[*_`]", "").strip();
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }
}
