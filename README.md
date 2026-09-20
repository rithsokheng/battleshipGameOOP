# Battleship: Naval Command

A modern Java 21 + JavaFX naval warfare game.

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
- Procedural canvas-rendered ocean waves, flowing ribbon accents, radar sweep, and compass watermark animations (dedicated `decor/` renderer classes).
- Fleet health monitor with live per-ship damage progress bars and attack logs.
- Dual-mode audio system: high-fidelity sound clips with a procedural sound generator fallback, delivered through the swappable `GameAudio` abstraction.

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
│   ├── Orientation.java       # Ship/launcher direction enum (replaces raw booleans)
│   ├── Player.java            # Participant entity (human or AI)
│   ├── ReadOnlyBoard.java     # Read-only board interface for views (no mutation)
│   ├── Ship.java              # Placed ship with idempotent hit tracking
│   ├── ShipType.java          # Fleet classes (size and traits)
│   ├── ShotResult.java        # Outcome record (coordinate, outcome, shipSunk)
│   ├── Theater.java           # Battlefield configuration presets
│   └── Turn.java              # Turn ownership enum (replaces raw int index)
│
├── ai              # AI strategy implementations (no UI dependencies)
│   ├── AIFactory.java         # Strategy factory
│   ├── AIStrategy.java        # Strategy interface
│   ├── AiShotPlan.java        # Weapon, anchor, and orientation plan record
│   ├── Difficulty.java        # Difficulty enum (Ensign, Lieutenant, Admiral)
│   ├── HuntTargetAI.java      # Normal difficulty: parity hunt + target state machine
│   ├── ParityHunter.java      # Shared stateless checkerboard parity hunt component
│   ├── RandomAI.java          # Easy difficulty: random unshot targeting
│   ├── SmartAI.java           # Hard difficulty: probability density mapping
│   └── TargetingQueue.java    # Reusable target queue component (composition)
│
├── controller      # Application services & orchestration
│   ├── BattleService.java     # Turn management, launcher selection & firing pipeline
│   ├── GameController.java    # Thin mediator between views and domain services
│   ├── LauncherFireResult.java# Shot result and sunk ship container
│   ├── NetworkFireService.java# Domain mutations of a network shot (ammo, resupply)
│   ├── PlacementService.java  # Fleet placement legality & auto-deployment
│   ├── ShotResolution.java    # Resolved multi-cell shot outcome record
│   └── ShotResolver.java      # Applies launcher blast patterns to the board
│
├── net             # LAN multiplayer networking (TCP sockets)
│   ├── EnemyTracker.java      # Fog-of-war observer of opponent's board
│   ├── NetMessage.java        # Sealed JSON protocol message hierarchy
│   ├── NetMessageCodec.java   # Gson wire codec with type discriminator
│   ├── NetUtil.java           # IP and port discovery utilities
│   ├── NetworkBattleMediator.java # Controller-level logic for network battles
│   ├── NetworkGameSession.java# Encapsulated session context
│   ├── NetworkSession.java    # Socket listener/sender with async callbacks
│   └── Role.java              # HOST/CLIENT role enum (replaces boolean flag)
│
├── persistence     # Save and load game state (JSON via Gson)
│   ├── GameSaveDTO.java       # Serializable transfer object
│   └── SaveGameService.java   # File storage service
│
└── view            # JavaFX presentation layer
    ├── MainApp.java           # JavaFX Application entry point
    ├── ViewNavigator.java     # Navigation & audio abstraction (DI seam for views)
    ├── MainMenuView.java      # Title screen and options
    ├── GameModeSelectView.java# Mode selector
    ├── BoardSelectView.java   # Theater selector
    ├── AbstractShipPlaceView.java # Shared deployment logic and UI
    ├── ShipPlaceView.java     # Drag-and-drop & auto fleet deployment (local modes)
    ├── NetworkShipPlaceView.java # Network deployment synchronization
    ├── ShipDockPane.java      # Dock tray for unplaced ships
    ├── BoardGridPane.java     # Interactive grid component
    ├── AbstractBattleView.java# Shared combat command center logic and UI
    ├── LocalBattleView.java   # Main combat view (AI & Hotseat)
    ├── NetworkBattleView.java # Authoritative peer combat view
    ├── GameOverView.java      # Victory/Defeat screen
    ├── NetworkGameOverView.java  # Network match outcome screen
    ├── PassScreen.java        # Hotseat turn privacy screen
    ├── MultiplayerLobbyView.java # Host / Join router
    ├── HostLobbyView.java     # QR invite and hosting listener
    ├── JoinLobbyView.java     # Join code input screen
    ├── GameAudio.java         # Audio abstraction (composes SfxAudio, MusicAudio, AudioSettings)
    ├── SfxAudio.java          # Sound effects role interface
    ├── MusicAudio.java        # Background music role interface
    ├── AudioSettings.java     # Volume/mute settings role interface
    ├── SilentAudio.java       # No-op audio stub for unit tests
    ├── SoundManager.java      # BGM and SFX player (GameAudio implementation)
    ├── SoundGenerator.java    # Procedural audio synthesizer fallback
    ├── DecorUtil.java         # Facade over the canvas decor renderers
    ├── decor/                 # Procedural canvas animation renderers
    │   ├── OceanSceneRenderer.java    # Layered ocean waves and sky gradients
    │   ├── OceanRibbonRenderer.java   # Flowing ribbon wave accents
    │   ├── RadarSweepRenderer.java    # Rotating radar sweep animation
    │   └── CompassWatermark.java      # Decorative compass overlay
    ├── MenuOverlays.java      # Shared menu overlay effects
    ├── CssClasses.java        # Centralized CSS style class constants
    ├── ImageResources.java    # Asset cache
    ├── QrCodeUtil.java        # ZXing QR code generator
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
- **Java 21** or higher
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

