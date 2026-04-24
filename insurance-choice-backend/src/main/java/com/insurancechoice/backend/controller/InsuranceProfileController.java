package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.entity.InsuranceProfile;
import com.insurancechoice.backend.repository.InsuranceProfileRepository;
import com.insurancechoice.backend.service.GptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/insurance-profile")
public class InsuranceProfileController {

    private static final Logger log = LoggerFactory.getLogger(InsuranceProfileController.class);

    @Autowired private InsuranceProfileRepository repo;
    @Lazy @Autowired private GptService gptService;

    /** GET /insurance-profile/{userObjectId} */
    @GetMapping("/{userObjectId}")
    public ResponseEntity<InsuranceProfile> getProfile(@PathVariable String userObjectId,
                                                       Authentication auth) {
        InsuranceProfile profile = repo.findByUserObjectId(userObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return ResponseEntity.ok(profile);
    }

    /** POST /insurance-profile — create */
    @PostMapping
    public ResponseEntity<InsuranceProfile> createProfile(@RequestBody InsuranceProfile profile,
                                                          Authentication auth) {
        if (repo.existsByUserObjectId(profile.getUserObjectId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists — use PUT to update");
        }
        InsuranceProfile saved = repo.save(profile);
        try { gptService.purgeCache(saved.getUserObjectId()); }
        catch (Exception e) { log.warn("Cache purge skipped on create: {}", e.getMessage()); }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** PUT /insurance-profile/{userObjectId} — update */
    @PutMapping("/{userObjectId}")
    public ResponseEntity<InsuranceProfile> updateProfile(@PathVariable String userObjectId,
                                                          @RequestBody InsuranceProfile updated,
                                                          Authentication auth) {
        InsuranceProfile profile = repo.findByUserObjectId(userObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        profile.setAssetAtRisk(updated.getAssetAtRisk());
        profile.setEstimatedValue(updated.getEstimatedValue());
        profile.setFinancialCapacity(updated.getFinancialCapacity());
        profile.setGeographicRisk(updated.getGeographicRisk());
        profile.setGeographicRiskDescription(updated.getGeographicRiskDescription());
        profile.setRiskTolerance(updated.getRiskTolerance());
        profile.setDecisionPreferences(updated.getDecisionPreferences());
        profile.setEngagementStyle(updated.getEngagementStyle());

        InsuranceProfile saved = repo.save(profile);
        try { gptService.purgeCache(userObjectId); }
        catch (Exception e) { log.warn("Cache purge skipped on update: {}", e.getMessage()); }
        return ResponseEntity.ok(saved);
    }
}
