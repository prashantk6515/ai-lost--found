package com.college.lostfound.dto;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ItemResponse {

    private Long id;
    private String name;
    private Category category;
    private String description;
    private String location;
    private LocalDate itemDate;
    private String itemTime;
    private String color;
    private ItemType type;
    private ItemStatus status;
    private String imageUrl;
    private String secretDetails; // Only populated if authorized
    private String currentHoldingLocation;
    private String contactInfo;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reporterCollegeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOwner;
    private int matchCount;

    public ItemResponse() {}

    public static ItemResponse fromEntity(Item item, Long currentUserId, boolean isAdminOrStaff) {
        ItemResponse res = new ItemResponse();
        res.setId(item.getId());
        res.setName(item.getName());
        res.setCategory(item.getCategory());
        res.setDescription(item.getDescription());
        res.setLocation(item.getLocation());
        res.setItemDate(item.getItemDate());
        res.setItemTime(item.getItemTime());
        res.setColor(item.getColor());
        res.setType(item.getType());
        res.setStatus(item.getStatus());
        res.setImageUrl(item.getImageUrl());
        res.setCurrentHoldingLocation(item.getCurrentHoldingLocation());
        res.setCreatedAt(item.getCreatedAt());
        res.setUpdatedAt(item.getUpdatedAt());

        boolean isUserOwner = currentUserId != null && item.getUser() != null && currentUserId.equals(item.getUser().getId());
        res.setOwner(isUserOwner);

        // Security / Privacy: Only owner, admin, or staff can see secret verification marks
        if (isUserOwner || isAdminOrStaff) {
            res.setSecretDetails(item.getSecretDetails());
            res.setContactInfo(item.getContactInfo());
        } else {
            res.setSecretDetails(null);
            // Non-sensitive masked contact
            res.setContactInfo(item.getContactInfo());
        }

        if (item.getUser() != null) {
            res.setReporterId(item.getUser().getId());
            res.setReporterName(item.getUser().getName());
            if (isUserOwner || isAdminOrStaff) {
                res.setReporterEmail(item.getUser().getEmail());
                res.setReporterCollegeId(item.getUser().getCollegeId());
            }
        }

        return res;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getItemDate() { return itemDate; }
    public void setItemDate(LocalDate itemDate) { this.itemDate = itemDate; }

    public String getItemTime() { return itemTime; }
    public void setItemTime(String itemTime) { this.itemTime = itemTime; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSecretDetails() { return secretDetails; }
    public void setSecretDetails(String secretDetails) { this.secretDetails = secretDetails; }

    public String getCurrentHoldingLocation() { return currentHoldingLocation; }
    public void setCurrentHoldingLocation(String currentHoldingLocation) { this.currentHoldingLocation = currentHoldingLocation; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getReporterCollegeId() { return reporterCollegeId; }
    public void setReporterCollegeId(String reporterCollegeId) { this.reporterCollegeId = reporterCollegeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
}
