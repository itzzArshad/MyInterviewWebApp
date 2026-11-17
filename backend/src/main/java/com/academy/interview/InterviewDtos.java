package com.academy.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class InterviewDtos {
    private InterviewDtos() {
    }

    public record FollowUp(String question, String answer) {
    }

    /** Key: "QA-<id>" = interview Q&A from a lesson; "QZ-<id>" = quiz question. */
    public record BankItem(String key, String conceptSlug, String conceptTitle, String subjectSlug, String subjectTitle,
                           String question, int attempts, Integer bestScore, Integer lastScore, boolean due) {
    }

    /**
     * A question to answer out loud. {@code autoGraded} questions have options and are checked by the server;
     * the others are self-scored against {@code keyPoints}. The model answer is shown only after you answer.
     */
    public record Question(String key, String conceptSlug, String conceptTitle, String subjectSlug, String subjectTitle,
                           String question, String code, List<String> options, boolean autoGraded,
                           String modelAnswer, List<String> keyPoints, List<FollowUp> followUps, int limitSec) {
    }

    public record PracticeRequest(@NotBlank String key,
                                  @Min(0) @Max(7200) int durationSec,
                                  @Size(max = 50) List<Integer> covered,
                                  Boolean followUpOk,
                                  @Size(max = 5000) String note,
                                  @Size(max = 100) String chosen) {
    }

    public record PracticeResult(int scorePct, int pointsCovered, int pointsTotal, String correctAnswer,
                                 String nextReview) {
    }

    // ---- mock interviews
    public record MockView(Long id, OffsetDateTime startedAt, OffsetDateTime finishedAt, List<Question> questions,
                           List<String> answeredKeys) {
    }

    public record MockItemResult(String key, String question, String subjectTitle, String conceptSlug, Integer scorePct,
                                 int durationSec, int limitSec, Boolean followUpOk, boolean answered) {
    }

    public record AreaScore(String subjectSlug, String subjectTitle, int scorePct, int questions) {
    }

    public record MockReport(Long id, OffsetDateTime startedAt, OffsetDateTime finishedAt, Integer scorePct,
                             List<AreaScore> areas, List<MockItemResult> items, List<String> advice) {
    }

    public record MockSummary(Long id, OffsetDateTime finishedAt, Integer scorePct, int questions) {
    }

    // ---- readiness
    public record AreaReadiness(String subjectSlug, String subjectTitle, int readinessPct, int coveragePct,
                                int retentionPct, int deliveryPct, int completed, int total, String advice) {
    }

    public record Readiness(int overallPct, List<AreaReadiness> areas) {
    }

    public record Hub(Readiness readiness, int bankSize, String nextExplainKey, List<MockSummary> recentMocks) {
    }
}
