package de.artnet2dmx.ui;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SplitPanePillTest {

    @BeforeAll
    static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already initialized
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSplitPaneDividerHasGrabberWithStyle() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean grabberFound = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                SplitPane splitPane = new SplitPane();
                splitPane.setOrientation(Orientation.VERTICAL);
                splitPane.getItems().addAll(new Region(), new Region());

                Scene scene = new Scene(splitPane, 400, 400);
                scene.getStylesheets().add(getClass().getResource("/styles/material-dark.css").toExternalForm());

                splitPane.applyCss();
                splitPane.layout();

                for (Node divider : splitPane.lookupAll(".split-pane-divider")) {
                    if (divider instanceof Parent p) {
                        for (Node child : p.getChildrenUnmodifiable()) {
                            if (child.getStyleClass().contains("vertical-grabber")) {
                                grabberFound.set(true);
                                Region r = (Region) child;
                                System.out.println("Grabber prefSize: " + r.getPrefWidth() + "x" + r.getPrefHeight() + ", bg=" + r.getBackground());
                            }
                        }
                    }
                }
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(grabberFound.get(), "vertical-grabber must be present inside SplitPaneDivider");
    }
}
