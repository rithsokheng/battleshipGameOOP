package com.battleship.view;

import javafx.animation.AnimationTimer;
import javafx.animation.RotateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Small shared helpers for purely decorative, non-interactive background art
 * used across several menu-style screens.
 */
final class DecorUtil {

    private DecorUtil() { }

    /**
     * A slow, continuously spinning, semi-transparent compass rose meant to sit
     * behind other content (add it as the FIRST child of a StackPane). Returns
     * null if the art asset couldn't be loaded, so callers can skip it cleanly.
     */
    static ImageView compassWatermark(double size) {
        Image compass = ImageResources.ui("compass-rose");
        if (compass == null) return null;

        ImageView iv = new ImageView(compass);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setOpacity(0.09);
        iv.setMouseTransparent(true);

        RotateTransition spin = new RotateTransition(Duration.seconds(120), iv);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.play();

        return iv;
    }

    /**
     * A small "Fleet Command"-style radar sweep: concentric rings, crosshairs,
     * a rotating cyan sweep wedge, and a handful of gold blips that flare as
     * the sweep passes over them. Non-interactive; sized to a square of the
     * given pixel size. Meant to sit inside a side-panel card.
     */
    static StackPane animatedRadarSweep(double size) {
        Canvas canvas = new Canvas(size, size);
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = size / 2.0, cy = size / 2.0, r = size / 2.0 - 4;

        List<double[]> blips = new ArrayList<>();
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < 6; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = rnd.nextDouble() * 0.4 + 0.15;
            blips.add(new double[]{a, d});
        }

        final double[] angle = {0};
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                angle[0] += 0.02;
                gc.clearRect(0, 0, size, size);

                gc.setStroke(Color.rgb(46, 93, 135, 0.55));
                gc.setLineWidth(1);
                for (int i = 1; i <= 3; i++) {
                    double rr = r * i / 3.0;
                    gc.strokeOval(cx - rr, cy - rr, rr * 2, rr * 2);
                }
                gc.strokeLine(cx, cy - r, cx, cy + r);
                gc.strokeLine(cx - r, cy, cx + r, cy);

                gc.save();
                gc.beginPath();
                gc.moveTo(cx, cy);
                gc.arc(cx, cy, r, r, Math.toDegrees(-angle[0]), 30);
                gc.closePath();
                gc.setFill(Color.rgb(68, 184, 255, 0.18));
                gc.fill();
                gc.restore();

