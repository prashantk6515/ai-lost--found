package com.college.lostfound.repository;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findByTypeOrderByCreatedAtDesc(ItemType type);

    List<Item> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);

    List<Item> findByTypeAndStatusNotIn(ItemType type, List<ItemStatus> excludedStatuses);

    long countByType(ItemType type);

    long countByStatus(ItemStatus status);

    @Query("SELECT i FROM Item i WHERE " +
           "(:keyword IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.color) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:type IS NULL OR i.type = :type) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:location IS NULL OR LOWER(i.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:itemDate IS NULL OR i.itemDate = :itemDate) " +
           "ORDER BY i.createdAt DESC")
    Page<Item> searchItems(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("type") ItemType type,
            @Param("status") ItemStatus status,
            @Param("location") String location,
            @Param("itemDate") LocalDate itemDate,
            Pageable pageable
    );

    @Query("SELECT i FROM Item i WHERE i.type = :targetType AND i.status IN :activeStatuses AND " +
           "(i.category = :category OR LOWER(i.location) LIKE LOWER(CONCAT('%', :locationFragment, '%')))")
    List<Item> findCandidatesForMatching(
            @Param("targetType") ItemType targetType,
            @Param("activeStatuses") List<ItemStatus> activeStatuses,
            @Param("category") Category category,
            @Param("locationFragment") String locationFragment
    );
}
