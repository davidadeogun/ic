package com.insurancechoice.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Login request from the Angular frontend.
 *
 * NOTE: The frontend sends the password field as "passoword" (missing 'i') —
 * that typo is preserved in the original codebase.  @JsonAlias accepts both
 * spellings so the backend works whether or not it is ever fixed.
 */
public class LoginRequest {

    private String email;

    @JsonAlias({"passoword", "password"})
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
