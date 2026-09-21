# Battleship: Naval Command

A modern Java 21 + JavaFX naval warfare game.
A modern Java 21 + JavaFX naval warfare game built with strict object-oriented design and clean architecture.

---

## Features

### Game Modes
- **Single Player (vs AI):** Battle against three distinct AI difficulty tiers.
- **Pass & Play (Hotseat):** Local two-player mode with a private handoff pass screen between turns.
- **Single Player (vs AI):** Battle against three distinct AI difficulty tiers (Ensign, Lieutenant, Admiral).
- **Pass & Play (Hotseat):** Local two-player mode with a private handoff pass screen (`PassScreen`) between turns to maintain fleet secrecy.
- **LAN Multiplayer ("Play with a Friend"):** Direct peer-to-peer TCP socket connection across local networks:
  - Scannable QR code (via ZXing) or shareable join string (`BATTLESHIP:<ip>:<port>:<code>`).
  - Strict Fog-of-War: Real ship placements are never transmitted over the wire; each client is authoritative only over its own board and reports shot outcomes (`MISS`, `HIT`, `SUNK`).

### Battlefields & Theaters
- **Coastal Waters (5×5):** Fast skirmish with Patrol Boat (2), Destroyer (2), and Submarine (3).
- **Open Sea (8×8):** Tactical battle with Destroyer (2), Submarine (3), Cruiser (3), and Battleship (4).
- **Pacific Theater (10×10):** Full fleet warfare adding the Aircraft Carrier (5).
- **Quick Match (5×5):** Fast skirmish with Patrol Boat (2) and Submarine (3). Total 7 hits to win.
- **Standard (8×8):** Tactical engagement with Destroyer (2), Submarine (2), and Battleship (1). Total 14 hits to win.
- **Classic (10×10):** Full fleet action with Destroyer (2), Submarine (2), Cruiser (1), Battleship (1), and Aircraft Carrier (1). Total 19 hits to win.

### Advanced Weaponry & Launcher System
- **Default Shell:** Precise 1×1 shot with infinite ammo.
- **Level 2 Salvo:** 1×3 line barrage available on larger grids (8×8 and 10×10).
### Advanced Weaponry & Arsenal System
- **Standard Shell:** Precise 1×1 shot with infinite ammo.
- **Salvo Barrage:** 1×3 line barrage available on larger grids (8×8 and 10×10).
- **Tactical Nuclear Warhead:** 2×3 area devastation available across all theaters.
  - **Launch Code Protocol:** Firing requires answering naval trivia questions to authorize detonation.
  - **Auto-Resupply:** Automatically initiates a resupply countdown and drill to restock warheads.
  - **Auto-Resupply:** Initiates a resupply drill to restock warheads after usage.
- **Orientation & Target Preview:** Press **`R`** or **Right-Click** to rotate weapon trajectory with real-time ghost overlay.

