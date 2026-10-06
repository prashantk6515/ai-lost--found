package com.college.lostfound.repository;

import com.college.lostfound.model.Claim;
import com.college.lostfound.model.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByItemIdOrderByCreatedAtDesc(Long itemId);
    List<Claim> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Claim> findByStatusOrderByCreatedAtDesc(ClaimStatus status);
    boolean existsByItemIdAndUserIdAndStatus(Long itemId, Long userId, ClaimStatus status);
    long countByStatus(ClaimStatus status);
}
