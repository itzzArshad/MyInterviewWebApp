package com.academy.quiz;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "quiz_answer")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "given_answer", length = 20000)
    private String givenAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(name = "answered_at", nullable = false)
    private OffsetDateTime answeredAt;

    protected QuizAnswer() {
    }

    public QuizAnswer(Long attemptId, Long questionId, String givenAnswer, boolean correct, int hintsUsed) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.givenAnswer = givenAnswer;
        this.correct = correct;
        this.hintsUsed = hintsUsed;
        this.answeredAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getAttemptId() { return attemptId; }
    public Long getQuestionId() { return questionId; }
    public String getGivenAnswer() { return givenAnswer; }
    public boolean isCorrect() { return correct; }
    public int getHintsUsed() { return hintsUsed; }
    public OffsetDateTime getAnsweredAt() { return answeredAt; }
}
