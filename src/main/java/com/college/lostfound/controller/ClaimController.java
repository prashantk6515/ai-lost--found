package com.college.lostfound.controller;

import com.college.lostfound.dto.ClaimRequest;
import com.college.lostfound.dto.ClaimResponse;
import com.college.lostfound.dto.ClaimStatusUpdateRequest;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.service.ClaimService;
import com.college.lostfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ClaimController {

    private final ClaimService claimService;
    private final UserService userService;

    public ClaimController(ClaimService claimService, UserService userService) {
        this.claimService = claimService;
        this.userService = userService;
    }

    @PostMapping("/api/items/{id}/claim")
    public ResponseEntity<?> submitClaim(@PathVariable Long id, @Valid @RequestBody ClaimRequest req) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to submit an ownership claim"));
        }
        try {
            ClaimResponse response = claimService.submitClaim(id, req, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/claims")
    public ResponseEntity<?> getClaims() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_STAFF) {
            return ResponseEntity.ok(claimService.getAllClaims());
        } else {
            return ResponseEntity.ok(claimService.getClaimsByUser(user.getId()));
        }
    }

    @GetMapping("/api/claims/my")
    public ResponseEntity<?> getMyClaims() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(claimService.getClaimsByUser(user.getId()));
    }

    @GetMapping("/api/claims/item/{itemId}")
    public ResponseEntity<?> getClaimsByItem(@PathVariable Long itemId) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(claimService.getClaimsByItem(itemId));
    }

    @PutMapping("/api/claims/{id}/status")
    public ResponseEntity<?> updateClaimStatus(@PathVariable Long id, @Valid @RequestBody ClaimStatusUpdateRequest req) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        try {
            ClaimResponse response = claimService.updateClaimStatus(id, req, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
