package com.insurancechoice.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Returned by /user/login and /user/register.
 * Field names match exactly what the Angular frontend stores in UserConfigService.
 */
public class LoginResponse {

    private String tokenType = "Bearer";
    private String accessToken;

    /** Serialised as "isAuthenticated" — Jackson strips "is" prefix without this annotation */
    @JsonProperty("isAuthenticated")
    private boolean isAuthenticated = true;

    private String userObjectId;
    private String username;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private List<String> roles;

    // ── Builder-style setters for readability in UserService ─────────────────

    public LoginResponse accessToken(String t) { this.accessToken = t; return this; }
    public LoginResponse userObjectId(String id) { this.userObjectId = id; return this; }
    public LoginResponse username(String u) { this.username = u; return this; }
    public LoginResponse firstName(String f) { this.firstName = f; return this; }
    public LoginResponse lastName(String l) { this.lastName = l; return this; }
    public LoginResponse emailAddress(String e) { this.emailAddress = e; return this; }
    public LoginResponse roles(List<String> r) { this.roles = r; return this; }

    // ── Standard getters (required by Jackson serialisation) ─────────────────

    public String getTokenType() { return tokenType; }
    public String getAccessToken() { return accessToken; }
    public boolean getIsAuthenticated() { return isAuthenticated; }
    public String getUserObjectId() { return userObjectId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmailAddress() { return emailAddress; }
    public List<String> getRoles() { return roles; }
}