### AI Strategies (Strategy & Composition Pattern)
- **Ensign (Easy):** Uniform random targeting across unshot coordinates.
- **Lieutenant (Normal):** Checkerboard parity hunt exploiting minimum ship size (length 2), transitioning to targeted neighbor pursuit upon impact.
- **Admiral (Hard):** Probability density mapping that computes the number of valid remaining ship configurations for every cell, paired with opportunistic area-weapon bombardment and line-following targeting.
- **Admiral (Hard):** Probability density mapping that computes valid remaining ship configurations for every cell, paired with opportunistic area-weapon bombardment and line-following targeting.

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
├── model           # Pure domain entities, aggregate roots, and value objects
│   ├── AiPlayer.java          # Machine participant entity
│   ├── AmmoReadout.java       # Read-only ammo query interface
│   ├── Arsenal.java           # Centralized launcher ammo tracking
│   ├── CellStatus.java        # Cell state enum (EMPTY, SHIP, HIT, MISS, SUNK)
│   ├── Coordinate.java        # Immutable board coordinate (row, col)
│   ├── GameMode.java          # Mode selector (AI, Hotseat, etc.)
│   ├── GameState.java         # Game state machine
│   ├── LauncherType.java      # Polymorphic weapon types with blast patterns
│   ├── Orientation.java       # Ship/launcher direction enum (replaces raw booleans)
│   ├── Player.java            # Participant entity (human or AI)
│   ├── ReadOnlyBoard.java     # Read-only board interface for views (no mutation)
│   ├── FleetDeployment.java   # Fleet placement state mutator interface
│   ├── FleetReadout.java      # Read-only fleet query interface
│   ├── GameMode.java          # Mode selector (AI_EASY, AI_NORMAL, AI_HARD, HOTSEAT, ONLINE)
│   ├── GameState.java         # Game state machine (MAIN_MENU, SHIP_PLACEMENT, PASS_SCREEN, BATTLE, GAME_OVER)
│   ├── HumanPlayer.java       # Human participant entity
│   ├── MatchStatistics.java   # Domain record computing shots, hits, misses, accuracy, and sunk ships
│   ├── Orientation.java       # Ship and weapon direction enum (HORIZONTAL, VERTICAL)
│   ├── Player.java            # Abstract participant base class
│   ├── PrimaryGrid.java       # Own board state (secret fleet placement and damage)
│   ├── Ship.java              # Placed ship with idempotent hit tracking
│   ├── ShipType.java          # Fleet classes (size and traits)
│   ├── ShipType.java          # Fleet classes (size, asset metadata, traits)
│   ├── ShotOrder.java         # Shot command record
│   ├── ShotResult.java        # Outcome record (coordinate, outcome, shipSunk)
│   ├── Theater.java           # Battlefield configuration presets
│   └── Turn.java              # Turn ownership enum (replaces raw int index)
│   ├── ShotTarget.java        # Target coordinate query abstraction
│   ├── Theater.java           # Battlefield configuration presets (SKIRMISH, ENGAGEMENT, FLEET_ACTION)
│   ├── Turn.java              # Turn ownership enum (PLAYER_1, PLAYER_2)
│   │
│   ├── fog/                   # Strict Fog-of-War tracking
│   │   ├── MarkerStatus.java  # Known enemy cell status (UNKNOWN, MISS, HIT, SUNK)
│   │   └── TrackingGrid.java  # Admiral's private observation grid of enemy waters
│   │
│   ├── projection/            # Read-only domain projections
│   │   └── ShipSnapshot.java  # Immutable snapshot of placed ship state
│   │
│   └── weapon/                # Polymorphic weapon hierarchy (OCP / LSP)
│       ├── BlastPattern.java  # Area-of-effect offsets with orientation rotation
│       ├── NuclearWarhead.java# 2×3 blast weapon with authorization & trivia gating
│       ├── SalvoBarrage.java  # 1×3 line barrage weapon
│       ├── StandardShell.java # Standard 1×1 single shell weapon
│       ├── Weapon.java        # Core weapon abstraction (sound, authorization, blast)
│       └── WeaponCatalog.java # Preconfigured weapon instances
│
├── ai              # AI strategy implementations (no UI dependencies)
│   ├── AIFactory.java         # Strategy factory
├── ai              # AI strategy implementations (pure Java domain logic)
│   ├── AIFactory.java         # Strategy factory with null-safe mode resolution
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
│   ├── LauncherFireResult.java# Multi-cell shot result and sunk ship container
│   ├── NetworkFireService.java# Domain mutations of a network shot (ammo, resupply)
│   ├── PlacementService.java  # Fleet placement legality & auto-deployment
│   ├── ShotResolution.java    # Resolved multi-cell shot outcome record
│   └── ShotResolver.java      # Applies launcher blast patterns to the board
│   └── ShotResolver.java      # Applies launcher blast patterns to target grids
│
├── net             # LAN multiplayer networking (TCP sockets)
│   ├── EnemyTracker.java      # Fog-of-war observer of opponent's board
│   ├── NetMessage.java        # Sealed JSON protocol message hierarchy
│   ├── NetMessageCodec.java   # Gson wire codec with type discriminator
│   ├── NetUtil.java           # IP and port discovery utilities
│   ├── NetworkBattleMediator.java # Controller-level logic for network battles
│   ├── NetworkGameSession.java# Encapsulated session context
│   ├── NetUtil.java           # LAN IP and ephemeral port discovery utilities
│   ├── NetworkBattleMediator.java # Controller-level mediation for network battles
│   ├── NetworkGameSession.java# Encapsulated networking context and turn state
│   ├── NetworkSession.java    # Socket listener/sender with async callbacks
│   └── Role.java              # HOST/CLIENT role enum (replaces boolean flag)
│   └── Role.java              # HOST/CLIENT role enum
│
├── persistence     # Save and load game state (JSON via Gson)
│   ├── GameSaveDTO.java       # Serializable transfer object
│   ├── GameSaveMapper.java    # SRP domain-to-DTO and DTO-to-domain mapper
│   └── SaveGameService.java   # File storage service
│
└── view            # JavaFX presentation layer
    ├── MainApp.java           # JavaFX Application entry point
    ├── ViewNavigator.java     # Navigation & audio abstraction (DI seam for views)
    ├── MainMenuView.java      # Title screen and options
    ├── GameModeSelectView.java# Mode selector
    ├── BoardSelectView.java   # Theater selector
    ├── ViewNavigator.java     # Navigation abstraction (DI seam for views)
    ├── AudioProvider.java     # Audio accessor interface
    ├── WindowProvider.java    # Stage accessor interface
    ├── MainMenuView.java      # Title screen with centered navigation
    ├── GameModeSelectView.java# Mode selector with card-wide click handlers
    ├── BoardSelectView.java   # Theater selector with interactive preview cards
    ├── AbstractShipPlaceView.java # Shared deployment logic and UI
    ├── ShipPlaceView.java     # Drag-and-drop & auto fleet deployment (local modes)
    ├── NetworkShipPlaceView.java # Network deployment synchronization
    ├── ShipDockPane.java      # Dock tray for unplaced ships
    ├── BoardGridPane.java     # Interactive grid component
    ├── AbstractBattleView.java# Shared combat command center logic and UI
    ├── LocalBattleView.java   # Main combat view (AI & Hotseat)
    ├── NetworkBattleView.java # Authoritative peer combat view
    ├── GameOverView.java      # Victory/Defeat screen
    ├── GameOverView.java      # Victory/Defeat screen with MatchStatistics
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
    │
    ├── battle/                # Combat view UI components
    │   ├── BattleLog.java     # Reusable combat message log component
    │   └── WeaponConsole.java # Weapon selection and ammo UI bar
    │
    ├── decor/                 # Procedural canvas animation renderers
    │   ├── CompassWatermark.java   # Decorative compass overlay
    │   ├── OceanRibbonRenderer.java# Flowing ribbon wave accents
    │   ├── OceanSceneRenderer.java # Layered ocean waves and sky gradients
    │   └── RadarSweepRenderer.java # Rotating radar sweep animation
    │
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
The test suite (JUnit 5) runs headlessly — **no JavaFX runtime or display required** — comprising **38 tests across 14 test suites**:
- **AI Strategies:** `ParityHunterTest`
- **Combat & Resolution:** `BattleServiceTest`, `ShotResolverTest`
- **Controller & DIP:** `GameControllerDIPTest` (hotseat lifecycle, online mode, DI seams)
- **Domain Models:** `TurnTest`, `MatchStatisticsTest`
- **Weapons & Polymorphism:** `BlastPatternRotationTest`, `WeaponPolymorphismTest`
- **Networking:** `NetworkGameSessionTest`, `NetworkBattleMediatorTest`
- **Persistence:** `GameSaveMapperTest`, `SaveGameServiceTest`, `SaveGameIntegrationTest`
- **Audio Abstractions:** `SilentAudioTest`

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
- **Polymorphism over Conditionals (OCP / LSP):**
  - The `Weapon` hierarchy (`StandardShell`, `SalvoBarrage`, `NuclearWarhead`) encapsulates blast pattern generation, ammo constraints, launch authorization protocol (`requiresAuthorization()`), and audio dispatch (`playFiringSound(...)`), eliminating `instanceof` checks and switch statements.
