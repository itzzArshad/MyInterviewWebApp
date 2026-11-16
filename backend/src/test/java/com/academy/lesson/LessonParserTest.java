package com.academy.lesson;

import com.academy.curriculum.Concept;
import com.academy.lesson.LessonParser.ParsedLesson;
import com.academy.quiz.QuizQuestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonParserTest {

    private final LessonParser parser = new LessonParser();

    private static final String GOOD = """
            ---
            title: Demo
            difficulty: INTERMEDIATE
            minutes: 12
            summary: A demo lesson.
            ---

            ## 1. What is this concept?
            Text with `code`.

            ```java
            System.out.println("hi");
            ```

            ## 2. Why was it introduced?
            Because.

            ## 14. Quiz

            ~~~quiz
            questions:
              - key: q1
                type: MCQ
                difficulty: BEGINNER
                prompt: |
                  Pick one.
                options:
                  - "First"
                  - "Second: with colon"
                  - "Third"
                answer: B
                hints:
                  - "Think"
                explanation: |
                  Because B. Example:
                  ```java
                  int x = 1;
                  ```
              - key: q2
                type: TRUE_FALSE
                prompt: Is it true?
                answer: false
                explanation: No.
              - key: q3
                type: PREDICT_OUTPUT
                prompt: Output?
                code: |
                  System.out.println(1 + 1);
                answer: |
                  2
                explanation: Arithmetic.
              - key: q4
                type: INTERVIEW
                prompt: Explain it.
                explanation: |
                  Model answer.
            ~~~
            """;

    @Test
    void parsesFrontMatterBodyAndQuiz() {
        ParsedLesson p = parser.parse(GOOD, 2);
        assertThat(p.errors()).isEmpty();
        assertThat(p.difficulty()).isEqualTo(Concept.Difficulty.INTERMEDIATE);
        assertThat(p.minutes()).isEqualTo(12);
        assertThat(p.summary()).isEqualTo("A demo lesson.");
        assertThat(p.numberedSections()).isEqualTo(2);
        // quiz block and its heading are removed from the rendered body
        assertThat(p.body()).doesNotContain("~~~").doesNotContain("Quiz").endsWith("Because.");
        assertThat(p.questions()).hasSize(4);

        var q1 = p.questions().get(0);
        assertThat(q1.type()).isEqualTo(QuizQuestion.Type.MCQ);
        assertThat(q1.correctAnswer()).isEqualTo("1");                // letter B → index 1
        assertThat(q1.options()).containsExactly("First", "Second: with colon", "Third");
        assertThat(q1.explanation()).contains("```java");               // inner backtick fences survive
        assertThat(p.questions().get(1).correctAnswer()).isEqualTo("false");
        assertThat(p.questions().get(1).difficulty()).isEqualTo(Concept.Difficulty.INTERMEDIATE); // default
        assertThat(p.questions().get(2).correctAnswer()).isEqualTo("2");
        assertThat(p.questions().get(2).code()).isEqualTo("System.out.println(1 + 1);");
        assertThat(p.questions().get(3).selfGraded()).isTrue();
    }

    @Test
    void unwrapsWholeAnswerWrappedInMarkdownFence() {
        ParsedLesson p = parser.parse("```markdown\n" + GOOD + "\n```", 2);
        assertThat(p.errors()).isEmpty();
        assertThat(p.questions()).hasSize(4);
    }

    @Test
    void backtickQuizFenceWithIndentedInnerFencesStillWorks() {
        ParsedLesson p = parser.parse(GOOD.replace("~~~quiz", "```quiz").replace("\n~~~\n", "\n```\n"), 2);
        assertThat(p.errors()).isEmpty();
        assertThat(p.questions()).hasSize(4);
    }

    @Test
    void windowsLineEndingsAndBomAreHandled() {
        ParsedLesson p = parser.parse("﻿" + GOOD.replace("\n", "\r\n"), 2);
        assertThat(p.errors()).isEmpty();
        assertThat(p.questions()).hasSize(4);
    }

    @Test
    void reportsBadAnswerLetterWithQuestionKey() {
        ParsedLesson p = parser.parse(GOOD.replace("answer: B", "answer: F"), 2);
        assertThat(p.valid()).isFalse();
        assertThat(p.errors()).anySatisfy(e -> assertThat(e).contains("q1").contains("A–C"));
    }

    @Test
    void reportsInvalidYamlWithLineNumber() {
        ParsedLesson p = parser.parse(GOOD.replace("  - key: q2", "  - key: q2\n   broken: [unclosed"), 2);
        assertThat(p.valid()).isFalse();
        assertThat(p.errors()).anySatisfy(e -> assertThat(e).startsWith("Quiz is not valid YAML").contains("line"));
    }

    @Test
    void missingSectionsAndQuizAreWarningsNotErrors() {
        ParsedLesson p = parser.parse("## 1. Only one section\nHello", 13);
        assertThat(p.valid()).isTrue();
        assertThat(p.warnings()).anySatisfy(w -> assertThat(w).contains("Missing numbered sections"));
        assertThat(p.warnings()).anySatisfy(w -> assertThat(w).contains("No quiz block"));
        assertThat(p.warnings()).anySatisfy(w -> assertThat(w).contains("No front matter"));
    }

    @Test
    void emptyAndUnclosedInputsAreErrors() {
        assertThat(parser.parse("   ", 13).valid()).isFalse();
        assertThat(parser.parse("## 1. A\n~~~quiz\nquestions: []\n", 13).errors())
                .anySatisfy(e -> assertThat(e).contains("never closed"));
    }

    @Test
    void bundledSeedFormatWithZeroBasedIndexesStillParses() {
        String seed = "## 1. X\nBody\n\n~~~quiz\nquestions:\n  - key: q1\n    type: MCQ\n    prompt: P\n"
                + "    options: [a, b, c]\n    answer: 2\n    explanation: E\n~~~\n";
        assertThat(parser.parse(seed, 1).questions().get(0).correctAnswer()).isEqualTo("2");
    }
}
