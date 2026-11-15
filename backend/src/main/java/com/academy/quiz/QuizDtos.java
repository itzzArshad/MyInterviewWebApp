package com.academy.quiz;

import com.academy.curriculum.Concept;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class QuizDtos {
    private QuizDtos() {
    }

    /**
     * A question as sent to the client. The correct answer and explanation are NOT included —
     * they are returned only after the answer is submitted. Self-graded questions carry their
     * model answer, which the UI reveals only after you have written your own.
     */
    public record QuestionView(Long id, QuizQuestion.Type type, Concept.Difficulty difficulty,
                               String prompt, String code, List<String> options, List<String> hints,
                               boolean selfGraded, String modelAnswer) {
    }

    public record StartRequest(@NotBlank String conceptSlug, @Size(max = 100) List<Long> questionIds) {
    }

    public record AttemptView(Long attemptId, String conceptSlug, List<QuestionView> questions) {
    }

    public record AnswerRequest(@NotNull Long questionId,
                                @Size(max = 20000) String answer,
                                @Min(0) @Max(10) int hintsUsed,
                                Boolean selfCorrect) {
    }

    public record AnswerResult(boolean correct, String correctAnswer, String explanation) {
    }

    public record FinishResult(int score, int total, int pct, int mastery, List<Long> incorrectQuestionIds) {
    }
}
