# Battleship: Naval Command

Java 17 + JavaFX implementation, built with Maven, following a Hexagonal
(Ports & Adapters) architecture.

## Package layout

```
com.battleship
├── model         # Pure Java domain entities — ZERO JavaFX imports
│   ├── Coordinate.java
│   ├── CellStatus.java
│   ├── ShipType.java
│   ├── Ship.java
│   ├── ShotResult.java
│   ├── Theater.java       # 5x5 / 8x8 / 10x10 board configs
│   ├── Board.java         # aggregate root
│   ├── Player.java
│   └── GameState.java
│
├── ai             # Pure Java domain services — ZERO JavaFX imports
│   ├── AIStrategy.java     # Strategy pattern interface
│   ├── Difficulty.java
│   ├── AIFactory.java
│   ├── RandomAI.java       # Ensign / Easy
│   ├── HuntTargetAI.java   # Lieutenant / Normal
│   └── SmartAI.java        # Admiral / Hard
│
├── controller     # Application services — orchestrates game flow
│   └── GameController.java
│
├── persistence    # JSON save/load (Gson)
│   ├── SaveGameService.java
│   └── GameSaveDTO.java
│
└── view           # JavaFX adapters — ALL JavaFX imports live here
    ├── MainApp.java         # entry point
    ├── MainMenuView.java
    ├── BoardSelectView.java
    ├── ShipPlaceView.java
    ├── BattleView.java
    ├── GameOverView.java
    ├── PassScreen.java      # hotseat handoff overlay
    ├── BoardGridPane.java   # reusable grid component
    └── ShipDockPane.java    # drag-and-drop ship tray
```

## Layer rule (non-negotiable)

`model` and `ai` must never import `javafx.*`. All rendering, animation,
and input handling belongs in `view`. `controller` is the only layer
allowed to talk to both sides.

## Build & run

```bash
mvn clean compile
mvn javafx:run
```

## Package a runnable JAR

```bash
mvn clean package
java -jar target/naval-command-1.0.0.jar
```

## Status

Skeleton only — every class above has method signatures and Javadoc from
the spec, with `// TODO` markers and `throw new UnsupportedOperationException("TODO")`
in method bodies. Next step: implement `model` layer first (it has zero
dependencies on anything else), then `ai`, then `controller`, then `view`.
