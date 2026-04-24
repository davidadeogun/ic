package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.dto.GptGenerateRequest;
import com.insurancechoice.backend.service.GptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class GptController {

    @Autowired private GptService gptService;

    /**
     * POST /gpt/generate
     * Generates personalised advantages/disadvantages via Azure OpenAI GPT.
     *
     * Request body:
     *   { "userObjectId": "...", "problemType": 1 }   → DM (Decision Making)
     *   { "userObjectId": "...", "problemType": 3 }   → PS (Problem Solving)
     *
     * Response: parsed JSON with "mode", "assetAtRisk", and "alternatives" array.
     * Requires a valid JWT (handled by JwtAuthFilter).
     */
    @PostMapping("/gpt/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody GptGenerateRequest req) {
        if (req.getUserObjectId() == null || req.getUserObjectId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userObjectId is required");
        }
        Map<String, Object> result = gptService.generate(req.getUserObjectId(), req.getProblemType());
        return ResponseEntity.ok(result);
    }
}
