# 🏫 Campus Retrieve - College Lost & Found Management System

> An enterprise-grade, centralized College Lost & Found Management Platform built with **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, **Smart Heuristic Matching Engine**, and a sleek **Glassmorphic Frontend UI**.

---

## 📌 1. Problem Statement (Kya Problem Hai?)
In university and college campuses, students and staff frequently misplace critical belongings:
- **Common Items:** Student ID cards, mobile phones, earbuds/AirPods, wallets, backpacks, laptops, chargers, bicycle/bike keys, and calculators.
- **Traditional Inefficiencies:**
  - Campus announcements in informal WhatsApp/Telegram groups get buried under hundreds of messages.
  - Notice boards are static, non-searchable, and ignored.
  - Unclaimed items pile up in lost-property or security offices without a mechanism to reach rightful owners.
  - Fraudulent claims occur because there is no structured proof-of-ownership verification.
  - No automated notification or matching between someone reporting a lost item and someone reporting a found item.

---

## 💡 2. Solution Strategy (Kaise Solve Kiya Gaya?)
**Campus Retrieve** solves this through a unified digital platform:
1. **Centralized Digital Intake:**
   - **Report Lost Items:** Capture category, exact last-seen location, timestamp, color, public description, and private secret verification details.
   - **Report Found Items:** Capture finding location, current custody holding place (e.g. Front Library Desk), finder contact, and hidden verification notes.
2. **AI & Heuristic Smart Matching Engine:**
   - When an item is reported, the system scans opposite active items using a multi-factor scoring model:
     - **Category (30%)**: Exact match
     - **Location Proximity (25%)**: Campus building/room keyword similarity
     - **Date Timeline (20%)**: Logical chronological window
     - **Color & Name Token Overlap (25%)**: Jaccard similarity on tokens
   - If confidence exceeds threshold (>= 55%), both the loser and finder receive **instant match alerts**!
3. **Secure Ownership Claim & Verification Flow:**
   - Students submit a claim explaining distinct proof of ownership (e.g., wallet contents, lockscreen wallpaper, internal ID numbers).
   - Staff/Admin cross-references proof against the reporter's secret notes and clicks **Approve** or **Reject**.
   - Item lifecycle automatically transitions: `LOST/FOUND` &rarr; `MATCHED` &rarr; `CLAIMED` &rarr; `RETURNED` &rarr; `CLOSED`.
4. **Role-Based Access Control (RBAC):**
   - **Student:** Report, search, claim, receive match notifications.
   - **Staff:** Log items found in departments/libraries, verify claims.
   - **Admin:** Complete KPI analytics, moderate reports, manage users, audit custody logs.

---

## 🚀 3. Quick Start Guide (How to Run)

### Option A: Double-Click (Windows)
Double-click [`run.bat`](file:///c:/Users/pc/OneDrive/Desktop/ai%20lost&%20found/run.bat) in the project root folder.

### Option B: Terminal Command
```bash
./mvnw.cmd spring-boot:run
```

Once started, open your browser and navigate to:
👉 **[http://localhost:8085](http://localhost:8085)**

---

## 🔑 4. Pre-Seeded Demo Accounts (1-Click Login Ready)

The system automatically initializes realistic campus seed data on startup:

| Role | Email | Password | College ID | Sample Activity |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | `rahul@college.edu` | `Student@123` | `CS202401` | Reported lost black Fossil wallet in Library |
| **Student** | `priya@college.edu` | `Student@123` | `EC202442` | Reported lost Apple AirPods Pro in Cafeteria |
| **Staff** | `staff@college.edu` | `Staff@123` | `STF101` | Handed in found black wallet at Library Desk |
| **Admin** | `admin@college.edu` | `Admin@123` | `ADM001` | Full Dashboard & Claim Verification Queue |

*(Tip: In the Sign-In modal, you can click any of the 1-Click Quick Demo buttons without typing!)*

---

## 🛠️ 5. Technology Stack
- **Backend:** Java 21+ / Java 26, Spring Boot 3.3.4, Spring Web MVC, Spring Data JPA, Spring Security (Stateless JWT + BCrypt)
- **Database:**
  - **Default (Zero Setup):** File-based Persistent H2 Database (`./data/lostfounddb`)
  - **Production:** MySQL / MariaDB (via `--spring.profiles.active=mysql` in `application-mysql.properties`)
- **Frontend:** Vanilla HTML5, Vanilla CSS3 (Custom Glassmorphism, Responsive CSS Grid/Flexbox, Outfit/Inter typography), Modular Vanilla JavaScript SPA
- **Testing:** JUnit 5, Mockito, Spring Boot Starter Test

---

## 📡 6. REST API Reference

### Authentication
- `POST /api/auth/register` - Create student or staff account
- `POST /api/auth/login` - Authenticate and receive JWT token
- `GET /api/auth/me` - Get current session profile

### Lost & Found Items
- `POST /api/items/lost` - Submit lost item report
- `POST /api/items/found` - Submit found item report
- `GET /api/items` - Search and filter items (keyword, category, location, date, type, status)
- `GET /api/items/{id}` - Get item details (with privacy filter on secret proof)
- `GET /api/items/{id}/matches` - Retrieve AI suggested matches for an item
- `GET /api/items/my` - Get items reported by current user
- `PUT /api/items/{id}/status` - Update status (LOST, FOUND, MATCHED, CLAIMED, RETURNED, CLOSED)
- `DELETE /api/items/{id}` - Delete item report

### Ownership Claims
- `POST /api/items/{id}/claim` - Submit proof-of-ownership claim
- `GET /api/claims` - View claims (user's own claims or all claims for staff/admin)
- `PUT /api/claims/{id}/status` - Approve or reject claim with admin notes

### Notifications & Dashboard
- `GET /api/notifications` - Retrieve user notifications
- `GET /api/notifications/unread-count` - Badge counter
- `PUT /api/notifications/{id}/read` - Mark notification as read
- `GET /api/admin/stats` - Comprehensive admin analytics
- `GET /api/admin/stats/public` - Public landing stats counter
