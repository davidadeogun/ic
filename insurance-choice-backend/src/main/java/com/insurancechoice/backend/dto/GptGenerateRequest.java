package com.insurancechoice.backend.dto;

/**
 * Request body for POST /gpt/generate.
 * problemType: 1 = Decision Making, 3 = Problem Solving
 */
public class GptGenerateRequest {
    private String userObjectId;
    private int problemType;

    public String getUserObjectId() { return userObjectId; }
    public void setUserObjectId(String userObjectId) { this.userObjectId = userObjectId; }

    public int getProblemType() { return problemType; }
    public void setProblemType(int problemType) { this.problemType = problemType; }
}
