# Battleship: Naval Command

A modern Java 17 + JavaFX naval warfare game.

## Features

### Game Modes
- **Single Player (vs AI):** Battle against three distinct AI difficulty tiers.
- **Pass & Play (Hotseat):** Local two-player mode with a private handoff pass screen between turns.
- **LAN Multiplayer ("Play with a Friend"):** Direct peer-to-peer TCP socket connection across local networks:
  - Scannable QR code (via ZXing) or shareable join string (`BATTLESHIP:<ip>:<port>:<code>`).
  - Strict Fog-of-War: Real ship placements are never transmitted over the wire; each client is authoritative only over its own board and reports shot outcomes (`MISS`, `HIT`, `SUNK`).

### Battlefields & Theaters
- **Coastal Waters (5×5):** Fast skirmish with Patrol Boat (2), Destroyer (2), and Submarine (3).
- **Open Sea (8×8):** Tactical battle with Destroyer (2), Submarine (3), Cruiser (3), and Battleship (4).
- **Pacific Theater (10×10):** Full fleet warfare adding the Aircraft Carrier (5).

### Advanced Weaponry & Launcher System
- **Default Shell:** Precise 1×1 shot with infinite ammo.
- **Level 2 Salvo:** 1×3 line barrage available on larger grids (8×8 and 10×10).
- **Tactical Nuclear Warhead:** 2×3 area devastation available across all theaters.
  - **Launch Code Protocol:** Firing requires answering naval trivia questions to authorize detonation.
  - **Auto-Resupply:** Automatically initiates a resupply countdown and drill to restock warheads.
- **Orientation & Target Preview:** Press **`R`** or **Right-Click** to rotate weapon trajectory with real-time ghost overlay.

### AI Strategies (Strategy & Composition Pattern)
- **Ensign (Easy):** Uniform random targeting across unshot coordinates.
- **Lieutenant (Normal):** Checkerboard parity hunt exploiting minimum ship size (length 2), transitioning to targeted neighbor pursuit upon impact.
- **Admiral (Hard):** Probability density mapping that computes the number of valid remaining ship configurations for every cell, paired with opportunistic area-weapon bombardment and line-following targeting.

### Audio & Visuals
- Procedural canvas-rendered ocean waves and radar sweep animations.
- Fleet health monitor with live per-ship damage progress bars and attack logs.
- Dual-mode audio system: high-fidelity sound clips with a procedural sound generator fallback.

---

## Architecture & Package Layout

Strict separation of concerns is enforced: `model` and `ai` contain pure Java domain logic and have **zero** JavaFX or UI dependencies.

