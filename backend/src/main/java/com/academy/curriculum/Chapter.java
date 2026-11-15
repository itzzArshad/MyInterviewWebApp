package com.academy.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Chapter() {
    }

    public Chapter(Long subjectId, String slug) {
        this.subjectId = subjectId;
        this.slug = slug;
    }

    public Long getId() { return id; }
    public Long getSubjectId() { return subjectId; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public int getSortOrder() { return sortOrder; }

    public void setTitle(String title) { this.title = title; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
