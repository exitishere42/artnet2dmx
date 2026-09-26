package de.artnet2dmx;

import de.artnet2dmx.ui.MainWindow;
import de.artnet2dmx.ui.MaterialTheme;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * JavaFX Hauptanwendung für artnet2dmx.
 */
public class ArtNet2DmxApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ArtNet2DmxApp.class.getName());
    private static HostServices hostServices;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] [%4$-7s] %5$s%n");
        try {
            Logger rootLogger = Logger.getLogger("");
            FileHandler fileHandler = new FileHandler("app.log", 5 * 1024 * 1024, 2, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            rootLogger.addHandler(fileHandler);
        } catch (Exception e) {
            System.err.println("Konnte FileHandler für app.log nicht initialisieren: " + e.getMessage());
        }
    }

    private MainWindow mainWindow;
    private Timeline tickTimeline;

    @Override
    public void start(Stage stage) {
        hostServices = getHostServices();
        LOGGER.info("==================================================================");
        LOGGER.info("Starte artnet2dmx (JavaFX 21 LTS)");
        LOGGER.info("Log-Datei: " + new File("app.log").getAbsolutePath());
        LOGGER.info("==================================================================");

        mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow, 880, 720);
        scene.setFill(MaterialTheme.COLOR_BG);

        // Dark Theme CSS laden
        try {
            String css = getClass().getResource("/styles/material-dark.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            LOGGER.warning("Konnte material-dark.css nicht laden: " + e.getMessage());
        }

        // App Icon
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/artnet2dmx.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }

        stage.setTitle("artnet2dmx");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(640);

        // 25 Hz UI-Aktualisierung (alle 40 ms)
        tickTimeline = new Timeline(new KeyFrame(Duration.millis(40), e -> mainWindow.tick()));
        tickTimeline.setCycleCount(Timeline.INDEFINITE);
        tickTimeline.play();

        stage.setOnCloseRequest(e -> {
            LOGGER.info("Beende artnet2dmx...");
            if (tickTimeline != null) {
                tickTimeline.stop();
            }
            if (mainWindow != null) {
                mainWindow.stopService();
            }
        });

        stage.show();
        LOGGER.info(String.format("Hauptfenster geöffnet: Scene=%.1fx%.1f | Stage=%.1fx%.1f",
                scene.getWidth(), scene.getHeight(), stage.getWidth(), stage.getHeight()));

        Parameters params = getParameters();
        boolean autoStart = params != null && (params.getRaw().contains("--start") || params.getRaw().contains("--autostart"));
        if (autoStart || mainWindow.getConfig().isAutostart()) {
            LOGGER.info("Autostart aktiv - starte DMX-Bridge...");
            mainWindow.toggleService();
        }

        stage.widthProperty().addListener((obs, oldW, newW) -> {
            LOGGER.fine(String.format("Fensterbreite geändert: %.1f -> %.1f", oldW.doubleValue(), newW.doubleValue()));
        });
        stage.heightProperty().addListener((obs, oldH, newH) -> {
            LOGGER.fine(String.format("Fensterhöhe geändert: %.1f -> %.1f", oldH.doubleValue(), newH.doubleValue()));
        });
    }

    public static HostServices getAppHostServices() {
        return hostServices;
    }

    /**
     * Öffnet eine URL sicher im Standardbrowser des Nutzers.
     * Verwendet primär JavaFX HostServices und fällt bei Bedarf auf native OS-Kommandos zurück.
     * Verwendet bewusst KEIN java.awt.Desktop, um AWT/GTK-Konflikte und Abstürze auf Linux zu vermeiden.
     */
    public static void openWebpage(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        Thread openerThread = new Thread(() -> {
            boolean success = false;

            // 1. JavaFX HostServices versuchen
            if (hostServices != null) {
                try {
                    hostServices.showDocument(url);
                    success = true;
                } catch (Throwable t) {
                    LOGGER.warning("HostServices.showDocument fehlgeschlagen: " + t.getMessage());
                }
            }

            // 2. Nativer OS-Aufruf als Fallback (ohne AWT)
            if (!success) {
                String os = System.getProperty("os.name", "").toLowerCase();
                try {
                    if (os.contains("win")) {
                        new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", url).start();
                        success = true;
                    } else if (os.contains("mac")) {
                        new ProcessBuilder("open", url).start();
                        success = true;
                    } else {
                        // Linux / BSD: xdg-open oder gio
                        String[] commands = {"xdg-open", "gio", "sensible-browser", "x-www-browser"};
                        for (String cmd : commands) {
                            try {
                                Process p = "gio".equals(cmd)
                                        ? new ProcessBuilder("gio", "open", url).start()
                                        : new ProcessBuilder(cmd, url).start();
                                if (p.isAlive()) {
                                    success = true;
                                    break;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.warning("Natives Öffnen des Browsers fehlgeschlagen: " + t.getMessage());
                }
            }
        }, "BrowserOpenerThread");

        openerThread.setDaemon(true);
        openerThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
