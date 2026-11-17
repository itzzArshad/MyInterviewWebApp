package com.academy.revision;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** A question you got wrong — with your answer and your own note on why. */
@Entity
@Table(name = "mistake")
public class Mistake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "given_answer", length = 20000)
    private String givenAnswer;

    @Column(nullable = false)
    private int times = 1;

    @Column(length = 2000)
    private String note;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "last_wrong_at", nullable = false)
    private OffsetDateTime lastWrongAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Mistake() {
    }

    public Mistake(Long userId, Long questionId, Long conceptId, String givenAnswer) {
        this.userId = userId;
        this.questionId = questionId;
        this.conceptId = conceptId;
        this.givenAnswer = givenAnswer;
        this.lastWrongAt = OffsetDateTime.now();
        this.createdAt = lastWrongAt;
    }

    public void repeat(String givenAnswer) {
        this.times++;
        this.givenAnswer = givenAnswer;
        this.lastWrongAt = OffsetDateTime.now();
        this.resolved = false;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQuestionId() { return questionId; }
    public Long getConceptId() { return conceptId; }
    public String getGivenAnswer() { return givenAnswer; }
    public int getTimes() { return times; }
    public String getNote() { return note; }
    public boolean isResolved() { return resolved; }
    public OffsetDateTime getLastWrongAt() { return lastWrongAt; }

    public void setNote(String note) { this.note = note; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
