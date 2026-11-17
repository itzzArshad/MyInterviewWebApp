package com.academy.revision;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_card")
public class ReviewCard {

    public enum Type { QUIZ, EXPLAIN, INTERVIEW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private Type type;

    @Column(name = "source_key", nullable = false, length = 50)
    private String sourceKey;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "interview_item_id")
    private Long interviewItemId;

    @Column(nullable = false)
    private double ease = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private int repetitions;

    @Column(nullable = false)
    private int lapses;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ReviewCard() {
    }

    public ReviewCard(Long userId, Type type, String sourceKey, Long conceptId, OffsetDateTime dueAt) {
        this.userId = userId;
        this.type = type;
        this.sourceKey = sourceKey;
        this.conceptId = conceptId;
        this.dueAt = dueAt;
        this.createdAt = OffsetDateTime.now();
    }

    public Scheduler.State state() {
        return new Scheduler.State(ease, intervalDays, repetitions, lapses);
    }

    public void apply(Scheduler.State s, OffsetDateTime reviewedAt, OffsetDateTime nextDue) {
        this.ease = s.ease();
        this.intervalDays = s.intervalDays();
        this.repetitions = s.repetitions();
        this.lapses = s.lapses();
        this.lastReviewedAt = reviewedAt;
        this.dueAt = nextDue;
    }

    public boolean isNew() {
        return lastReviewedAt == null && repetitions == 0;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Type getType() { return type; }
    public String getSourceKey() { return sourceKey; }
    public Long getConceptId() { return conceptId; }
    public Long getQuestionId() { return questionId; }
    public Long getInterviewItemId() { return interviewItemId; }
    public double getEase() { return ease; }
    public int getIntervalDays() { return intervalDays; }
    public int getRepetitions() { return repetitions; }
    public int getLapses() { return lapses; }
    public OffsetDateTime getDueAt() { return dueAt; }
    public OffsetDateTime getLastReviewedAt() { return lastReviewedAt; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public void setInterviewItemId(Long interviewItemId) { this.interviewItemId = interviewItemId; }
    public void setDueAt(OffsetDateTime dueAt) { this.dueAt = dueAt; }
}
