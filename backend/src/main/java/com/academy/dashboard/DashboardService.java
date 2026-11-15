package com.academy.dashboard;

import com.academy.curriculum.LearnDtos.ChapterNode;
import com.academy.curriculum.LearnDtos.ConceptNode;
import com.academy.curriculum.LearnDtos.PathNode;
import com.academy.curriculum.LearnDtos.SubjectNode;
import com.academy.curriculum.LearnService;
import com.academy.dashboard.DashboardDtos.*;
import com.academy.progress.ConceptProgress;
import com.academy.quiz.QuizAttempt;
import com.academy.quiz.QuizAttemptRepository;
import com.academy.user.ActivityService;
import com.academy.user.AppUser;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardService {

    /** Days of history shown in the activity heatmap (16 weeks). */
    private static final int HEATMAP_DAYS = 112;
    /** Quizzed concepts below this mastery are listed as weak areas. */
    private static final int WEAK_THRESHOLD = 60;

    private final LearnService learn;
    private final ActivityService activity;
    private final QuizAttemptRepository attempts;

    public DashboardService(LearnService learn, ActivityService activity, QuizAttemptRepository attempts) {
        this.learn = learn;
        this.activity = activity;
        this.attempts = attempts;
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

        int completed = (int) published.stream().filter(l -> l.concept().progress() == ConceptProgress.Status.COMPLETED).count();
        int overallPct = published.isEmpty() ? 0 : Math.round(100f * completed / published.size());
        int readiness = (int) Math.round(published.stream().mapToInt(l -> l.concept().mastery()).average().orElse(0));

        List<QuizAttempt> recent = attempts.findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(userId, Limit.of(20));
        Integer quizAvg = recent.isEmpty() ? null
                : (int) Math.round(recent.stream().mapToDouble(a -> 100.0 * a.getScore() / a.getTotal()).average().orElse(0));

        List<ConceptRef> weak = published.stream().map(Located::concept)
                .filter(n -> n.quizzed() && n.mastery() < WEAK_THRESHOLD)
                .sorted(Comparator.comparingInt(ConceptNode::mastery))
                .limit(5)
                .map(DashboardService::ref)
                .toList();

        List<ConceptRef> bookmarks = published.stream().map(Located::concept)
                .filter(ConceptNode::bookmarked).map(DashboardService::ref).toList();

        LocalDate today = LocalDate.now();
        List<LocalDate> days = activity.activeDaysSince(userId, today.minusDays(HEATMAP_DAYS - 1));

        List<ModuleStatus> modules = List.of(
                new ModuleStatus("dashboard", "Dashboard", true, 1),
                new ModuleStatus("learn", "Learning Academy", true, 2),
                new ModuleStatus("notes", "Notes", false, 3),
                new ModuleStatus("github", "GitHub", false, 4),
                new ModuleStatus("interview", "Interview Mode", false, 5),
                new ModuleStatus("revision", "Revision", false, 5));

        return new Dashboard(user.getDisplayName(),
                new Stats(overallPct, ActivityService.currentStreak(days, today), completed, published.size(),
                        quizAvg, recent.size(), 0, readiness),
                continueItem(published), subjects, weak, bookmarks, days, modules);
    }

    /** Most recently viewed unfinished lesson; otherwise the first published lesson not yet completed. */
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

    private static ConceptRef ref(ConceptNode n) {
        return new ConceptRef(n.slug(), n.title(), n.mastery());
    }
}
