package com.academy.curriculum;

/** Lightweight concept row (no lesson body) used for navigation and progress aggregation. */
public record ConceptSummary(Long id, Long chapterId, String slug, String title,
                             Concept.Difficulty difficulty, Concept.Status status,
                             int estMinutes, int sortOrder) {

    public boolean published() {
        return status == Concept.Status.PUBLISHED;
    }
}
