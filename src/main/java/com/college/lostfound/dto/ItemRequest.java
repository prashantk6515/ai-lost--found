package com.college.lostfound.dto;

import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Date is required")
    private LocalDate itemDate;

    private String itemTime;

    private String color;

    private ItemType type; // Can be specified or deduced by endpoint

    private String imageUrl;

    private String secretDetails; // e.g. "student ID on backside", "scratch near logo"

    private String currentHoldingLocation; // For found items

    private String contactInfo;

    public ItemRequest() {}

    // Getters and Setters
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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSecretDetails() { return secretDetails; }
    public void setSecretDetails(String secretDetails) { this.secretDetails = secretDetails; }

    public String getCurrentHoldingLocation() { return currentHoldingLocation; }
    public void setCurrentHoldingLocation(String currentHoldingLocation) { this.currentHoldingLocation = currentHoldingLocation; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
