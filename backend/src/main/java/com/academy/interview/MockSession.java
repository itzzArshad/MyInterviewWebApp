package com.academy.interview;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mock_session")
public class MockSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** JSON list of planned questions: [{key, area, limitSec}] */
    @Column(name = "plan_json", nullable = false, length = 20000)
    private String planJson;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "score_pct")
    private Integer scorePct;

    protected MockSession() {
    }

    public MockSession(Long userId, String planJson) {
        this.userId = userId;
        this.planJson = planJson;
        this.startedAt = OffsetDateTime.now();
    }

    public void finish(int scorePct) {
        this.scorePct = scorePct;
        this.finishedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPlanJson() { return planJson; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public Integer getScorePct() { return scorePct; }
}
