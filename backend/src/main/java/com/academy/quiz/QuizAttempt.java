package com.academy.quiz;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "question_ids", nullable = false, length = 4000)
    private String questionIds;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    private Integer score;

    @Column(nullable = false)
    private int total;

    /** LESSON = full quiz, RETRY = only previously wrong questions, DRILL = daily single-question drill */
    @Column(nullable = false, length = 20)
    private String mode = "LESSON";

    protected QuizAttempt() {
    }

    public QuizAttempt(Long userId, Long conceptId, List<Long> questionIds, String mode) {
        this(userId, conceptId, questionIds);
        this.mode = mode;
    }

    public QuizAttempt(Long userId, Long conceptId, List<Long> questionIds) {
        this.userId = userId;
        this.conceptId = conceptId;
        this.questionIds = questionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        this.total = questionIds.size();
        this.startedAt = OffsetDateTime.now();
    }

    public List<Long> questionIdList() {
        return Arrays.stream(questionIds.split(",")).map(Long::valueOf).toList();
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public void finish(int score) {
        this.score = score;
        this.finishedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getConceptId() { return conceptId; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public Integer getScore() { return score; }
    public int getTotal() { return total; }
    public String getMode() { return mode; }
}
