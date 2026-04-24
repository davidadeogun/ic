package com.insurancechoice.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurancechoice.backend.dto.LoginRequest;
import com.insurancechoice.backend.dto.LoginResponse;
import com.insurancechoice.backend.dto.RegisterRequest;
import com.insurancechoice.backend.entity.AppUser;
import com.insurancechoice.backend.repository.UserRepository;
import com.insurancechoice.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Auth ─────────────────────────────────────────────────────────────────

    public LoginResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = new AppUser();
        user.setUserObjectId(UUID.randomUUID().toString());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        List<String> roles = (req.getRole() != null && !req.getRole().isEmpty())
                ? req.getRole()
                : List.of("User");
        user.setRolesJson(toJson(roles));

        userRepository.save(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        AppUser user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return buildLoginResponse(user);
    }

    // ── User CRUD (admin) ─────────────────────────────────────────────────────

    public AppUser getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toPublicMap)
                .toList();
    }

    public Map<String, Object> createUser(RegisterRequest req) {
        register(req); // reuse register logic
        return Map.of("success", true);
    }

    public Map<String, Object> updateUser(Map<String, Object> req) {
        String email = (String) req.getOrDefault("email",
                req.getOrDefault("emailAddress", ""));
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.containsKey("firstName")) user.setFirstName((String) req.get("firstName"));
        if (req.containsKey("lastName"))  user.setLastName((String) req.get("lastName"));
        userRepository.save(user);
        return Map.of("success", true);
    }

    public Map<String, Object> resetPassword(Map<String, String> req, String currentUserEmail) {
        AppUser user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String current = req.get("currentPassword");
        if (!passwordEncoder.matches(current, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.get("newPassword")));
        userRepository.save(user);
        return Map.of("success", true);
    }

    public Map<String, Object> getUserInfo(String userObjectId) {
        AppUser user = userRepository.findByUserObjectId(userObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toPublicMap(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LoginResponse buildLoginResponse(AppUser user) {
        List<String> roles = fromJson(user.getRolesJson());
        String token = jwtUtil.generateToken(user.getEmail(), user.getUserObjectId(), roles);

        return new LoginResponse()
                .accessToken(token)
                .userObjectId(user.getUserObjectId())
                .username(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailAddress(user.getEmail())
                .roles(roles);
    }

    private Map<String, Object> toPublicMap(AppUser user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userObjectId", user.getUserObjectId());
        m.put("username", user.getEmail());
        m.put("firstName", user.getFirstName());
        m.put("lastName", user.getLastName());
        m.put("email", user.getEmail());
        m.put("emailAddress", user.getEmail());
        m.put("roles", fromJson(user.getRolesJson()));
        return m;
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of("User");
        try { return mapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of("User"); }
    }
}