                for (double[] b : blips) {
                    double bx = cx + Math.cos(b[0]) * b[1] * r;
                    double by = cy + Math.sin(b[0]) * b[1] * r;
                    double diff = Math.abs(((angle[0] - b[0] + Math.PI * 4) % (Math.PI * 2)));
                    double alpha = diff < 0.6 ? 1.0 : 0.15;
                    gc.setFill(Color.rgb(255, 209, 102, alpha));
                    gc.fillOval(bx - 3, by - 3, 6, 6);
                }
            }
        };
        timer.start();

        StackPane wrap = new StackPane(canvas);
        wrap.setMouseTransparent(true);
        wrap.setPrefSize(size, size);
        wrap.setMaxSize(size, size);
        return wrap;
    }

    /**
     * A wide, very faint ribbon of drifting sine-wave "ocean" lines meant to sit
     * behind menu content (add as the FIRST child of a StackPane), echoing the
     * animated ocean atmosphere in the web reference. Purely decorative and
     * mouse-transparent.
     */
    static Canvas animatedOceanRibbon(double width, double height) {
        Canvas canvas = new Canvas(width, height);
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Color[] shades = {
                Color.rgb(68, 184, 255, 0.10),
                Color.rgb(68, 184, 255, 0.06),
                Color.rgb(255, 209, 102, 0.05)
        };

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt * 0.35;

                gc.clearRect(0, 0, width, height);
                double midY = height / 2.0;
                for (int line = 0; line < shades.length; line++) {
                    gc.setStroke(shades[line]);
                    gc.setLineWidth(2);
                    gc.beginPath();
                    double amp = 10 + line * 6;
                    double freq = 0.015 - line * 0.002;
                    double phase = t[0] * (1.2 + line * 0.4) + line * 1.7;
                    double yOffset = line * 14 - 14;
                    for (double x = 0; x <= width; x += 4) {
                        double y = midY + yOffset + Math.sin(x * freq + phase) * amp;
                        if (x == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                    }
                    gc.stroke();
                }
            }
        };
        timer.start();

        return canvas;
    }

    /**
     * A full painted night-ocean backdrop: gradient sky, a glowing moon,
     * scattered stars, a horizon line, and several layers of filled,
     * parallaxing swells with moonlit shimmer on the water. Meant to fill
     * the whole background of a screen — the returned canvas's width/height
     * are bound to {@code sizeSource} so it always covers the full window,
     * including on resize/maximize (add as the FIRST child of a StackPane
     * that is itself sized to fill the scene). Purely decorative,
     * mouse-transparent, and drawn entirely with vector shapes/gradients —
     * no external art needed.
     */
    static Canvas animatedOceanScene(Region sizeSource) {
        return animatedOceanScene(sizeSource, 0.40);
    }

    /**
     * Same night-ocean backdrop as {@link #animatedOceanScene(Region)}, but with
     * a configurable horizon line (fraction of height taken up by sky). Screens
     * that want to feel like they're sitting low on the water — e.g. the battle
     * screen — pass a small fraction so the sea fills most of the view.
     */
    static Canvas animatedOceanScene(Region sizeSource, double horizonFrac) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Star field stored as fractional coordinates (0..1) so it rescales
        // cleanly with the canvas instead of being tied to one fixed size.
        java.util.Random starRng = new java.util.Random(42);
        double[][] starsFrac = new double[46][3];
        for (int i = 0; i < starsFrac.length; i++) {
            starsFrac[i][0] = starRng.nextDouble();
            starsFrac[i][1] = starRng.nextDouble() * 0.92;
            starsFrac[i][2] = starRng.nextDouble();
        }

        // Swell layers, back to front: [speed, amplitude, wavelength, y-position(0..1 of sea band), base opacity, warm-tint]
        double[][] layers = {
                {0.12, 8, 220, 0.10, 0.55, 0},
                {0.20, 11, 170, 0.30, 0.65, 0},
                {0.30, 15, 140, 0.52, 0.78, 0.15},
                {0.42, 19, 110, 0.76, 0.92, 0.25},
        };
        Color deepSea = Color.rgb(8, 26, 44);
        Color midSea = Color.rgb(13, 42, 66);

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt;

                double width = canvas.getWidth();
                double height = canvas.getHeight();
                if (width <= 0 || height <= 0) return;

                double horizonY = height * horizonFrac;
                boolean showSky = horizonY > 4;
                double moonX = width * 0.78;
                double moonY = horizonY * 0.38;
                double moonR = Math.max(22, Math.min(46, width * 0.036));

                gc.clearRect(0, 0, width, height);

                if (showSky) {
                    // Sky.
                    LinearGradient sky = new LinearGradient(0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(3, 9, 20)),
                            new Stop(0.6, Color.rgb(7, 20, 38)),
                            new Stop(1, Color.rgb(13, 34, 55)));
                    gc.setFill(sky);
                    gc.fillRect(0, 0, width, horizonY);

                    // Stars (gentle twinkle).
                    for (double[] s : starsFrac) {
                        double tw = 0.35 + 0.45 * (0.5 + 0.5 * Math.sin(t[0] * 1.4 + s[2] * 20));
                        gc.setFill(Color.rgb(230, 240, 255, tw * 0.7));
                        double r = 0.6 + s[2] * 1.1;
                        gc.fillOval(s[0] * width, s[1] * horizonY, r, r);
                    }

                    // Moon.
                    RadialGradient moonGlow = new RadialGradient(0, 0, moonX, moonY, moonR * 5.2,
                            false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(255, 244, 214, 0.35)),
                            new Stop(0.35, Color.rgb(255, 232, 180, 0.10)),
                            new Stop(1, Color.rgb(255, 232, 180, 0.0)));
                    RadialGradient moonBody = new RadialGradient(0, 0, moonX - moonR * 0.3, moonY - moonR * 0.3, moonR * 1.6,
                            false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(255, 250, 235, 0.95)),
                            new Stop(1, Color.rgb(255, 224, 168, 0.85)));
                    gc.setFill(moonGlow);
                    gc.fillOval(moonX - moonR * 5.2, moonY - moonR * 5.2, moonR * 10.4, moonR * 10.4);
                    gc.setFill(moonBody);
                    gc.fillOval(moonX - moonR, moonY - moonR, moonR * 2, moonR * 2);
                }

                // Base sea fill — spans the whole canvas when there's no sky band.
                LinearGradient sea = new LinearGradient(0, horizonY, 0, height, false, CycleMethod.NO_CYCLE,
                        new Stop(0, midSea),
                        new Stop(1, deepSea));
                gc.setFill(sea);
                gc.fillRect(0, horizonY, width, height - horizonY);

                if (showSky) {
                    // Soft horizon glow where the moon meets the water.
                    gc.setFill(Color.rgb(255, 232, 190, 0.10));
                    gc.fillRect(0, horizonY, width, 3);

                    // Moonlit shimmer column on the water — thin broken highlights.
                    for (int i = 0; i < 26; i++) {
                        double frac = i / 26.0;
                        double y = horizonY + frac * (height - horizonY);
                        double spread = 10 + frac * 46;
                        double wobble = Math.sin(t[0] * 2.2 + i * 1.3) * spread * 0.5;
                        double segW = 10 + frac * 26;
                        double alpha = (1 - frac) * 0.22;
                        gc.setFill(Color.rgb(255, 240, 205, alpha));
                        gc.fillRoundRect(moonX + wobble - segW / 2.0, y, segW, 1.6 + frac * 1.4, 4, 4);
                    }
                }

                // Layered filled swells, drawn back (dim) to front (brighter, warmer near shore).
                for (double[] layer : layers) {
                    double speed = layer[0], amp = layer[1], wavelen = layer[2];
                    double yFrac = layer[3], baseAlpha = layer[4], warm = layer[5];
                    double baseY = horizonY + (height - horizonY) * yFrac;
                    double phase = t[0] * speed * 6.0;

                    Color base = deepSea.interpolate(Color.rgb(68, 184, 255), 0.10 + yFrac * 0.10)
                            .interpolate(Color.rgb(255, 209, 102), warm * 0.12);
                    Color fillColor = Color.color(base.getRed(), base.getGreen(), base.getBlue(), baseAlpha);
                    Color crestColor = base.brighter().deriveColor(0, 1, 1, Math.min(1, baseAlpha + 0.15));

                    gc.beginPath();
                    gc.moveTo(0, height + 4);
                    gc.lineTo(0, baseY);
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        gc.lineTo(x, y);
                    }
                    gc.lineTo(width, height + 4);
                    gc.closePath();
                    gc.setFill(fillColor);
                    gc.fill();

                    // A thin brighter crest line riding the top of this swell.
                    gc.setStroke(crestColor);
                    gc.setLineWidth(1.2);
                    gc.beginPath();
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        if (x == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                    }
                    gc.stroke();
                }

                // Subtle vignette so foreground UI text stays readable.
                gc.setFill(Color.rgb(2, 6, 12, 0.28));
                gc.fillRect(0, 0, width, height * 0.16);
            }
        };
        timer.start();

        return canvas;
    }

    /**
     * A brighter, daytime "light sea" backdrop — no moon/stars, a soft sun
     * glow instead, and lighter sky/water tones than {@link #animatedOceanScene}.
     * Same sizing contract: bind to a Region and add as the first StackPane child.
     */
    static Canvas lightSeaScene(Region sizeSource) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double[][] layers = {
                {0.12, 8, 220, 0.10, 0.45, 0},
                {0.20, 11, 170, 0.32, 0.55, 0.05},
                {0.30, 15, 140, 0.56, 0.68, 0.10},
                {0.42, 19, 110, 0.80, 0.82, 0.16},
        };
        Color deepSea = Color.rgb(18, 66, 104);
        Color midSea = Color.rgb(30, 96, 140);

        final double[] t = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                t[0] += dt;

                double width = canvas.getWidth();
                double height = canvas.getHeight();
                if (width <= 0 || height <= 0) return;

                double horizonY = height * 0.38;
                double sunX = width * 0.80;
                double sunY = horizonY * 0.34;
                double sunR = Math.max(20, Math.min(40, width * 0.03));

                gc.clearRect(0, 0, width, height);

                LinearGradient sky = new LinearGradient(0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(22, 58, 92)),
                        new Stop(0.6, Color.rgb(34, 82, 122)),
                        new Stop(1, Color.rgb(58, 118, 160)));
                gc.setFill(sky);
                gc.fillRect(0, 0, width, horizonY);

                // Sun glow.
                RadialGradient sunGlow = new RadialGradient(0, 0, sunX, sunY, sunR * 5.0,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 244, 214, 0.4)),
                        new Stop(0.35, Color.rgb(255, 226, 170, 0.14)),
                        new Stop(1, Color.rgb(255, 226, 170, 0.0)));
                RadialGradient sunBody = new RadialGradient(0, 0, sunX, sunY, sunR * 1.4,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 250, 235, 0.95)),
                        new Stop(1, Color.rgb(255, 214, 140, 0.85)));
                gc.setFill(sunGlow);
                gc.fillOval(sunX - sunR * 5.0, sunY - sunR * 5.0, sunR * 10.0, sunR * 10.0);
                gc.setFill(sunBody);
                gc.fillOval(sunX - sunR, sunY - sunR, sunR * 2, sunR * 2);

                // A few soft daylight clouds drifting slowly.
                gc.setFill(Color.rgb(255, 255, 255, 0.10));
                for (int i = 0; i < 4; i++) {
                    double cx = ((i * 260 + t[0] * 6) % (width + 200)) - 100;
                    double cy = horizonY * (0.18 + i * 0.16);
                    gc.fillOval(cx, cy, 90, 22);
                    gc.fillOval(cx + 30, cy - 8, 70, 20);
                }

                // Base sea fill beneath the horizon.
                LinearGradient sea = new LinearGradient(0, horizonY, 0, height, false, CycleMethod.NO_CYCLE,
                        new Stop(0, midSea),
                        new Stop(1, deepSea));
                gc.setFill(sea);
                gc.fillRect(0, horizonY, width, height - horizonY);

                // Soft horizon glow line.
                gc.setFill(Color.rgb(255, 240, 210, 0.16));
                gc.fillRect(0, horizonY, width, 3);

                // Sunlit shimmer column on the water.
                for (int i = 0; i < 26; i++) {
                    double frac = i / 26.0;
                    double y = horizonY + frac * (height - horizonY);
                    double spread = 10 + frac * 46;
                    double wobble = Math.sin(t[0] * 2.2 + i * 1.3) * spread * 0.5;
                    double segW = 10 + frac * 26;
                    double alpha = (1 - frac) * 0.20;
                    gc.setFill(Color.rgb(255, 240, 205, alpha));
                    gc.fillRoundRect(sunX + wobble - segW / 2.0, y, segW, 1.6 + frac * 1.4, 4, 4);
                }

                // Layered filled swells, brighter cyan tones for the daylight feel.
                for (double[] layer : layers) {
                    double speed = layer[0], amp = layer[1], wavelen = layer[2];
                    double yFrac = layer[3], baseAlpha = layer[4], warm = layer[5];
                    double baseY = horizonY + (height - horizonY) * yFrac;
                    double phase = t[0] * speed * 6.0;

                    Color base = deepSea.interpolate(Color.rgb(120, 210, 255), 0.16 + yFrac * 0.14)
                            .interpolate(Color.rgb(255, 220, 160), warm * 0.10);
                    Color fillColor = Color.color(base.getRed(), base.getGreen(), base.getBlue(), baseAlpha);
                    Color crestColor = base.brighter().deriveColor(0, 1, 1, Math.min(1, baseAlpha + 0.15));

                    gc.beginPath();
                    gc.moveTo(0, height + 4);
                    gc.lineTo(0, baseY);
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        gc.lineTo(x, y);
                    }
                    gc.lineTo(width, height + 4);
                    gc.closePath();
                    gc.setFill(fillColor);
                    gc.fill();

                    gc.setStroke(crestColor);
                    gc.setLineWidth(1.2);
                    gc.beginPath();
                    for (double x = 0; x <= width; x += 6) {
                        double y = baseY + Math.sin((x / wavelen) * 2 * Math.PI + phase) * amp
                                + Math.sin((x / (wavelen * 2.7)) + phase * 1.6) * amp * 0.3;
                        if (x == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                    }
                    gc.stroke();
                }
            }
        };
        timer.start();

        return canvas;
    }
}
