package com.academy.lesson;

/** Which section template a topic follows (decided by its chapter). */
public enum LessonKind {
    CONCEPT("Concept lesson", "prompts/sections-concept.md"),
    LLD_PROBLEM("Low-level design problem", "prompts/sections-lld.md"),
    HLD_PROBLEM("High-level design problem", "prompts/sections-hld.md");

    public static final int SECTIONS = 13;

    private final String label;
    private final String sectionsResource;

    LessonKind(String label, String sectionsResource) {
        this.label = label;
        this.sectionsResource = sectionsResource;
    }

    public String label() {
        return label;
    }

    public String sectionsResource() {
        return sectionsResource;
    }

    public static LessonKind forChapter(String chapterSlug) {
        return switch (chapterSlug) {
            case "lld-problems" -> LLD_PROBLEM;
            case "hld-problems" -> HLD_PROBLEM;
            default -> CONCEPT;
        };
    }
}
