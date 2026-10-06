package com.college.lostfound.controller;

import com.college.lostfound.dto.DashboardStats;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.ClaimStatus;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.ClaimRepository;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.MatchRecordRepository;
import com.college.lostfound.repository.UserRepository;
import com.college.lostfound.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final UserService userService;

    public AdminController(UserRepository userRepository,
                           ItemRepository itemRepository,
                           ClaimRepository claimRepository,
                           MatchRecordRepository matchRecordRepository,
                           UserService userService) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.matchRecordRepository = matchRecordRepository;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(buildStats());
    }

    @GetMapping("/stats/public")
    public ResponseEntity<DashboardStats> getPublicStats() {
        return ResponseEntity.ok(buildStats());
    }

    private DashboardStats buildStats() {
        long totalUsers = userRepository.count();
        long totalLostItems = itemRepository.countByType(ItemType.LOST);
        long totalFoundItems = itemRepository.countByType(ItemType.FOUND);
        long potentialMatches = matchRecordRepository.count();
        long pendingClaims = claimRepository.countByStatus(ClaimStatus.PENDING);
        long returnedItems = itemRepository.countByStatus(ItemStatus.RETURNED);
        long closedItems = itemRepository.countByStatus(ItemStatus.CLOSED);

        return new DashboardStats(
                totalUsers,
                totalLostItems,
                totalFoundItems,
                potentialMatches,
                pendingClaims,
                returnedItems,
                closedItems
        );
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userService.getUserById(id);
        Role newRole = Role.valueOf(body.get("role"));
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User role updated successfully", "user", user));
    }
}
