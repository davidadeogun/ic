package com.insurancechoice.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles subscription and plan endpoints.
 * Phase 1: returns hardcoded plans so the Profile page renders correctly.
 * Phase 2: wire to a real PayPal / Stripe integration.
 */
@RestController
public class SubscriptionController {

    private static final List<Map<String, Object>> PLANS = List.of(
            plan(1, "Basic",        0,    "Free tier — up to 3 saved problems",
                    List.of("3 problems", "All 3 decision modes"), "paypal-plan-basic"),
            plan(2, "Professional", 9.99, "Unlimited problems + AI analysis",
                    List.of("Unlimited problems", "AI recommendations", "Export to PDF"), "paypal-plan-pro"),
            plan(3, "Enterprise",   29.99,"Team sharing + priority support",
                    List.of("Everything in Pro", "Team sharing", "Priority support"), "paypal-plan-ent")
    );

    /** GET /subscription — returns available plans */
    @GetMapping("/subscription")
    public ResponseEntity<List<Map<String, Object>>> getPlans(Authentication auth) {
        return ResponseEntity.ok(PLANS);
    }

    /** POST /user-subscription/add-subscription */
    @PostMapping("/user-subscription/add-subscription")
    public ResponseEntity<Map<String, Object>> addSubscription(@RequestBody Map<String, Object> req,
                                                               Authentication auth) {
        // Phase 1 stub — acknowledge receipt, no DB write yet
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** POST /user-subscription/cancel-subscription */
    @PostMapping("/user-subscription/cancel-subscription")
    public ResponseEntity<Map<String, Object>> cancelSubscription(@RequestBody Map<String, Object> req,
                                                                  Authentication auth) {
        // Phase 1 stub
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static Map<String, Object> plan(int id, String name, double price,
                                            String desc, List<String> features,
                                            String paypalPlanId) {
        return Map.of(
                "id", id,
                "name", name,
                "price", price,
                "description", desc,
                "features", features,
                "paypalPlanId", paypalPlanId
        );
    }
}
