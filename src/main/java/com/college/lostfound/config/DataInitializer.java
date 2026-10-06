package com.college.lostfound.config;

import com.college.lostfound.dto.ItemRequest;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Category;
import com.college.lostfound.model.enums.ItemType;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.UserRepository;
import com.college.lostfound.service.ItemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ItemService itemService;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ItemService itemService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.itemService = itemService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Data already exists
        }

        // 1. Seed Users
        User admin = new User(
                "College Administrator",
                "admin@college.edu",
                "ADM001",
                passwordEncoder.encode("Admin@123"),
                Role.ROLE_ADMIN,
                "+91 98765 43210"
        );
        userRepository.save(admin);

        User staff = new User(
                "Prof. Ramesh Sharma (Staff)",
                "staff@college.edu",
                "STF101",
                passwordEncoder.encode("Staff@123"),
                Role.ROLE_STAFF,
                "+91 98765 43211"
        );
        userRepository.save(staff);

        User student1 = new User(
                "Rahul Sharma",
                "rahul@college.edu",
                "CS202401",
                passwordEncoder.encode("Student@123"),
                Role.ROLE_STUDENT,
                "+91 98765 43212"
        );
        userRepository.save(student1);

        User student2 = new User(
                "Priya Verma",
                "priya@college.edu",
                "EC202442",
                passwordEncoder.encode("Student@123"),
                Role.ROLE_STUDENT,
                "+91 98765 43213"
        );
        userRepository.save(student2);

        // 2. Seed Realistic Items
        // Item 1: Rahul reports Lost Black Wallet in Central Library
        ItemRequest lostWallet = new ItemRequest();
        lostWallet.setName("Black Fossil Leather Wallet");
        lostWallet.setCategory(Category.WALLET_PURSE);
        lostWallet.setDescription("Black bifold Fossil leather wallet. Contains college ID card (CS202401) and metro card with some cash.");
        lostWallet.setLocation("Central Library Reading Hall 2nd Floor");
        lostWallet.setItemDate(LocalDate.now().minusDays(2));
        lostWallet.setItemTime("14:30");
        lostWallet.setColor("Black");
        lostWallet.setSecretDetails("Metro smart card number ending in 8921, small family photo inside secret slot.");
        lostWallet.setContactInfo("rahul@college.edu | +91 98765 43212");
        itemService.createItem(lostWallet, ItemType.LOST, student1);

        // Item 2: Staff reports Found Black Wallet in Central Library (Will trigger auto-match!)
        ItemRequest foundWallet = new ItemRequest();
        foundWallet.setName("Black Leather Wallet");
        foundWallet.setCategory(Category.WALLET_PURSE);
        foundWallet.setDescription("Found a black bifold leather wallet on study table #14 in Central Library.");
        foundWallet.setLocation("Central Library Reading Hall");
        foundWallet.setItemDate(LocalDate.now().minusDays(1));
        foundWallet.setItemTime("16:00");
        foundWallet.setColor("Black");
        foundWallet.setCurrentHoldingLocation("Library Front Helpdesk (Counter 2)");
        foundWallet.setSecretDetails("Contains metro card and student ID.");
        foundWallet.setContactInfo("library-desk@college.edu");
        itemService.createItem(foundWallet, ItemType.FOUND, staff);

        // Item 3: Priya reports Lost Blue AirPods
        ItemRequest lostAirPods = new ItemRequest();
        lostAirPods.setName("Apple AirPods Pro 2nd Gen");
        lostAirPods.setCategory(Category.ELECTRONICS);
        lostAirPods.setDescription("White AirPods with a distinctive navy blue matte silicon protective case with a small brass carabiner clip.");
        lostAirPods.setLocation("Cafeteria Outer Lawn");
        lostAirPods.setItemDate(LocalDate.now().minusDays(3));
        lostAirPods.setItemTime("13:15");
        lostAirPods.setColor("Blue");
        lostAirPods.setSecretDetails("Engraved with letters 'P.V.' on the inner lid.");
        lostAirPods.setContactInfo("priya@college.edu");
        itemService.createItem(lostAirPods, ItemType.LOST, student2);

        // Item 4: Staff reports Found Laptop Charger
        ItemRequest foundCharger = new ItemRequest();
        foundCharger.setName("Dell 65W Type-C Laptop Adapter");
        foundCharger.setCategory(Category.ELECTRONICS);
        foundCharger.setDescription("Black Dell 65-watt USB-C laptop charger left plugged into socket under desk #5.");
        foundCharger.setLocation("Computer Science Lab 3, Block C");
        foundCharger.setItemDate(LocalDate.now().minusDays(1));
        foundCharger.setItemTime("17:45");
        foundCharger.setColor("Black");
        foundCharger.setCurrentHoldingLocation("Lab Assistant Room 301, Block C");
        foundCharger.setSecretDetails("Has small yellow tape on the cable wire.");
        foundCharger.setContactInfo("labassistant.cs@college.edu");
        itemService.createItem(foundCharger, ItemType.FOUND, staff);

        // Item 5: Lost Student ID Card
        ItemRequest lostId = new ItemRequest();
        lostId.setName("Student Identity Card - ECE Department");
        lostId.setCategory(Category.ID_CARDS);
        lostId.setDescription("Official college ID card with red lanyard badge holder. Belongs to Priya Verma (EC202442).");
        lostId.setLocation("Auditorium Hallway");
        lostId.setItemDate(LocalDate.now().minusDays(4));
        lostId.setItemTime("11:00");
        lostId.setColor("Red");
        lostId.setSecretDetails("Red thread tied near the plastic clip hook.");
        lostId.setContactInfo("priya@college.edu");
        itemService.createItem(lostId, ItemType.LOST, student2);

        // Item 6: Found Keys
        ItemRequest foundKeys = new ItemRequest();
        foundKeys.setName("Honda Bike Key with Marvel Keychain");
        foundKeys.setCategory(Category.KEYS);
        foundKeys.setDescription("Single black vehicle key attached to an Iron Man metallic keychain.");
        foundKeys.setLocation("Student Parking Lot Gate 2");
        foundKeys.setItemDate(LocalDate.now());
        foundKeys.setItemTime("09:30");
        foundKeys.setColor("Silver");
        foundKeys.setCurrentHoldingLocation("Main Security Gate Guard Post");
        foundKeys.setSecretDetails("Small scratch across the Iron Man helmet mask.");
        foundKeys.setContactInfo("security@college.edu");
        itemService.createItem(foundKeys, ItemType.FOUND, staff);

        System.out.println("=================================================");
        System.out.println("  COLLEGE LOST & FOUND SYSTEM INITIALIZED!       ");
        System.out.println("  Demo Accounts Created:                         ");
        System.out.println("  - Admin:   admin@college.edu   / Admin@123     ");
        System.out.println("  - Staff:   staff@college.edu   / Staff@123     ");
        System.out.println("  - Student: rahul@college.edu   / Student@123   ");
        System.out.println("  - Student: priya@college.edu   / Student@123   ");
        System.out.println("=================================================");
    }
}
