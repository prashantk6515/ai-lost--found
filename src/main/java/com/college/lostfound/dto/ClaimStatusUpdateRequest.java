package com.college.lostfound.dto;

import com.college.lostfound.model.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public class ClaimStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ClaimStatus status;

    private String adminNotes;

    public ClaimStatusUpdateRequest() {}

    public ClaimStatusUpdateRequest(ClaimStatus status, String adminNotes) {
        this.status = status;
        this.adminNotes = adminNotes;
    }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
