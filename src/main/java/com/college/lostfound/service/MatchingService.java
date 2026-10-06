package com.college.lostfound.service;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.MatchRecord;
import com.college.lostfound.model.enums.ItemStatus;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.MatchRecordRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MatchingService {

    private final ItemRepository itemRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final NotificationService notificationService;

    public MatchingService(ItemRepository itemRepository,
                           MatchRecordRepository matchRecordRepository,
                           NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.matchRecordRepository = matchRecordRepository;
        this.notificationService = notificationService;
    }

    /**
     * Run matching algorithm when a new or updated item is saved.
     */
    @Transactional
    public List<MatchRecord> findAndSaveMatchesForItem(Item item) {
        if (item.getStatus() == ItemStatus.RETURNED || item.getStatus() == ItemStatus.CLOSED) {
            return Collections.emptyList();
        }

        ItemType candidateType = (item.getType() == ItemType.LOST) ? ItemType.FOUND : ItemType.LOST;
        List<ItemStatus> activeStatuses = Arrays.asList(ItemStatus.LOST, ItemStatus.FOUND, ItemStatus.MATCHED);

        List<Item> candidates = itemRepository.findByTypeAndStatusNotIn(
                candidateType,
                Arrays.asList(ItemStatus.RETURNED, ItemStatus.CLOSED)
        );

        List<MatchRecord> newMatches = new ArrayList<>();

        for (Item candidate : candidates) {
            // Don't match item with itself or items reported by same user if desired
            if (candidate.getId().equals(item.getId())) continue;

            Item lostItem = (item.getType() == ItemType.LOST) ? item : candidate;
            Item foundItem = (item.getType() == ItemType.FOUND) ? item : candidate;

            if (matchRecordRepository.existsByLostItemIdAndFoundItemId(lostItem.getId(), foundItem.getId())) {
                continue; // Already recorded
            }

            MatchEvaluation evaluation = evaluateMatch(lostItem, foundItem);

            if (evaluation.score >= 55.0) {
                MatchRecord record = new MatchRecord(
                        lostItem,
                        foundItem,
                        Math.round(evaluation.score * 10.0) / 10.0,
                        String.join(" | ", evaluation.reasons)
                );
                MatchRecord savedRecord = matchRecordRepository.save(record);
                newMatches.add(savedRecord);

                // Update statuses to MATCHED if currently in base state
                if (lostItem.getStatus() == ItemStatus.LOST) {
                    lostItem.setStatus(ItemStatus.MATCHED);
                    itemRepository.save(lostItem);
                }
                if (foundItem.getStatus() == ItemStatus.FOUND) {
                    foundItem.setStatus(ItemStatus.MATCHED);
                    itemRepository.save(foundItem);
                }

                // Send notifications to both reporters
                notificationService.createNotification(
                        lostItem.getUser(),
                        "Potential Match Found!",
                        "A found item (\"" + foundItem.getName() + "\" at " + foundItem.getLocation() +
                                ") matches your lost \"" + lostItem.getName() + "\" (" + Math.round(evaluation.score) + "% match).",
                        foundItem.getId(),
                        "MATCH_FOUND"
                );

                notificationService.createNotification(
                        foundItem.getUser(),
                        "Potential Owner Found!",
                        "A lost report (\"" + lostItem.getName() + "\") matches the item you found: \"" +
                                foundItem.getName() + "\" (" + Math.round(evaluation.score) + "% match).",
                        lostItem.getId(),
                        "MATCH_FOUND"
                );
            }
        }

        return newMatches;
    }

    public List<MatchRecord> getMatchesForItem(Long itemId) {
        return matchRecordRepository.findAllMatchesForItem(itemId);
    }

    private MatchEvaluation evaluateMatch(Item lost, Item found) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // 1. Category check (Weight: 30%)
        if (lost.getCategory() == found.getCategory()) {
            score += 30.0;
            reasons.add("Matching Category: " + lost.getCategory());
        }

        // 2. Location similarity (Weight: 25%)
        if (lost.getLocation() != null && found.getLocation() != null) {
            String lostLoc = lost.getLocation().toLowerCase().trim();
            String foundLoc = found.getLocation().toLowerCase().trim();

            if (lostLoc.equals(foundLoc)) {
                score += 25.0;
                reasons.add("Exact Location Match: " + lost.getLocation());
            } else if (containsCommonWord(lostLoc, foundLoc)) {
                score += 18.0;
                reasons.add("Proximity Area Match (" + lost.getLocation() + " / " + found.getLocation() + ")");
            }
        }

        // 3. Date Proximity (Weight: 20%)
        if (lost.getItemDate() != null && found.getItemDate() != null) {
            long daysDiff = ChronoUnit.DAYS.between(lost.getItemDate(), found.getItemDate());
            // Found should be on or after lost date, within 14 days
            if (daysDiff >= 0 && daysDiff <= 3) {
                score += 20.0;
                reasons.add("Close Date Timeline (" + daysDiff + " day diff)");
            } else if (daysDiff > 3 && daysDiff <= 10) {
                score += 15.0;
                reasons.add("Plausible Date Window (" + daysDiff + " days later)");
            } else if (daysDiff > 10 && daysDiff <= 30) {
                score += 8.0;
                reasons.add("Within same month");
            } else if (daysDiff < 0 && daysDiff >= -2) {
                // Minor reporting date discrepancy
                score += 10.0;
                reasons.add("Minor date variance");
            }
        }

        // 4. Color Match (Weight: 10%)
        if (lost.getColor() != null && found.getColor() != null) {
            String c1 = lost.getColor().toLowerCase().trim();
            String c2 = found.getColor().toLowerCase().trim();
            if (!c1.isEmpty() && !c2.isEmpty() && (c1.contains(c2) || c2.contains(c1))) {
                score += 10.0;
                reasons.add("Color Match: " + lost.getColor());
            }
        }

        // 5. Name & Keyword Token Overlap (Weight: 15%)
        double nameSimilarity = computeTokenSimilarity(lost.getName(), found.getName());
        if (nameSimilarity > 0.3) {
            double nameScore = Math.min(15.0, nameSimilarity * 15.0);
            score += nameScore;
            reasons.add("Name Similarity (" + Math.round(nameSimilarity * 100) + "%)");
        } else {
            // Check description tokens
            double descSimilarity = computeTokenSimilarity(lost.getDescription(), found.getDescription());
            if (descSimilarity > 0.2) {
                score += 8.0;
                reasons.add("Description Key Details Match");
            }
        }

        return new MatchEvaluation(score, reasons);
    }

    private boolean containsCommonWord(String s1, String s2) {
        Set<String> stopWords = Set.of("the", "in", "at", "near", "floor", "room", "hall", "block", "dept", "department", "and", "of", "a");
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\W+")));
        words1.removeAll(stopWords);
        for (String w : s2.split("\\W+")) {
            if (!stopWords.contains(w) && w.length() > 2 && words1.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private double computeTokenSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        Set<String> stopWords = Set.of("the", "a", "an", "with", "and", "in", "on", "for", "item", "my");
        Set<String> set1 = new HashSet<>();
        for (String w : text1.toLowerCase().split("\\W+")) {
            if (w.length() > 2 && !stopWords.contains(w)) set1.add(w);
        }
        Set<String> set2 = new HashSet<>();
        for (String w : text2.toLowerCase().split("\\W+")) {
            if (w.length() > 2 && !stopWords.contains(w)) set2.add(w);
        }
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private static class MatchEvaluation {
        double score;
        List<String> reasons;

        MatchEvaluation(double score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }
    }
}
