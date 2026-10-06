package com.college.lostfound.service;

import com.college.lostfound.dto.ClaimRequest;
import com.college.lostfound.dto.ClaimResponse;
import com.college.lostfound.dto.ClaimStatusUpdateRequest;
import com.college.lostfound.model.Claim;
import com.college.lostfound.model.Item;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.ClaimStatus;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.ClaimRepository;
import com.college.lostfound.repository.ItemRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    public ClaimService(ClaimRepository claimRepository,
                        ItemRepository itemRepository,
                        NotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ClaimResponse submitClaim(Long itemId, ClaimRequest req, User claimant) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemId));

        if (item.getUser().getId().equals(claimant.getId())) {
            throw new IllegalArgumentException("You cannot submit a claim for an item you reported yourself.");
        }

        if (claimRepository.existsByItemIdAndUserIdAndStatus(itemId, claimant.getId(), ClaimStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending claim for this item.");
        }

        Claim claim = new Claim();
        claim.setItem(item);
        claim.setUser(claimant);
        claim.setProofDescription(req.getProofDescription());
        claim.setProofImageUrl(req.getProofImageUrl());
        claim.setStatus(ClaimStatus.PENDING);

        Claim saved = claimRepository.save(claim);

        // Notify item finder/reporter
        notificationService.createNotification(
                item.getUser(),
                "New Claim Submitted",
                claimant.getName() + " submitted an ownership claim for \"" + item.getName() + "\".",
                item.getId(),
                "CLAIM_RECEIVED"
        );

        return ClaimResponse.fromEntity(saved);
    }

    @Transactional
    public ClaimResponse updateClaimStatus(Long claimId, ClaimStatusUpdateRequest req, User reviewer) {
        if (reviewer.getRole() != Role.ROLE_ADMIN && reviewer.getRole() != Role.ROLE_STAFF) {
            throw new AccessDeniedException("Only Staff or Admin can review and update claims.");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found with id: " + claimId));

        claim.setStatus(req.getStatus());
        claim.setAdminNotes(req.getAdminNotes());
        claim.setResolvedAt(LocalDateTime.now());

        Item item = claim.getItem();

        if (req.getStatus() == ClaimStatus.APPROVED) {
            item.setStatus(ItemStatus.CLAIMED);
            itemRepository.save(item);

            // Notify Claimant
            String holdingLoc = item.getCurrentHoldingLocation() != null ? item.getCurrentHoldingLocation() : "Campus Lost & Found Office";
            notificationService.createNotification(
                    claim.getUser(),
                    "Claim Approved!",
                    "Your claim for \"" + item.getName() + "\" has been APPROVED! Please visit " + holdingLoc + " to verify and collect your item.",
                    item.getId(),
                    "CLAIM_APPROVED"
            );
        } else if (req.getStatus() == ClaimStatus.REJECTED) {
            // Notify Claimant
            String reason = req.getAdminNotes() != null ? " Reason: " + req.getAdminNotes() : "";
            notificationService.createNotification(
                    claim.getUser(),
                    "Claim Update",
                    "Your claim for \"" + item.getName() + "\" was not approved." + reason,
                    item.getId(),
                    "CLAIM_REJECTED"
            );
        }

        Claim saved = claimRepository.save(claim);
        return ClaimResponse.fromEntity(saved);
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll()
                .stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getClaimsByItem(Long itemId) {
        return claimRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getClaimsByUser(Long userId) {
        return claimRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getPendingClaims() {
        return claimRepository.findByStatusOrderByCreatedAtDesc(ClaimStatus.PENDING)
                .stream()
                .map(ClaimResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
