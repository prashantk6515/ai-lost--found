package com.college.lostfound;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.MatchRecord;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.MatchRecordRepository;
import com.college.lostfound.service.MatchingService;
import com.college.lostfound.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MatchingServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MatchRecordRepository matchRecordRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testMatchingEngineDetectsCloseMatch() {
        User student = new User("Rahul", "rahul@college.edu", "CS01", "pass", Role.ROLE_STUDENT, "123");
        student.setId(1L);

        User staff = new User("Staff", "staff@college.edu", "ST01", "pass", Role.ROLE_STAFF, "456");
        staff.setId(2L);

        // Lost Black Wallet
        Item lostWallet = new Item();
        lostWallet.setId(10L);
        lostWallet.setName("Black Fossil Leather Wallet");
        lostWallet.setCategory(Category.WALLET_PURSE);
        lostWallet.setDescription("Black leather bifold wallet lost in Library");
        lostWallet.setLocation("Central Library Reading Hall");
        lostWallet.setItemDate(LocalDate.now().minusDays(1));
        lostWallet.setColor("Black");
        lostWallet.setType(ItemType.LOST);
        lostWallet.setUser(student);

        // Found Black Wallet
        Item foundWallet = new Item();
        foundWallet.setId(20L);
        foundWallet.setName("Black Leather Wallet");
        foundWallet.setCategory(Category.WALLET_PURSE);
        foundWallet.setDescription("Found black leather wallet on desk in library");
        foundWallet.setLocation("Central Library Reading Hall 2nd Floor");
        foundWallet.setItemDate(LocalDate.now());
        foundWallet.setColor("Black");
        foundWallet.setType(ItemType.FOUND);
        foundWallet.setUser(staff);

        when(itemRepository.findByTypeAndStatusNotIn(eq(ItemType.FOUND), anyList()))
                .thenReturn(List.of(foundWallet));
        when(matchRecordRepository.existsByLostItemIdAndFoundItemId(10L, 20L)).thenReturn(false);
        when(matchRecordRepository.save(any(MatchRecord.class))).thenAnswer(i -> i.getArgument(0));

        List<MatchRecord> matches = matchingService.findAndSaveMatchesForItem(lostWallet);

        assertNotNull(matches);
        assertEquals(1, matches.size());
        MatchRecord record = matches.get(0);
        assertTrue(record.getMatchScore() >= 60.0, "Match score should be high for similar wallet in library: " + record.getMatchScore());
        verify(notificationService, times(1)).createNotification(eq(student), anyString(), anyString(), any(), anyString());
        verify(notificationService, times(1)).createNotification(eq(staff), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testMatchingEngineIgnoresCompletelyUnrelatedItems() {
        User student = new User("Rahul", "rahul@college.edu", "CS01", "pass", Role.ROLE_STUDENT, "123");
        student.setId(1L);

        Item lostItem = new Item();
        lostItem.setId(10L);
        lostItem.setName("Blue Umbrella");
        lostItem.setCategory(Category.OTHER);
        lostItem.setDescription("Navy blue umbrella");
        lostItem.setLocation("Main Gate");
        lostItem.setItemDate(LocalDate.now().minusDays(20));
        lostItem.setColor("Navy");
        lostItem.setType(ItemType.LOST);
        lostItem.setUser(student);

        Item foundItem = new Item();
        foundItem.setId(20L);
        foundItem.setName("Scientific Calculator");
        foundItem.setCategory(Category.ELECTRONICS);
        foundItem.setDescription("Casio FX-991EX calculator");
        foundItem.setLocation("Chemistry Lab");
        foundItem.setItemDate(LocalDate.now());
        foundItem.setColor("Black");
        foundItem.setType(ItemType.FOUND);
        foundItem.setUser(student);

        when(itemRepository.findByTypeAndStatusNotIn(eq(ItemType.FOUND), anyList()))
                .thenReturn(List.of(foundItem));

        List<MatchRecord> matches = matchingService.findAndSaveMatchesForItem(lostItem);

        assertTrue(matches.isEmpty(), "Unrelated items should not trigger a match");
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }
}
