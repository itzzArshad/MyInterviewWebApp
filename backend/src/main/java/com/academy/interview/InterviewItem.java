package com.academy.interview;

import jakarta.persistence.*;

/** One interview Q&A extracted from a lesson's interview section ("**Q1. …**" + answer). */
@Entity
@Table(name = "interview_item")
public class InterviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "item_key", nullable = false, length = 20)
    private String itemKey;

    @Column(name = "question_md", nullable = false, length = 5000)
    private String questionMd;

    @Column(name = "answer_md", nullable = false, length = 50000)
    private String answerMd;

    @Column(name = "key_points_json", length = 10000)
    private String keyPointsJson;

    @Column(name = "follow_ups_json", length = 20000)
    private String followUpsJson;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected InterviewItem() {
    }

    public InterviewItem(Long conceptId, String itemKey) {
        this.conceptId = conceptId;
        this.itemKey = itemKey;
    }

    public Long getId() { return id; }
    public Long getConceptId() { return conceptId; }
    public String getItemKey() { return itemKey; }
    public String getQuestionMd() { return questionMd; }
    public String getAnswerMd() { return answerMd; }
    public String getKeyPointsJson() { return keyPointsJson; }
    public String getFollowUpsJson() { return followUpsJson; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }

    public void setQuestionMd(String questionMd) { this.questionMd = questionMd; }
    public void setAnswerMd(String answerMd) { this.answerMd = answerMd; }
    public void setKeyPointsJson(String keyPointsJson) { this.keyPointsJson = keyPointsJson; }
    public void setFollowUpsJson(String followUpsJson) { this.followUpsJson = followUpsJson; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }
}
