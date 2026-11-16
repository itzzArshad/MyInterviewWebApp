package com.academy.lesson;

import com.academy.curriculum.Concept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class LessonDtos {
    private LessonDtos() {
    }

    public record MarkdownRequest(@NotNull @Size(max = LessonParser.MAX_LENGTH) String markdown) {
    }

    /** Result of parsing a paste, shown live in the editor. */
    public record Preview(boolean valid, Concept.Difficulty difficulty, Integer minutes, String summary,
                          int sections, int expectedSections, int questions, Map<String, Long> questionsByType,
                          String body, List<String> errors, List<String> warnings) {
    }

    /** GitHub mirror state of one lesson. state: DISABLED | NOT_SYNCED | SYNCED | FAILED */
    public record SyncView(String state, String path, String url, OffsetDateTime syncedAt, String error) {
    }

    public record Source(String slug, String title, String kind, boolean published,
                         Concept.ContentSource contentSource, String markdown, SyncView sync) {
    }

    public record Prompt(String slug, String kind, String prompt) {
    }

    public record SaveResult(String slug, int questions, List<String> warnings, SyncView sync) {
    }

    public record GitHubStatus(boolean enabled, String repo, String branch, String url) {
    }

    public record BulkResult(int succeeded, int failed, List<String> messages) {
    }
}
