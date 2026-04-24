package com.insurancechoice.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_profiles")
public class InsuranceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userObjectId;

    private String assetAtRisk;
    private String estimatedValue;
    private String financialCapacity;
    private String geographicRisk;

    @Column(columnDefinition = "TEXT")
    private String geographicRiskDescription;

    private String riskTolerance;
    private String decisionPreferences;
    private String engagementStyle;

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

    public Long getId() { return id; }

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }

    public String getAssetAtRisk() { return assetAtRisk; }
    public void setAssetAtRisk(String assetAtRisk) { this.assetAtRisk = assetAtRisk; }

    public String getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(String estimatedValue) { this.estimatedValue = estimatedValue; }

    public String getFinancialCapacity() { return financialCapacity; }
    public void setFinancialCapacity(String financialCapacity) { this.financialCapacity = financialCapacity; }

    public String getGeographicRisk() { return geographicRisk; }
    public void setGeographicRisk(String geographicRisk) { this.geographicRisk = geographicRisk; }

    public String getGeographicRiskDescription() { return geographicRiskDescription; }
    public void setGeographicRiskDescription(String d) { this.geographicRiskDescription = d; }

    public String getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(String riskTolerance) { this.riskTolerance = riskTolerance; }

    public String getDecisionPreferences() { return decisionPreferences; }
    public void setDecisionPreferences(String decisionPreferences) { this.decisionPreferences = decisionPreferences; }

    public String getEngagementStyle() { return engagementStyle; }
    public void setEngagementStyle(String engagementStyle) { this.engagementStyle = engagementStyle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