```
com.battleship
├── model           # Pure domain entities and aggregate roots
│   ├── AmmoInventory.java     # Centralized launcher ammo tracking
│   ├── Board.java             # Aggregate root: grid status, ships, shot resolution
│   ├── CellStatus.java        # Cell enum (EMPTY, SHIP, HIT, MISS, SUNK)
│   ├── Coordinate.java        # Immutable board coordinate (row, col)
│   ├── GameMode.java          # Mode selector (AI, Hotseat, etc.)
│   ├── GameState.java         # Game state machine
│   ├── LauncherType.java      # Polymorphic weapon types with blast patterns
│   ├── Player.java            # Participant entity (human or AI)
│   ├── Ship.java              # Placed ship with idempotent hit tracking
│   ├── ShipType.java          # Fleet classes (size and traits)
│   ├── ShotResult.java        # Outcome record (coordinate, outcome, shipSunk)
│   └── Theater.java           # Battlefield configuration presets
│
├── ai              # AI strategy implementations (no UI dependencies)
│   ├── AIFactory.java         # Strategy factory
│   ├── AIStrategy.java        # Strategy interface
│   ├── AiShotPlan.java        # Weapon, anchor, and orientation plan record
│   ├── Difficulty.java        # Difficulty enum (Ensign, Lieutenant, Admiral)
│   ├── HuntTargetAI.java      # Normal difficulty: parity hunt + target state machine
│   ├── RandomAI.java          # Easy difficulty: random unshot targeting
│   ├── SmartAI.java           # Hard difficulty: probability density mapping
│   └── TargetingQueue.java    # Reusable target queue component (composition)
│
├── controller      # Application services & orchestration
│   ├── GameController.java    # Orchestrates state flow between View and Model
│   └── LauncherFireResult.java# Shot result and sunk ship container
│
├── net             # LAN multiplayer networking (TCP sockets)
│   ├── EnemyTracker.java      # Fog-of-war observer of opponent's board
│   ├── NetMessage.java        # JSON protocol message
│   ├── NetUtil.java           # IP and port discovery utilities
│   ├── NetworkGameSession.java# Encapsulated session context
│   └── NetworkSession.java    # Socket listener/sender with async callbacks
│
├── persistence     # Save and load game state (JSON via Gson)
│   ├── GameSaveDTO.java       # Serializable transfer object
│   └── SaveGameService.java   # File storage service
│
└── view            # JavaFX presentation layer
    ├── MainApp.java           # JavaFX Application entry point
    ├── MainMenuView.java      # Title screen and options
    ├── GameModeSelectView.java# Mode selector
    ├── BoardSelectView.java   # Theater selector
    ├── ShipPlaceView.java     # Drag-and-drop & auto fleet deployment
    ├── ShipDockPane.java      # Dock tray for unplaced ships
    ├── BoardGridPane.java     # Interactive grid component
    ├── BattleView.java        # Main combat command center
    ├── GameOverView.java      # Victory/Defeat screen
    ├── PassScreen.java        # Hotseat turn privacy screen
    ├── MultiplayerLobbyView.java # Host / Join router
    ├── HostLobbyView.java     # QR invite and hosting listener
    ├── JoinLobbyView.java     # Join code input screen
    ├── NetworkShipPlaceView.java # Network deployment synchronization
    ├── NetworkBattleView.java # Authoritative peer combat view
    ├── NetworkGameOverView.java  # Network match outcome screen
    ├── DecorUtil.java         # Ocean canvas and radar animations
    ├── ImageResources.java    # Asset cache
    ├── QrCodeUtil.java        # ZXing QR code generator
    ├── SoundGenerator.java    # Procedural audio synthesizer
    ├── SoundManager.java      # BGM and SFX player
    └── quiz/                  # Nuclear authorization & resupply minigames
        ├── NuclearLaunchDialog.java
        ├── NuclearResupplyDialog.java
        ├── QuizBank.java
        └── QuizQuestion.java
```

---

## Controls & Shortcuts

| Action | Control |
|:---|:---|
| **Place Ship** | Drag from Dock onto Grid or Click dock then target cell |
| **Rotate Ship / Weapon** | **`R`** key or **Right-Click** |
| **Remove Placed Ship** | Click placed ship on grid during deployment |
| **Fire Weapon** | Select weapon from weapon bar, then click target cell on enemy grid |
| **Auto Deploy** | Click `AUTO PLACE` in deployment dock |

---

## Prerequisites & Build

### Requirements
- **Java 17** or higher
- **Maven 3.8+**

### Compile & Run

```bash
# Compile and build classes
mvn clean compile

# Launch the game with JavaFX Maven Plugin
mvn javafx:run
```

### Package Executable JAR

```bash
mvn clean package
java -jar target/naval-command-1.0.0.jar
```

---

## Key OOP Principles Implemented

- **Polymorphism over Conditionals:** `LauncherType` enums implement `getTargetCells(...)` directly, eliminating external switch statement logic.
- **Composition over Inheritance:** `SmartAI` and `HuntTargetAI` compose independent `TargetingQueue` components rather than sharing deep inheritance trees with shadowed state.
- **Strict Encapsulation & Immutability:** 
  - `Board.getShips()` and `EnemyTracker.getKnownSunkShips()` return unmodifiable collections (`Collections.unmodifiableList`).
  - `Ship` hit tracking uses an internal `Set<Coordinate>` for idempotent hit registration, preventing duplicate counting.
  - `NetworkGameSession` encapsulates networking and state fields behind controlled getters and thread-safe volatile flags.
- **Domain Independence:** Model and AI logic execute entirely independently of UI frameworks, making domain logic unit-testable without JavaFX initialization.
