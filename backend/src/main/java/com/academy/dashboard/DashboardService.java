package com.academy.dashboard;

import com.academy.common.AppTime;
import com.academy.curriculum.LearnDtos.ChapterNode;
import com.academy.curriculum.LearnDtos.ConceptNode;
import com.academy.curriculum.LearnDtos.PathNode;
import com.academy.curriculum.LearnDtos.SubjectNode;
import com.academy.curriculum.LearnService;
import com.academy.dashboard.DashboardDtos.*;
import com.academy.interview.InterviewService;
import com.academy.progress.ConceptProgress;
import com.academy.quiz.QuizAttempt;
import com.academy.quiz.QuizAttemptRepository;
import com.academy.revision.ReviewService;
import com.academy.user.ActivityService;
import com.academy.user.AppUser;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DashboardService {

    /** Days of history shown in the activity heatmap (16 weeks). */
    private static final int HEATMAP_DAYS = 112;
    /** Quizzed concepts below this mastery are listed as weak areas. */
    private static final int WEAK_THRESHOLD = 60;

    private final LearnService learn;
    private final ActivityService activity;
    private final QuizAttemptRepository attempts;
    private final ReviewService reviews;
    private final InterviewService interview;
    private final AppTime time;

    public DashboardService(LearnService learn, ActivityService activity, QuizAttemptRepository attempts,
                            ReviewService reviews, InterviewService interview, AppTime time) {
        this.learn = learn;
        this.activity = activity;
        this.attempts = attempts;
        this.reviews = reviews;
        this.interview = interview;
        this.time = time;
    }

    /** A concept together with where it lives — flattened from the tree. */
    private record Located(ConceptNode concept, SubjectNode subject, ChapterNode chapter) {
    }

    @Transactional
    public Dashboard build(AppUser user) {
        Long userId = user.getId();
        activity.touchToday(userId);

        List<Located> published = new ArrayList<>();
        List<SubjectProgress> subjects = new ArrayList<>();
        collect(userId, published, subjects);

        int completed = (int) published.stream().filter(l -> l.concept().progress() == ConceptProgress.Status.COMPLETED).count();
        int overallPct = published.isEmpty() ? 0 : Math.round(100f * completed / published.size());

        List<QuizAttempt> recent = attempts.findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(userId, Limit.of(20));
        Integer quizAvg = recent.isEmpty() ? null
                : (int) Math.round(recent.stream().mapToDouble(a -> 100.0 * a.getScore() / a.getTotal()).average().orElse(0));

        // Weak areas use mastery adjusted for forgetting: a concept you aced months ago but never revised is weak again.
        Map<Long, Double> retention = reviews.retentionByConcept(userId);
        Map<String, Long> idBySlug = learn.conceptIdsBySlug();
        List<ConceptRef> weak = published.stream().map(Located::concept)
                .filter(ConceptNode::quizzed)
                .map(n -> new ConceptRef(n.slug(), n.title(), effective(n.mastery(), retention.get(idBySlug.get(n.slug())))))
                .filter(r -> r.mastery() < WEAK_THRESHOLD)
                .sorted(Comparator.comparingInt(ConceptRef::mastery))
                .limit(5)
                .toList();

        List<ConceptRef> bookmarks = published.stream().map(Located::concept)
                .filter(ConceptNode::bookmarked).map(n -> new ConceptRef(n.slug(), n.title(), n.mastery())).toList();

        List<ActivityDay> days = activity.daysSince(userId, time.today().minusDays(HEATMAP_DAYS - 1)).stream()
                .map(d -> new ActivityDay(d.date(), d.completed())).toList();

        List<ModuleStatus> modules = List.of(
                new ModuleStatus("today", "Today's training", true, 5),
                new ModuleStatus("learn", "Learning Academy", true, 2),
                new ModuleStatus("revision", "Revision", true, 5),
                new ModuleStatus("interview", "Interview Mode", true, 5),
                new ModuleStatus("notes", "Notes", false, 3),
                new ModuleStatus("github", "GitHub", false, 4));

        return new Dashboard(user.getDisplayName(),
                new Stats(overallPct, activity.sessionStreak(userId), completed, published.size(),
                        quizAvg, recent.size(), (int) reviews.stats(userId).dueNow(), interview.readiness(userId).overallPct()),
                continueItem(published), subjects, weak, bookmarks, days, modules);
    }

    /** Next lesson to study: the most recently viewed unfinished one, else the first published one not completed. */
    @Transactional(readOnly = true)
    public ContinueItem continueLearning(Long userId) {
        List<Located> published = new ArrayList<>();
        collect(userId, published, new ArrayList<>());
        return continueItem(published);
    }

    private void collect(Long userId, List<Located> published, List<SubjectProgress> subjects) {
        for (PathNode p : learn.tree(userId)) {
            for (SubjectNode s : p.subjects()) {
                subjects.add(new SubjectProgress(s.slug(), s.title(), s.completed(), s.published(), s.total()));
                for (ChapterNode c : s.chapters()) {
                    for (ConceptNode n : c.concepts()) {
                        if (n.published()) published.add(new Located(n, s, c));
                    }
                }
            }
        }
    }

    private static int effective(int mastery, Double retention) {
        return retention == null ? mastery : (int) Math.round(mastery * retention);
    }

    private static ContinueItem continueItem(List<Located> published) {
        Located pick = published.stream()
                .filter(l -> l.concept().progress() == ConceptProgress.Status.IN_PROGRESS)
                .filter(l -> l.concept().lastViewedAt() != null)
                .max(Comparator.comparing(l -> l.concept().lastViewedAt(), Comparator.<OffsetDateTime>naturalOrder()))
                .orElseGet(() -> published.stream()
                        .filter(l -> l.concept().progress() != ConceptProgress.Status.COMPLETED)
                        .findFirst().orElse(null));
        if (pick == null) return null;
        ConceptNode n = pick.concept();
        return new ContinueItem(n.slug(), n.title(), pick.subject().title(), pick.chapter().title(),
                !Objects.equals(n.progress(), ConceptProgress.Status.NOT_STARTED));
    }
}
