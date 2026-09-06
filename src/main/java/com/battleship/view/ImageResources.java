package com.battleship.view;

import com.battleship.model.LauncherType;
import com.battleship.model.ShipType;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Central place to load and cache the game's bundled art (ship hulls, weapon
 * icons, hit/miss effects, menu decorations). Every lookup is classpath-based
 * ("/images/..."), cached after first use, and returns {@code null} instead of
 * throwing if an asset is missing so callers can gracefully fall back to the
 * plain color rendering that predates this class.
 */
public final class ImageResources {

    private ImageResources() { }

    private static final ConcurrentMap<String, Image> CACHE = new ConcurrentHashMap<>();

    private static Image load(String classpathPath) {
        return CACHE.computeIfAbsent(classpathPath, path -> {
            try (InputStream in = ImageResources.class.getResourceAsStream(path)) {
                return in == null ? null : new Image(in);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Hull art for a ship type/orientation. PATROL_BOAT has no bespoke art
     * (it's a 2-length ship, like DESTROYER) so it borrows the destroyer sprite.
     */
    public static Image ship(ShipType type, boolean horizontal) {
        String name = switch (type) {
            case PATROL_BOAT, DESTROYER -> "destroyer";
            case SUBMARINE -> "submarine";
            case CRUISER -> "cruiser";
            case BATTLESHIP -> "battleship";
            case CARRIER -> "carrier";
        };
        return load("/images/ships/" + name + "-" + (horizontal ? "h" : "v") + ".png");
    }

    public static Image launcherIcon(LauncherType type) {
        String name = switch (type) {
            case DEFAULT -> "launcher-default";
            case LEVEL_2 -> "launcher-level2";
            case NUCLEAR -> "launcher-nuclear";
        };
        return load("/images/ui/" + name + ".png");
    }

    /** Hit/miss/fire effect art. Pass just the base name, e.g. "hit-explosion", "miss-splash", "fire-1". */
    public static Image effect(String baseName) {
        return load("/images/effects/" + baseName + ".png");
    }

    /** Menu/chrome art. Pass just the base name, e.g. "logo-battleship", "wave-line", "compass-rose", "icon-anchor". */
    public static Image ui(String baseName) {
        return load("/images/ui/" + baseName + ".png");
    }
}
