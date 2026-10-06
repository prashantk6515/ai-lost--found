package com.college.lostfound.service;

import com.college.lostfound.dto.ItemRequest;
import com.college.lostfound.dto.ItemResponse;
import com.college.lostfound.model.Item;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.MatchRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final MatchingService matchingService;
    private final MatchRecordRepository matchRecordRepository;

    public ItemService(ItemRepository itemRepository,
                       MatchingService matchingService,
                       MatchRecordRepository matchRecordRepository) {
        this.itemRepository = itemRepository;
        this.matchingService = matchingService;
        this.matchRecordRepository = matchRecordRepository;
    }

    @Transactional
    public ItemResponse createItem(ItemRequest req, ItemType type, User user) {
        Item item = new Item();
        item.setName(req.getName());
        item.setCategory(req.getCategory());
        item.setDescription(req.getDescription());
        item.setLocation(req.getLocation());
        item.setItemDate(req.getItemDate());
        item.setItemTime(req.getItemTime());
        item.setColor(req.getColor());
        item.setType(type);
        item.setStatus(type == ItemType.LOST ? ItemStatus.LOST : ItemStatus.FOUND);
        item.setImageUrl(req.getImageUrl());
        item.setSecretDetails(req.getSecretDetails());
        item.setCurrentHoldingLocation(req.getCurrentHoldingLocation());
        item.setContactInfo(req.getContactInfo() != null && !req.getContactInfo().isBlank()
                ? req.getContactInfo() : user.getEmail());
        item.setUser(user);

        Item saved = itemRepository.save(item);

        // Run smart automated matching engine
        matchingService.findAndSaveMatchesForItem(saved);

        return toResponse(saved, user);
    }

    public Page<ItemResponse> searchItems(String keyword,
                                          Category category,
                                          ItemType type,
                                          ItemStatus status,
                                          String location,
                                          LocalDate date,
                                          int page,
                                          int size,
                                          User currentUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Item> itemPage = itemRepository.searchItems(keyword, category, type, status, location, date, pageable);

        return itemPage.map(item -> toResponse(item, currentUser));
    }

    public ItemResponse getItemById(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

        return toResponse(item, currentUser);
    }

    public List<ItemResponse> getItemsByUser(Long userId, User currentUser) {
        return itemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(item -> toResponse(item, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest req, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

        boolean isOwner = currentUser != null && item.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to update this item report.");
        }

        item.setName(req.getName());
        item.setCategory(req.getCategory());
        item.setDescription(req.getDescription());
        item.setLocation(req.getLocation());
        item.setItemDate(req.getItemDate());
        item.setItemTime(req.getItemTime());
        item.setColor(req.getColor());
        if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
            item.setImageUrl(req.getImageUrl());
        }
        if (req.getSecretDetails() != null) {
            item.setSecretDetails(req.getSecretDetails());
        }
        if (req.getCurrentHoldingLocation() != null) {
            item.setCurrentHoldingLocation(req.getCurrentHoldingLocation());
        }
        if (req.getContactInfo() != null) {
            item.setContactInfo(req.getContactInfo());
        }

        Item saved = itemRepository.save(item);
        matchingService.findAndSaveMatchesForItem(saved);

        return toResponse(saved, currentUser);
    }

    @Transactional
    public void deleteItem(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

        boolean isOwner = currentUser != null && item.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to delete this item.");
        }

        itemRepository.delete(item);
    }

    @Transactional
    public ItemResponse updateStatus(Long id, ItemStatus newStatus, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

        boolean isOwner = currentUser != null && item.getUser().getId().equals(currentUser.getId());
        boolean isStaffOrAdmin = currentUser != null &&
                (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_STAFF);

        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("You are not authorized to change the status of this item.");
        }

        item.setStatus(newStatus);
        Item saved = itemRepository.save(item);
        return toResponse(saved, currentUser);
    }

    public Item getItemEntity(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));
    }

    private ItemResponse toResponse(Item item, User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean isAdminOrStaff = currentUser != null &&
                (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_STAFF);

        ItemResponse res = ItemResponse.fromEntity(item, currentUserId, isAdminOrStaff);
        int matches = matchRecordRepository.findAllMatchesForItem(item.getId()).size();
        res.setMatchCount(matches);
        return res;
    }
}
