package com.academy.revision;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_log")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int grade;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    protected ReviewLog() {
    }

    public ReviewLog(Long cardId, Long userId, int grade, int intervalDays, OffsetDateTime reviewedAt) {
        this.cardId = cardId;
        this.userId = userId;
        this.grade = grade;
        this.intervalDays = intervalDays;
        this.reviewedAt = reviewedAt;
    }

    public int getGrade() { return grade; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
}
