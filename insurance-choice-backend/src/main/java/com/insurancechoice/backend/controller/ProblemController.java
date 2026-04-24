package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.entity.AppUser;
import com.insurancechoice.backend.repository.UserRepository;
import com.insurancechoice.backend.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class ProblemController {

    @Autowired private ProblemService problemService;
    @Autowired private UserRepository userRepository;

    // ── Template endpoints (no userId needed) ─────────────────────────────────

    /**
     * POST /problem/list_problem_templates
     * Returns the three mode entries for the "Choose a mode" dropdown.
     */
    @PostMapping("/problem/list_problem_templates")
    public ResponseEntity<List<Map<String, Object>>> listTemplates(Authentication auth) {
        return ResponseEntity.ok(problemService.listTemplates());
    }

    /**
     * GET /problem/template/{id}
     * Returns the full tree structure for the selected mode (1, 2, or 3).
     * MUST be declared before GET /problem/{id} to avoid path ambiguity.
     */
    @GetMapping("/problem/template/{id}")
    public ResponseEntity<Map<String, Object>> getTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) String assetAtRisk) {
        return ResponseEntity.ok(problemService.getTemplateById(id, assetAtRisk));
    }

    // ── User problem endpoints ────────────────────────────────────────────────

    /**
     * POST /problem/list_problems
     * Lists all saved problems for the authenticated user.
     */
    @PostMapping("/problem/list_problems")
    public ResponseEntity<List<Map<String, Object>>> listProblems(Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(problemService.listProblems(user.getId()));
    }

    /**
     * POST /problem/save_problem
     * Creates a new problem or updates an existing one (id = 0 → create).
     */
    @PostMapping("/problem/save_problem")
    public ResponseEntity<Map<String, Object>> saveProblem(@RequestBody Map<String, Object> req,
                                                           Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(problemService.saveProblem(req, user.getId()));
    }

    /**
     * POST /update_problem  (root path — frontend calls this on edits)
     * Delegates to the same service method as save_problem.
     */
    @PostMapping("/update_problem")
    public ResponseEntity<Map<String, Object>> updateProblem(@RequestBody Map<String, Object> req,
                                                             Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(problemService.saveProblem(req, user.getId()));
    }

    /**
     * POST /create_template  (root path — admin creates new mode templates)
     * Phase 1 stub that simply echoes success; real logic added later.
     */
    @PostMapping("/create_template")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> req,
                                                              Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(problemService.saveProblem(req, user.getId()));
    }

    /**
     * POST /problem/delete_problem/{id}
     */
    @PostMapping("/problem/delete_problem/{id}")
    public ResponseEntity<Map<String, Object>> deleteProblem(@PathVariable Long id,
                                                             Authentication auth) {
        AppUser user = resolveUser(auth);
        problemService.deleteProblem(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * GET /problem/{id}
     * Returns the full problem with alternatives tree.
     */
    @GetMapping("/problem/{id}")
    public ResponseEntity<Map<String, Object>> getProblemById(@PathVariable Long id,
                                                              Authentication auth) {
        return ResponseEntity.ok(problemService.getProblemById(id));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Resolves the AppUser from the JWT principal (email stored as principal). */
    private AppUser resolveUser(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
