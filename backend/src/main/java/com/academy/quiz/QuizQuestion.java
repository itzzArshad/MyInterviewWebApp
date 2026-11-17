package com.academy.quiz;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    public enum Type {
        MCQ, TRUE_FALSE, PREDICT_OUTPUT, FIND_BUG, CODE, SCENARIO, EXPLAIN, INTERVIEW;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "question_key", nullable = false, length = 50)
    private String questionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.academy.curriculum.Concept.Difficulty difficulty;

    @Column(name = "prompt_md", nullable = false, length = 20000)
    private String promptMd;

    @Column(name = "code_snippet", length = 20000)
    private String codeSnippet;

    @Column(name = "options_json", length = 20000)
    private String optionsJson;

    /** Option index, "true"/"false", or expected output text. NULL means the question is self-graded. */
    @Column(name = "correct_answer", length = 5000)
    private String correctAnswer;

    @Column(name = "explanation_md", nullable = false, length = 50000)
    private String explanationMd;

    @Column(name = "hints_json", length = 10000)
    private String hintsJson;

    /** Checklist used to self-score explain/interview answers. */
    @Column(name = "key_points_json", length = 10000)
    private String keyPointsJson;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected QuizQuestion() {
    }

    public QuizQuestion(Long conceptId, String questionKey) {
        this.conceptId = conceptId;
        this.questionKey = questionKey;
    }

    public boolean isSelfGraded() {
        return correctAnswer == null;
    }

    public Long getId() { return id; }
    public Long getConceptId() { return conceptId; }
    public String getQuestionKey() { return questionKey; }
    public Type getType() { return type; }
    public com.academy.curriculum.Concept.Difficulty getDifficulty() { return difficulty; }
    public String getPromptMd() { return promptMd; }
    public String getCodeSnippet() { return codeSnippet; }
    public String getOptionsJson() { return optionsJson; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanationMd() { return explanationMd; }
    public String getHintsJson() { return hintsJson; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }

    public void setType(Type type) { this.type = type; }
    public void setDifficulty(com.academy.curriculum.Concept.Difficulty difficulty) { this.difficulty = difficulty; }
    public void setPromptMd(String promptMd) { this.promptMd = promptMd; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanationMd(String explanationMd) { this.explanationMd = explanationMd; }
    public void setHintsJson(String hintsJson) { this.hintsJson = hintsJson; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }
    public String getKeyPointsJson() { return keyPointsJson; }
    public void setKeyPointsJson(String keyPointsJson) { this.keyPointsJson = keyPointsJson; }
}
