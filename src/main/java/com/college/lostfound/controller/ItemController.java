package com.college.lostfound.controller;

import com.college.lostfound.dto.ItemRequest;
import com.college.lostfound.dto.ItemResponse;
import com.college.lostfound.model.Item;
import com.college.lostfound.model.MatchRecord;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.service.ItemService;
import com.college.lostfound.service.MatchingService;
import com.college.lostfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;
    private final MatchingService matchingService;

    public ItemController(ItemService itemService, UserService userService, MatchingService matchingService) {
        this.itemService = itemService;
        this.userService = userService;
        this.matchingService = matchingService;
    }

    @PostMapping("/lost")
    public ResponseEntity<?> reportLostItem(@Valid @RequestBody ItemRequest req) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to report a lost item"));
        }
        ItemResponse response = itemService.createItem(req, ItemType.LOST, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/found")
    public ResponseEntity<?> reportFoundItem(@Valid @RequestBody ItemRequest req) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to report a found item"));
        }
        ItemResponse response = itemService.createItem(req, ItemType.FOUND, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponse>> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User currentUser = userService.getCurrentUser();
        Page<ItemResponse> items = itemService.searchItems(keyword, category, type, status, location, date, page, size, currentUser);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(@PathVariable Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            ItemResponse response = itemService.getItemById(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReportedItems() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        List<ItemResponse> items = itemService.getItemsByUser(user.getId(), user);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest req) {
        try {
            User currentUser = userService.getCurrentUser();
            ItemResponse response = itemService.updateItem(id, req, currentUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            itemService.deleteItem(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateItemStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            User currentUser = userService.getCurrentUser();
            ItemStatus newStatus = ItemStatus.valueOf(body.get("status"));
            ItemResponse response = itemService.updateStatus(id, newStatus, currentUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<?> getItemMatches(@PathVariable Long id) {
        try {
            List<MatchRecord> matches = matchingService.getMatchesForItem(id);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
