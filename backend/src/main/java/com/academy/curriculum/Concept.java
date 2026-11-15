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
}
