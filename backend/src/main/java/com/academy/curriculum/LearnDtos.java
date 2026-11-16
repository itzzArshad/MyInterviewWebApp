package com.academy.curriculum;

import com.academy.progress.ConceptProgress;

import java.time.OffsetDateTime;
import java.util.List;

public final class LearnDtos {
    private LearnDtos() {
    }

    // ---- Tree -----------------------------------------------------------------------------
    public record PathNode(String slug, String title, String description, List<SubjectNode> subjects) {
    }

    public record SubjectNode(String slug, String title, String description,
                              int completed, int published, int total, List<ChapterNode> chapters) {
    }

    public record ChapterNode(String slug, String title, List<ConceptNode> concepts) {
    }

    /** @param quizzed whether at least one quiz attempt on this concept was finished (mastery is meaningful) */
    public record ConceptNode(String slug, String title, Concept.Difficulty difficulty, int minutes,
                              boolean published, ConceptProgress.Status progress, int mastery,
                              boolean bookmarked, int questionCount, boolean quizzed,
                              OffsetDateTime lastViewedAt) {
    }

    // ---- Lesson ---------------------------------------------------------------------------
    public record Ref(String slug, String title) {
    }

    public record RelatedRef(String slug, String title, boolean published) {
    }

    public record ProgressView(ConceptProgress.Status status, int mastery, boolean bookmarked) {
        public static ProgressView of(ConceptProgress p) {
            return p == null
                    ? new ProgressView(ConceptProgress.Status.NOT_STARTED, 0, false)
                    : new ProgressView(p.getStatus(), p.getMastery(), p.isBookmarked());
        }
    }

    public record ConceptDetail(String slug, String title, Concept.Difficulty difficulty, int minutes,
                                String summary, String bodyMd, boolean published,
                                Ref subject, Ref chapter, ProgressView progress,
                                Ref prev, Ref next, List<RelatedRef> related, int questionCount,
                                LastQuiz lastQuiz, com.academy.lesson.LessonDtos.SyncView sync) {
    }

    public record LastQuiz(int score, int total, OffsetDateTime finishedAt) {
    }

    public record ProgressUpdate(Boolean completed, Boolean bookmarked) {
    }
}
