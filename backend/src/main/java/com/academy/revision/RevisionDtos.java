package com.academy.revision;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class RevisionDtos {
    private RevisionDtos() {
    }

    /**
     * A card in the review queue. Front = prompt (+code/options); back = answer, explanation and key points,
     * revealed by the client when you tap "Show answer" (self-assessed, like Anki).
     */
    public record CardView(Long id, String type, String conceptSlug, String conceptTitle, boolean bookmarked,
                           int overdueDays, String prompt, String code, List<String> options, Integer correctOption,
                           String answer, String explanation, List<String> keyPoints, Map<Integer, String> nextIntervals) {
    }

    public record Stats(long dueNow, long reviewedToday, int dailyGoal, long totalCards, long openMistakes,
                        Integer retentionPct) {
    }

    public record ReviewQueue(List<CardView> cards, Stats stats) {
    }

    public record GradeRequest(@Min(1) @Max(4) int grade) {
    }

    public record GradeResult(int intervalDays, OffsetDateTime dueAt, String nextLabel, Stats stats) {
    }

    public record MistakeView(Long id, String conceptSlug, String conceptTitle, String prompt, String code,
                              String yourAnswer, String correctAnswer, String explanation, int times,
                              String note, boolean resolved, OffsetDateTime lastWrongAt) {
    }

    public record MistakeUpdate(@Size(max = 2000) String note, Boolean resolved) {
    }

    public record SummaryView(String slug, String title, String subjectTitle, String summary, String notesMd,
                              Integer retentionPct) {
    }
}
