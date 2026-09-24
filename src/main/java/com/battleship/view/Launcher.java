package com.battleship.view;

/**
 * Entry-point shim that makes the shaded fat JAR runnable via a plain
 * {@code java -jar target/naval-command-1.0.0.jar} command.
 *
 * <p>The JDK launcher refuses to start a main class that extends
 * {@link javafx.application.Application} unless the JavaFX runtime is present
 * as <em>modules</em> (on the module path). In a shaded fat JAR the JavaFX
 * classes live on the classpath (unnamed module), so pointing {@code Main-Class}
 * at {@link MainApp} aborts with
 * "Error: JavaFX runtime components are missing, and are required to run this
 * application" — before {@code main} is ever invoked. Because this class does
 * <em>not</em> extend {@code Application}, the launcher skips that module
 * check, and {@link MainApp}'s internal {@code launch()} call starts the
 * JavaFX toolkit from the classpath instead.</p>
 */
public final class Launcher {

    private Launcher() {
        // Entry-point shim only — never instantiated.
    }

    /**
     * Delegates straight to {@link MainApp#main(String[])}, so the packaged
     * JAR behaves exactly like the {@code mvn javafx:run} launch.
     *
     * @param args command-line arguments, forwarded to the application
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
