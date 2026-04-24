package com.insurancechoice.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gpt_cache",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_object_id", "problem_type"}))
public class GptCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_object_id", nullable = false)
    private String userObjectId;

    @Column(name = "problem_type", nullable = false)
    private int problemType;

    /** SHA-256 hash of the serialised InsuranceProfile fields — used for change detection. */
    @Column(name = "profile_snapshot", nullable = false, length = 64)
    private String profileSnapshot;

    /** Full raw JSON string returned by GPT. */
    @Column(name = "gpt_response", nullable = false, columnDefinition = "TEXT")
    private String gptResponse;

    /** Per-alternative summary values extracted from the first alternative for context prepend. */
    private Double br;
    private Double rrr;
    private Double rr;

    @Column(name = "cl_rrr")
    private String clRrr;

    @Column(name = "cl_rr")
    private String clRr;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }

    public int getProblemType() { return problemType; }
    public void setProblemType(int problemType) { this.problemType = problemType; }

    public String getProfileSnapshot() { return profileSnapshot; }
    public void setProfileSnapshot(String profileSnapshot) { this.profileSnapshot = profileSnapshot; }

    public String getGptResponse() { return gptResponse; }
    public void setGptResponse(String gptResponse) { this.gptResponse = gptResponse; }

    public Double getBr() { return br; }
    public void setBr(Double br) { this.br = br; }

    public Double getRrr() { return rrr; }
    public void setRrr(Double rrr) { this.rrr = rrr; }

    public Double getRr() { return rr; }
    public void setRr(Double rr) { this.rr = rr; }

    public String getClRrr() { return clRrr; }
    public void setClRrr(String clRrr) { this.clRrr = clRrr; }

    public String getClRr() { return clRr; }
    public void setClRr(String clRr) { this.clRr = clRr; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
