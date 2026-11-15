package com.academy.user;

import jakarta.persistence.*;

import java.time.LocalDate;

/** One row per day the user was active. Powers the streak and the activity heatmap. */
@Entity
@Table(name = "activity_log", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"}))
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false)
    private int minutes;

    @Column(name = "items_completed", nullable = false)
    private int itemsCompleted;

    protected ActivityLog() {
    }

    public ActivityLog(Long userId, LocalDate activityDate) {
        this.userId = userId;
        this.activityDate = activityDate;
    }

    public LocalDate getActivityDate() { return activityDate; }
    public int getMinutes() { return minutes; }
    public int getItemsCompleted() { return itemsCompleted; }
}
