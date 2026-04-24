package com.insurancechoice.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to the app_users table.
 * Named AppUser to avoid collision with PostgreSQL reserved word "user".
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID string surfaced to the Angular frontend as userObjectId */
    @Column(nullable = false, unique = true)
    private String userObjectId;

    private String firstName;
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password — never returned in responses */
    @Column(nullable = false)
    private String password;

    /**
     * Roles stored as a JSON array string, e.g. ["User"] or ["Admin"].
     * Kept simple for Phase 1; extract to a join table when roles expand.
     */
    @Column(columnDefinition = "TEXT")
    private String rolesJson;

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

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRolesJson() { return rolesJson; }
    public void setRolesJson(String rolesJson) { this.rolesJson = rolesJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
