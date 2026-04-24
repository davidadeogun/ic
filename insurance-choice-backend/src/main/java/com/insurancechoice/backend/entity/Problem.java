package com.insurancechoice.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A user's problem (or a system template when isTemplate = true).
 *
 * The full alternative tree and metadata are serialised as JSON TEXT columns.
 * This avoids a complex adjacency-list ORM for Phase 1; the tree is
 * reconstructed in-memory by ProblemService before returning to the frontend.
 *
 * problemType:
 *   1 = Decision Making
 *   2 = Problem Solving – Reactive
 *   3 = Problem Solving – Proactive
 */
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Integer problemType;

    /** FK to app_users.id.  Null for template rows. */
    private Long userId;

    /** When true this row is a reusable mode template, not a user problem */
    private Boolean isTemplate = false;

    /** Full metadata object serialised as JSON (frame labels, sub-alternatives) */
    @Column(columnDefinition = "TEXT")
    private String metaDataJson;

    /** Full problemAlternatives tree serialised as JSON array */
    @Column(columnDefinition = "TEXT")
    private String problemAlternativesJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getProblemType() { return problemType; }
    public void setProblemType(Integer problemType) { this.problemType = problemType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Boolean getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Boolean isTemplate) { this.isTemplate = isTemplate; }

    public String getMetaDataJson() { return metaDataJson; }
    public void setMetaDataJson(String metaDataJson) { this.metaDataJson = metaDataJson; }

    public String getProblemAlternativesJson() { return problemAlternativesJson; }
    public void setProblemAlternativesJson(String problemAlternativesJson) {
        this.problemAlternativesJson = problemAlternativesJson;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
