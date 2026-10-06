package com.college.lostfound.dto;

import com.college.lostfound.model.Claim;
import com.college.lostfound.model.enums.ClaimStatus;
import java.time.LocalDateTime;

public class ClaimResponse {

    private Long id;
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private String itemImageUrl;
    private Long claimantId;
    private String claimantName;
    private String claimantEmail;
    private String claimantCollegeId;
    private String claimantPhone;
    private String proofDescription;
    private String proofImageUrl;
    private ClaimStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public ClaimResponse() {}

    public static ClaimResponse fromEntity(Claim claim) {
        ClaimResponse res = new ClaimResponse();
        res.setId(claim.getId());
        if (claim.getItem() != null) {
            res.setItemId(claim.getItem().getId());
            res.setItemName(claim.getItem().getName());
            res.setItemCategory(claim.getItem().getCategory().name());
            res.setItemImageUrl(claim.getItem().getImageUrl());
        }
        if (claim.getUser() != null) {
            res.setClaimantId(claim.getUser().getId());
            res.setClaimantName(claim.getUser().getName());
            res.setClaimantEmail(claim.getUser().getEmail());
            res.setClaimantCollegeId(claim.getUser().getCollegeId());
            res.setClaimantPhone(claim.getUser().getPhone());
        }
        res.setProofDescription(claim.getProofDescription());
        res.setProofImageUrl(claim.getProofImageUrl());
        res.setStatus(claim.getStatus());
        res.setAdminNotes(claim.getAdminNotes());
        res.setCreatedAt(claim.getCreatedAt());
        res.setResolvedAt(claim.getResolvedAt());
        return res;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }

    public String getItemImageUrl() { return itemImageUrl; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }

    public Long getClaimantId() { return claimantId; }
    public void setClaimantId(Long claimantId) { this.claimantId = claimantId; }

    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }

    public String getClaimantEmail() { return claimantEmail; }
    public void setClaimantEmail(String claimantEmail) { this.claimantEmail = claimantEmail; }

    public String getClaimantCollegeId() { return claimantCollegeId; }
    public void setClaimantCollegeId(String claimantCollegeId) { this.claimantCollegeId = claimantCollegeId; }

    public String getClaimantPhone() { return claimantPhone; }
    public void setClaimantPhone(String claimantPhone) { this.claimantPhone = claimantPhone; }

    public String getProofDescription() { return proofDescription; }
    public void setProofDescription(String proofDescription) { this.proofDescription = proofDescription; }

    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
