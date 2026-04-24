package com.insurancechoice.backend.controller;

import com.insurancechoice.backend.dto.LoginRequest;
import com.insurancechoice.backend.dto.LoginResponse;
import com.insurancechoice.backend.dto.RegisterRequest;
import com.insurancechoice.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // ── Public ────────────────────────────────────────────────────────────────

    /** POST /user/login — no auth required */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }

    /** POST /user/register — no auth required */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(userService.register(req));
    }

    // ── Protected ─────────────────────────────────────────────────────────────

    /** POST /user/list_users */
    @PostMapping("/list_users")
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication auth) {
        return ResponseEntity.ok(userService.listUsers());
    }

    /** POST /user/create_user */
    @PostMapping("/create_user")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody RegisterRequest req,
                                                          Authentication auth) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    /** POST /user/update_user */
    @PostMapping("/update_user")
    public ResponseEntity<Map<String, Object>> updateUser(@RequestBody Map<String, Object> req,
                                                          Authentication auth) {
        return ResponseEntity.ok(userService.updateUser(req));
    }

    /** POST /user/reset-password */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> req,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(userService.resetPassword(req, email));
    }

    /**
     * POST /user/get-user-info
     * Frontend sends {"userId": "<userObjectId>"}.
     */
    @PostMapping("/get-user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestBody Map<String, Object> req,
                                                           Authentication auth) {
        String userObjectId = (String) req.getOrDefault("userId", auth.getCredentials());
        return ResponseEntity.ok(userService.getUserInfo(userObjectId));
    }

    /**
     * POST /user/shared_user_list
     * Phase 1 stub — returns an empty list; sharing logic added in a later phase.
     */
    @PostMapping("/shared_user_list")
    public ResponseEntity<List<Map<String, Object>>> sharedUserList(
            @RequestBody(required = false) Map<String, Object> req,
            Authentication auth) {
        return ResponseEntity.ok(List.of());
    }
}
