package com.insurancechoice.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * Registration / user-creation request.
 * The frontend sends either "email" or "emailAddress" depending on the flow,
 * so both are accepted via @JsonAlias.
 */
public class RegisterRequest {

    private String firstName;
    private String lastName;

    @JsonAlias({"email", "emailAddress"})
    private String email;

    private String password;
    private List<String> role;
    private String userObjectId;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<String> getRole() { return role; }
    public void setRole(List<String> role) { this.role = role; }

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }
}
