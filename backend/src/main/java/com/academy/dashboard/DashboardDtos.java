package com.academy.dashboard;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record Dashboard(String displayName,
                            Stats stats,
                            ContinueItem continueLearning,
                            List<SubjectProgress> subjects,
                            List<ConceptRef> weakAreas,
                            List<ConceptRef> bookmarks,
                            List<ActivityDay> activity,
                            List<ModuleStatus> modules) {
    }

    /** completed = the daily training session was finished that day (only those count for the streak). */
    public record ActivityDay(LocalDate date, boolean completed) {
    }

    /**
     * @param overallProgressPct    completed / published concepts
     * @param interviewReadinessPct average quiz mastery across all published concepts
     */
    public record Stats(int overallProgressPct, int streakDays, int conceptsCompleted, int conceptsTotal,
                        Integer quizAveragePct, int quizzesTaken, int dueForRevision, int interviewReadinessPct) {
    }

    public record ContinueItem(String slug, String title, String subjectTitle, String chapterTitle, boolean started) {
    }

    /** total = all concepts in the map, published = lessons written so far. */
    public record SubjectProgress(String slug, String title, int completed, int published, int total) {
    }

    public record ConceptRef(String slug, String title, int mastery) {
    }

    /** Lets the UI show which modules are live and which arrive in later phases. */
    public record ModuleStatus(String key, String title, boolean available, int phase) {
    }
}
