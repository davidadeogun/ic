package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.service.DomainTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DomainTemplateController {

    @Autowired private DomainTemplateService domainTemplateService;

    /**
     * GET /domain-template?assetAtRisk=Auto
     * Returns the domain-specific template data for the given asset-at-risk label.
     * Using a query parameter avoids Tomcat's rejection of encoded slashes (%2F)
     * in path variables, which broke domains like "Home/Property".
     */
    @GetMapping("/domain-template")
    public ResponseEntity<Map<String, Object>> getDomainTemplate(
            @RequestParam String assetAtRisk) {
        return ResponseEntity.ok(domainTemplateService.getByAssetAtRisk(assetAtRisk));
    }
}
