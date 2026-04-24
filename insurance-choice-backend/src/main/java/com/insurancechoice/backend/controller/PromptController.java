package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.dto.PromptRequest;
import com.insurancechoice.backend.entity.InsuranceProfile;
import com.insurancechoice.backend.repository.InsuranceProfileRepository;
import com.insurancechoice.backend.service.DomainTemplateService;
import com.insurancechoice.backend.service.PromptBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class PromptController {

    @Autowired private InsuranceProfileRepository profileRepository;
    @Autowired private DomainTemplateService domainTemplateService;
    @Autowired private PromptBuilderService promptBuilderService;

    /**
     * POST /prompt/build
     * Resolves the user's InsuranceProfile and domain template, builds the
     * completed prompt for the requested mode, logs it, and returns it.
     *
     * Request body: { "userObjectId": "...", "mode": "DM" | "PS" }
     */
    @PostMapping("/prompt/build")
    public ResponseEntity<Map<String, Object>> buildPrompt(@RequestBody PromptRequest req) {
        if (req.getUserObjectId() == null || req.getUserObjectId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userObjectId is required");
        }
        if (req.getMode() == null || (!req.getMode().equalsIgnoreCase("DM") && !req.getMode().equalsIgnoreCase("PS"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be DM or PS");
        }

        InsuranceProfile profile = profileRepository.findByUserObjectId(req.getUserObjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Insurance profile not found for user: " + req.getUserObjectId()));

        if (profile.getAssetAtRisk() == null || profile.getAssetAtRisk().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insurance profile has no Asset-at-Risk set");
        }

        Map<String, Object> domain = domainTemplateService.getByAssetAtRisk(profile.getAssetAtRisk());

        String prompt = req.getMode().equalsIgnoreCase("DM")
                ? promptBuilderService.buildDmPrompt(profile, domain)
                : promptBuilderService.buildPsPrompt(profile, domain);

        return ResponseEntity.ok(Map.of(
                "mode",          req.getMode().toUpperCase(),
                "assetAtRisk",   profile.getAssetAtRisk(),
                "prompt",        prompt
        ));
    }
}
