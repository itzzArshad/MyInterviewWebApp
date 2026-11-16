package com.academy.curriculum;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "concept")
public class Concept {

    public enum Status { PUBLISHED, PLANNED, ARCHIVED }

    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty = Difficulty.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PLANNED;

    private String summary;

    @Column(name = "body_md", length = 1_000_000)
    private String bodyMd;

    @Column(name = "related_slugs", length = 2000)
    private String relatedSlugs;

    @Column(name = "est_minutes", nullable = false)
    private int estMinutes;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** The lesson exactly as authored (front matter + body + quiz block) — what the editor shows and GitHub stores. */
    @Column(name = "source_md", length = 1_000_000)
    private String sourceMd;

    /** SEED = bundled with the app (may be refreshed on startup); USER = written in the app (never overwritten). */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_source", nullable = false, length = 10)
    private ContentSource contentSource = ContentSource.SEED;

    @Column(name = "github_path", length = 500)
    private String githubPath;

    @Column(name = "github_sha", length = 64)
    private String githubSha;

    @Column(name = "github_synced_at")
    private OffsetDateTime githubSyncedAt;

    @Column(name = "github_error", length = 2000)
    private String githubError;

    public enum ContentSource { SEED, USER }

    protected Concept() {
    }

    public Concept(String slug) {
        this.slug = slug;
        this.updatedAt = OffsetDateTime.now();
    }

    public List<String> relatedSlugList() {
        if (relatedSlugs == null || relatedSlugs.isBlank()) return List.of();
        return Arrays.stream(relatedSlugs.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public Long getId() { return id; }
    public Long getChapterId() { return chapterId; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public Difficulty getDifficulty() { return difficulty; }
    public Status getStatus() { return status; }
    public String getSummary() { return summary; }
    public String getBodyMd() { return bodyMd; }
    public String getRelatedSlugs() { return relatedSlugs; }
    public int getEstMinutes() { return estMinutes; }
    public String getContentHash() { return contentHash; }
    public int getSortOrder() { return sortOrder; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public void setTitle(String title) { this.title = title; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setStatus(Status status) { this.status = status; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setBodyMd(String bodyMd) { this.bodyMd = bodyMd; }
    public void setRelatedSlugs(String relatedSlugs) { this.relatedSlugs = relatedSlugs; }
    public void setEstMinutes(int estMinutes) { this.estMinutes = estMinutes; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSourceMd() { return sourceMd; }
    public ContentSource getContentSource() { return contentSource; }
    public String getGithubPath() { return githubPath; }
    public String getGithubSha() { return githubSha; }
    public OffsetDateTime getGithubSyncedAt() { return githubSyncedAt; }
    public String getGithubError() { return githubError; }

    public void setSourceMd(String sourceMd) { this.sourceMd = sourceMd; }
    public void setContentSource(ContentSource contentSource) { this.contentSource = contentSource; }

    public void markSynced(String path, String sha) {
        this.githubPath = path;
        this.githubSha = sha;
        this.githubSyncedAt = OffsetDateTime.now();
        this.githubError = null;
    }

    public void markSyncFailed(String error) {
        this.githubError = error == null ? "Unknown error" : error.substring(0, Math.min(error.length(), 1900));
    }

    public void clearGithub() {
        this.githubPath = null;
        this.githubSha = null;
        this.githubSyncedAt = null;
        this.githubError = null;
    }
}
