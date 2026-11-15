package com.academy.quiz;

import com.academy.quiz.QuizService.Grader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraderTest {

    private static QuizQuestion question(QuizQuestion.Type type, String options, String answer) {
        QuizQuestion q = new QuizQuestion(1L, "k");
        q.setType(type);
        q.setOptionsJson(options);
        q.setCorrectAnswer(answer);
        return q;
    }

    @Test
    void optionQuestionsCompareIndex() {
        QuizQuestion q = question(QuizQuestion.Type.MCQ, "[\"a\",\"b\"]", "1");
        assertThat(Grader.matches(q, "1")).isTrue();
        assertThat(Grader.matches(q, " 1 ")).isTrue();
        assertThat(Grader.matches(q, "0")).isFalse();
    }

    @Test
    void trueFalseIsCaseInsensitive() {
        QuizQuestion q = question(QuizQuestion.Type.TRUE_FALSE, null, "false");
        assertThat(Grader.matches(q, "FALSE")).isTrue();
        assertThat(Grader.matches(q, "true")).isFalse();
    }

    @Test
    void outputComparisonIgnoresTrailingWhitespaceAndLineEndingsOnly() {
        QuizQuestion q = question(QuizQuestion.Type.PREDICT_OUTPUT, null, "true\nfalse");
        assertThat(Grader.matches(q, "true  \r\nfalse\n")).isTrue();
        assertThat(Grader.matches(q, "True\nfalse")).isFalse();      // content/case matters
        assertThat(Grader.matches(q, "true false")).isFalse();
    }

    @Test
    void hintsReduceCreditButNeverBelowHalf() {
        assertThat(Grader.credit(0)).isEqualTo(1.0);
        assertThat(Grader.credit(1)).isEqualTo(0.75);
        assertThat(Grader.credit(5)).isEqualTo(0.5);
    }

    @Test
    void masterySmoothsTowardsLatestResult() {
        assertThat(Grader.nextMastery(null, 80)).isEqualTo(80);
        assertThat(Grader.nextMastery(50, 100)).isEqualTo(80);
        assertThat(Grader.nextMastery(100, 0)).isEqualTo(40);
    }
}