- **Strict Fog-of-War Encapsulation:**
  - Opponents observe enemy waters exclusively through `TrackingGrid` populated by `MarkerStatus` records. Private fleet positions on `PrimaryGrid` are never exposed or serialized across the network.
- **Single Responsibility Principle (SRP):**
  - `MatchStatistics` encapsulates post-battle metric computation (shots, hits, misses, accuracy, sunk ships).
  - `GameSaveMapper` decouples serialization DTO mapping from controller orchestration.
  - UI components like `BattleLog` and `WeaponConsole` manage their own rendering and layout.
- **Information Expert:**
  - `ShipType` encapsulates its own UI asset names (`getAssetName()`), eliminating switch statements in image resource caches.
- **Composition over Inheritance:**
  - `SmartAI` and `HuntTargetAI` compose independent `TargetingQueue` and `ParityHunter` components rather than sharing brittle inheritance trees.
- **Strict Encapsulation & Immutability:**
  - `Ship` hit tracking uses an internal `Set<Coordinate>` for idempotent hit registration, preventing duplicate damage counts.
  - `Coordinate` is an immutable record.
  - Collections returned by domain queries are unmodifiable.
- **Interface Segregation & Dependency Inversion:**
  - Views interact with domain models via narrow read-only interfaces (`FleetReadout`, `AmmoReadout`).
  - Audio is accessed through `GameAudio`, composed of distinct `SfxAudio`, `MusicAudio`, and `AudioSettings` roles.
  - `SilentAudio` serves as a headless Null Object implementation for unit tests.
- **No Primitive Obsession:**
  - Self-documenting enums and value types replace raw primitives: `Orientation` (direction), `Turn` (turn ownership), `Role` (HOST/CLIENT), `CellStatus`, and `MarkerStatus`.
- **Domain Independence:**
  - All domain and AI logic executes independently of JavaFX or any graphical library, enabling fast, headless unit and integration testing.
