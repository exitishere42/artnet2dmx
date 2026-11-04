package de.artnet2dmx.ui;

import de.artnet2dmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LucideIconTest {

    @BeforeAll
    static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRenderAllIcons() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                String[] icons = {
                    "play", "square", "activity", "trending-up", "refresh-cw", "sliders",
                    "circle", "circle-dot", "circle-help", "help-circle", "check-circle", "check",
                    "arrow-up-circle", "upload-cloud", "download", "alert-triangle", "info",
                    "external-link", "x", "settings", "gear", "globe",
                    "music", "mic", "plus", "trash-2", "edit-3", "zap", "layers",
                    "maximize", "minimize", "folder-open", "file-text", "bookmark",
                    "save", "piano", "keyboard", "radio", "rotate-ccw", "power", "unknown-fallback"
                };

                for (String iconName : icons) {
                    LucideIcon icon = new LucideIcon(iconName, 18, Color.CYAN);
                    icon.draw();
                }
                success.set(true);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(success.get(), "All Lucide icons must render without exceptions");
    }
}
