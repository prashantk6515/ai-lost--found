package com.college.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

public class ClaimRequest {

    @NotBlank(message = "Proof of ownership description is required")
    private String proofDescription;

    private String proofImageUrl;

    public ClaimRequest() {}

    public ClaimRequest(String proofDescription, String proofImageUrl) {
        this.proofDescription = proofDescription;
        this.proofImageUrl = proofImageUrl;
    }

    public String getProofDescription() { return proofDescription; }
    public void setProofDescription(String proofDescription) { this.proofDescription = proofDescription; }

    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
}
