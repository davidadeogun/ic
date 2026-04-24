package com.insurancechoice.backend.dto;

/**
 * Request body for POST /prompt/build.
 * mode must be "DM" (Decision Making) or "PS" (Problem Solving).
 */
public class PromptRequest {
    private String userObjectId;
    private String mode;

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
