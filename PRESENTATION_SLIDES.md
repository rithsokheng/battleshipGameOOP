---
marp: true
theme: default
paginate: true
header: "Battleship: Naval Command — Final OOP Project Presentation"
footer: "បង្រៀនដោយ៖ លោក គិត តារា | Java 21 + JavaFX"
---

<!-- Slide 1: Title Slide -->
# 🚢 Battleship: Naval Command
### Tactical Naval Warfare Simulator Built with Modern OOP & Clean Architecture
**Final Project Presentation — Advanced Object-Oriented Programming (OOP)**

---

**បង្រៀនដោយ៖** លោក **គិត តារា**  
**Academic Year:** 2025 – 2026  
**Language & Platform:** Java 21 LTS · JavaFX 21 · Maven  

---

<!-- Slide 2: Contents / Table of Contents -->
# Contents

1. **Introduction**
2. **Objective**
3. **Inspiration**
4. **Problem Statement**
5. **Project Features** (Exam Scope: Software & Game)
6. **Technology In Used** (High-Level Architecture & Stack)
7. **Future Plan**
8. **Conclusion**
9. **Our Team Members & Roles**
10. **Q&A / Thank You**

---

<!-- Slide 3: Introduction & Objective -->
# Introduction & Objective

### 1. Introduction
* **Battleship: Naval Command** is a high-performance desktop naval combat simulator developed in **Java 21** using **JavaFX**.
* Re-engineers the classic naval strategy game into an enterprise-grade, object-oriented application.
* Designed with a strict **Clean Architecture**: complete separation of pure domain logic, AI decision trees, local/LAN networking, and UI rendering.

### 2. Objective
* **Mastery of OOP Core Pillars:** Strict Encapsulation, Polymorphism over conditionals, Abstraction via role interfaces, and Inheritance/Composition.
* **SOLID Design Principles:** Implement real-world design patterns (Strategy, Factory, DTO, Template Method, Observer).
* **Headless Testability:** 100% decoupling of game mechanics from UI framework, enabling comprehensive JUnit 5 unit testing.
* **Multiplayer & Networking:** Deliver frictionless local hotseat play and peer-to-peer (P2P) TCP socket LAN multiplayer with QR code pairing.

---

<!-- Slide 4: Introduction (cont.) — Inspiration -->
# Introduction (cont.)

### 3. Inspiration
* **The 1931 Classic Battleship & Salvo Strategy:** Transforming static pen-and-paper naval guessing into an immersive tactical desktop war room.
* **Modern Naval Command Centers (C4ISR):**
  * Live radar sweep animations and procedural ocean wave canvas rendering.
  * Multi-tiered weapon arsenals (Salvo Barrages and Tactical Nuclear Strikes).
  * Strict naval Fog-of-War tracking screens.
* **Elevating Academic Projects:**
  * Moving beyond simple console exercises into production-grade software engineering.
  * Implementing genuine computer science algorithms: checkerboard parity heuristics and probability density mapping for autonomous AI.

---

<!-- Slide 5: Introduction (cont.) — Problem Statement -->
# Introduction (cont.)

### 4. Problem Statement
* **Information Leakage & Cheating in Multiplayer:**
  * *Challenge:* In hotseat or network games, exposing board state or memory coordinates allows players to see enemy ships.
  * *Solution:* Cryptic Fog-of-War encapsulation (`TrackingGrid` vs. `PrimaryGrid`); network packets only transmit shot coordinates and outcome flags (`HIT`, `MISS`, `SUNK`), never fleet coordinates.
* **Spaghetti Code in Game Development:**
  * *Challenge:* Junior game architectures tightly couple UI buttons and drawing routines with win/loss logic and ship coordinates.
  * *Solution:* Strict Layered Architecture (`model` and `ai` have zero JavaFX imports; `view` communicates only through navigator and controller abstractions).
* **Predictable & Clunky AI:**
  * *Challenge:* Naive game AI shoots randomly or gets stuck in repetitive loops.
  * *Solution:* 3 calibrated AI tiers ranging from stochastic exploration to probabilistic density calculation.
* **Complex LAN Setup:**
  * *Challenge:* Typing manual IP addresses and port configurations creates friction for players.
  * *Solution:* Instant QR code generation (ZXing) and shareable single-string invite codes (`BATTLESHIP:<ip>:<port>:<code>`).

---

<!-- Slide 6: Project Features -->
# Project Features
### Final Exam Project Scope & Deliverables

#### 1. Software Architecture & Engineering
* **Interactive JavaFX GUI:** Custom glassmorphism styling, procedural animated ocean canvas, radar sweep, sound FX, and responsive grid layouts.
* **Robust File & State Persistence:** Complete JSON serialization/deserialization via Google Gson; save and resume active matches seamlessly (`SaveGameService` + DTO mappers).
* **Headless Automated Testing:** 45 automated unit tests across 16 test suites ensuring rock-solid domain rules without needing a display server.
* **Score & Evaluation:** Integrated `MatchStatistics` system tracking total shots, accuracy %, hits, misses, and fleet survivability (Individual + Team metrics).

#### 2. Game Mechanics & Systems
* **3 Distinct Game Modes:**
  * *Single Player:* Fight against 3 AI tiers (Ensign, Lieutenant, Admiral).
  * *Pass & Play (Hotseat):* Local two-player combat with private `PassScreen` handoff.
  * *LAN Multiplayer:* Real-time peer-to-peer direct socket connection across local WiFi.
