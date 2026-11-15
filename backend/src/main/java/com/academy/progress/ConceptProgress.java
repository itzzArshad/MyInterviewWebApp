package com.academy.progress;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "concept_progress")
@IdClass(ConceptProgress.Key.class)
public class ConceptProgress {

    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED }

    /** Composite primary key (user_id, concept_id). */
    public static class Key implements Serializable {
        private Long userId;
        private Long conceptId;

        public Key() {
        }

        public Key(Long userId, Long conceptId) {
            this.userId = userId;
            this.conceptId = conceptId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId) && Objects.equals(conceptId, k.conceptId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, conceptId);
        }
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "concept_id")
    private Long conceptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NOT_STARTED;

    @Column(nullable = false)
    private int mastery;

    @Column(nullable = false)
    private boolean bookmarked;

    @Column(name = "last_viewed_at")
    private OffsetDateTime lastViewedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ConceptProgress() {
    }

    public ConceptProgress(Long userId, Long conceptId) {
        this.userId = userId;
        this.conceptId = conceptId;
    }

    public void markViewed() {
        lastViewedAt = OffsetDateTime.now();
        if (status == Status.NOT_STARTED) status = Status.IN_PROGRESS;
    }

    public void setCompleted(boolean completed) {
        if (completed && status != Status.COMPLETED) {
            status = Status.COMPLETED;
            completedAt = OffsetDateTime.now();
        } else if (!completed && status == Status.COMPLETED) {
            status = Status.IN_PROGRESS;
            completedAt = null;
        }
    }

    public Long getUserId() { return userId; }
    public Long getConceptId() { return conceptId; }
    public Status getStatus() { return status; }
    public int getMastery() { return mastery; }
    public boolean isBookmarked() { return bookmarked; }
    public OffsetDateTime getLastViewedAt() { return lastViewedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public void setMastery(int mastery) { this.mastery = Math.max(0, Math.min(100, mastery)); }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
}
