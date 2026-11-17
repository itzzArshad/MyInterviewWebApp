package com.academy.interview;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** One answered interview question — explain practice or part of a mock interview. */
@Entity
@Table(name = "interview_answer")
public class InterviewAnswer {

    public enum Mode { EXPLAIN, MOCK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mock_session_id")
    private Long mockSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mode mode;

    /** QA = interview item, QUIZ = quiz question */
    @Column(nullable = false, length = 10)
    private String source;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "duration_sec", nullable = false)
    private int durationSec;

    @Column(name = "points_total", nullable = false)
    private int pointsTotal;

    @Column(name = "points_covered", nullable = false)
    private int pointsCovered;

    @Column(name = "score_pct", nullable = false)
    private int scorePct;

    @Column(name = "follow_up_ok")
    private Boolean followUpOk;

    @Column(length = 5000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected InterviewAnswer() {
    }

    public InterviewAnswer(Long userId, Long mockSessionId, Mode mode, String source, Long itemId, Long conceptId,
                           int durationSec, int pointsTotal, int pointsCovered, Boolean followUpOk, String note) {
        this.userId = userId;
        this.mockSessionId = mockSessionId;
        this.mode = mode;
        this.source = source;
        this.itemId = itemId;
        this.conceptId = conceptId;
        this.durationSec = durationSec;
        this.pointsTotal = pointsTotal;
        this.pointsCovered = pointsCovered;
        this.scorePct = pointsTotal == 0 ? 0 : Math.round(100f * pointsCovered / pointsTotal);
        this.followUpOk = followUpOk;
        this.note = note;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getMockSessionId() { return mockSessionId; }
    public Mode getMode() { return mode; }
    public String getSource() { return source; }
    public Long getItemId() { return itemId; }
    public Long getConceptId() { return conceptId; }
    public int getDurationSec() { return durationSec; }
    public int getPointsTotal() { return pointsTotal; }
    public int getPointsCovered() { return pointsCovered; }
    public int getScorePct() { return scorePct; }
    public Boolean getFollowUpOk() { return followUpOk; }
    public String getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
