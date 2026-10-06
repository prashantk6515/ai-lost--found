package com.college.lostfound.repository;

import com.college.lostfound.model.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    List<MatchRecord> findByLostItemIdOrderByMatchScoreDesc(Long lostItemId);

    List<MatchRecord> findByFoundItemIdOrderByMatchScoreDesc(Long foundItemId);

    @Query("SELECT m FROM MatchRecord m WHERE m.lostItem.id = :itemId OR m.foundItem.id = :itemId ORDER BY m.matchScore DESC")
    List<MatchRecord> findAllMatchesForItem(@Param("itemId") Long itemId);

    boolean existsByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);

    long countByStatus(String status);
}