### Run Tests

```bash
mvn test
```

Unit tests (JUnit 5) run headlessly — no JavaFX runtime required — and cover the AI strategies (`ParityHunterTest`), controller services (`BattleServiceTest`, `ShotResolverTest`, `GameControllerDIPTest`), domain model (`TurnTest`), networking (`NetworkGameSessionTest`, `NetworkBattleMediatorTest`), persistence (`SaveGameServiceTest`), and audio abstractions (`SilentAudioTest`).

---

## Key OOP Principles Implemented

- **Polymorphism over Conditionals:** `LauncherType` enums implement `getTargetCells(...)` directly, eliminating external switch statement logic.
- **Composition over Inheritance:** `SmartAI` and `HuntTargetAI` compose independent `TargetingQueue` and `ParityHunter` components rather than sharing deep inheritance trees with shadowed state.
- **Strict Encapsulation & Immutability:** 
  - `Board.getShips()` and `EnemyTracker.getKnownSunkShips()` return unmodifiable collections (`Collections.unmodifiableList`).
  - `Ship` hit tracking uses an internal `Set<Coordinate>` for idempotent hit registration, preventing duplicate counting.
  - `NetworkGameSession` encapsulates networking and state fields behind controlled getters and thread-safe volatile flags.
- **Read-Only Exposure:** Views query board state through the `ReadOnlyBoard` interface, so cell/ship state can be read but never mutated from the presentation layer.
- **Interface Segregation & Dependency Inversion:** Views depend on the `ViewNavigator` and `GameAudio` abstractions — the latter composed of the narrow `SfxAudio`, `MusicAudio`, and `AudioSettings` roles — instead of the concrete `MainApp` or the static `SoundManager` singleton. `SilentAudio` serves as a no-op stub injected in tests.
- **No Primitive Obsession:** Raw flags and indices are replaced with self-documenting enums: `Orientation` (ship/launcher direction), `Turn` (whose turn it is), and `Role` (HOST/CLIENT in network play).
- **Single Responsibility:** Firing (`BattleService`, `ShotResolver`), placement (`PlacementService`), and network-shot domain logic (`NetworkFireService`, `NetworkBattleMediator`) live in focused services, keeping `GameController` and the views as thin mediators.
- **Domain Independence:** Model and AI logic execute entirely independently of UI frameworks, making domain logic unit-testable without JavaFX initialization.