* **3 Battlefield Theaters:** Skirmish ($5 \times 5$), Tactical Engagement ($8 \times 8$), Classic Fleet Action ($10 \times 10$).
* **Polymorphic Weapon Arsenal:** Standard Shell ($1 \times 1$), Salvo Barrage ($1 \times 3$), and Tactical Nuclear Warhead ($2 \times 3$).
* **Naval Trivia Authorization:** Launching nuclear warheads requires correctly answering naval trivia launch codes, complete with resupply drills!

---

<!-- Slide 7: Technology In Used -->
# Technology In Used
### High-Level Architecture & Technical Stack

```
┌────────────────────────────────────────────────────────────────────────┐
│                        VIEW LAYER (JavaFX 21)                          │
│   MainApp · BoardGridPane · ShipDockPane · Procedural Canvas Decor     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (ViewNavigator / Controller)
┌───────────────────────────────────▼────────────────────────────────────┐
│                  CONTROLLER & ORCHESTRATION LAYER                      │
│   BattleService · PlacementService · ShotResolver · NetworkMediator    │
└──────────────┬──────────────────────────────────────────┬──────────────┘
               │                                          │
┌──────────────▼──────────────────────────┐ ┌─────────────▼──────────────┐
│       DOMAIN MODEL (Pure Java)          │ │       AI STRATEGY LAYER    │
│ PrimaryGrid · TrackingGrid · Player     │ │ RandomAI (Ensign)          │
│ Ship · Weapon Hierarchy · Arsenal       │ │ HuntTargetAI (Lieutenant)  │
│ MatchStatistics · GameState Machine     │ │ SmartAI (Admiral)          │
└──────────────┬──────────────────────────┘ └────────────────────────────┘
               │
┌──────────────▼─────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE, NETWORKING & PERSISTENCE               │
│  Java Sockets (TCP P2P) · Gson 2.11 (JSON) · ZXing 3.5 (QR Codes)     │
│  Maven Shade (Fat JAR) · JUnit 5 (Headless Testing) · Java 21 LTS      │
└────────────────────────────────────────────────────────────────────────┘
```

* **Core Stack:** Java 21 LTS (Records, Sealed Types, Pattern Matching), Maven 3.8+.
* **GUI & Graphics:** OpenJFX 21 (`javafx-controls`, `javafx-fxml`, Canvas 2D).
* **Networking & Protocols:** Standard Java Non-blocking / TCP Sockets, Custom JSON Wire Codec.
* **Persistence & Tooling:** Google Gson, ZXing QR Library, JUnit Jupiter 5.10.

---

<!-- Slide 8: Future Plan -->
# Future Plan
### Roadmap & Future Enhancements

* **Cloud Matchmaking & WAN Relay Server:**
  * Introduce a lightweight Spring Boot / WebSocket relay server to enable online matchmaking over the internet without requiring local LAN or router port-forwarding.
* **Global Accounts & Competitive Ranking:**
  * Centralized leaderboard and player profiles recording career wins, battle accuracy, and Admiral rank badges.
* **Aviation & Reconnaissance Fleet Units:**
  * Aircraft Carrier reconnaissance planes to perform radar flyovers over fog-of-war sectors.
  * Submarine stealth sonar pings and smoke-screen counter-measures.
* **Cross-Platform Native Bundling:**
  * Package the application with `jpackage` and GraalVM Native Image to generate standalone native executables (`.exe` for Windows, `.dmg` for macOS, `.deb`/AppImage for Linux) without requiring pre-installed JREs.

---

<!-- Slide 9: Conclusion -->
# Conclusion

* **Excellence in OOP & Software Craftsmanship:**
  * Successfully demonstrated OOP principles and clean design patterns (Strategy, Factory, DTO, SRP, ISP, OCP).
  * Zero coupling between presentation layer and core game logic.
* **Production-Ready & Fully Verified:**
  * 45 unit tests validate edge cases, weapon blast physics, AI heuristics, and network serialization headlessly.
* **Engaging & Frictionless User Experience:**
  * Smooth animations, responsive drag-and-drop fleet deployment, multiple board sizes, and instant QR-based LAN matchmaking.
* **Proudly Built as an Academic & Engineering Showcase** for Cambodia's next generation of software engineers!

---

<!-- Slide 10: Our Team Members -->
# Our Team Members & Roles

<div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; text-align: center;">

<div>
  <h3>👨‍🏫 Mentor / Lecturer</h3>
  <p><strong>លោក គិត តារា</strong></p>
  <p><em>Course Instructor & Technical Advisor</em></p>
</div>

<div>
  <h3>👑 Team Lead & Architect</h3>
  <p><strong>[Student Name 1]</strong></p>
  <p><em>Architecture, Game Engine & Project Coordination</em></p>
</div>

<div>
  <h3>🎨 Frontend & JavaFX UX/UI</h3>
  <p><strong>[Student Name 2]</strong></p>
  <p><em>Scene Graph, Procedural Canvas & Sound Synthesis</em></p>
</div>

<div>
  <h3>🧠 AI & Heuristic Algorithms</h3>
  <p><strong>[Student Name 3]</strong></p>
  <p><em>Parity Hunting & Probability Density Logic</em></p>
</div>

<div>
  <h3>🌐 Networking & Persistence</h3>
  <p><strong>[Student Name 4]</strong></p>
  <p><em>TCP P2P Protocol, QR Pairing & JSON Save System</em></p>
</div>

<div>
  <h3>🧪 QA & Automated Testing</h3>
  <p><strong>[Student Name 5]</strong></p>
  <p><em>JUnit 5 Test Suites & CI/Build Verification</em></p>
</div>

</div>

---

<!-- Slide 11: Thank You -->
# Thank You!

### Questions & Answers
**Please have a wonderful day..!**

```
🚢 Battleship: Naval Command
Repository: github.com/seavminhleang-art/BattleShip-Game
Built with Java 21, JavaFX & Maven
```
