package de.artnet2dmx.ui.component;

import de.artnet2dmx.ui.MaterialTheme;
import de.artnet2dmx.ui.icon.LucideIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * 512-Kanal DMX-Visualizer mit exakt 32 sichtbaren Kanälen im Viewport,
 * horizontaler Scrollbar und Schnell-Sprungtasten.
 * 
 * Verwendet ein entkoppeltes CanvasPane (unmanaged Canvas) um Layout-Schleifen
 * und unkontrolliertes Breitenwachstum ("Slider werden immer breiter") vollständig auszuschließen.
 */
public class ChannelVisualizer extends VBox {
    private static final Logger LOGGER = Logger.getLogger(ChannelVisualizer.class.getName());

    public static final int TOTAL_CHANNELS = 512;
    public static final int VISIBLE_COUNT = 32;

    private final Canvas canvas;
    private final CanvasPane canvasPane;
    private final ScrollBar scrollBar;
    private final int[] prevChannels = new int[TOTAL_CHANNELS];

    private Label titleLabel;
    private Label lblBereich;

    private double channelWidth = 25.0;
    private double barWidth = 19.0;
    private double scrollOffsetChannels = 0.0;
    private long renderCount = 0;

    public ChannelVisualizer() {
        LOGGER.info("ChannelVisualizer initialisiert: " + TOTAL_CHANNELS + " Kanäle, " + VISIBLE_COUNT + " sichtbar im Viewport.");

        setPadding(new Insets(10, 14, 10, 14));
        setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_1DP + 
                 "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                 "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        Arrays.fill(prevChannels, -1);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));

        LucideIcon iconSliders = new LucideIcon("sliders", 14, MaterialTheme.COLOR_TEXT_MED);
        titleLabel = new Label(de.artnet2dmx.util.I18n.get("vis.title"));
        titleLabel.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox jumpBox = new HBox(4);
        jumpBox.setAlignment(Pos.CENTER_RIGHT);

        lblBereich = new Label(de.artnet2dmx.util.I18n.get("vis.range"));
        lblBereich.setTextFill(MaterialTheme.COLOR_TEXT_DISABLED);
        lblBereich.setFont(Font.font("Segoe UI", 10));
        jumpBox.getChildren().add(lblBereich);

        int[][] jumps = {
            {1, 32}, {33, 64}, {65, 96}, {97, 128}, {129, 256}, {257, 512}
        };

        for (int[] j : jumps) {
            String labelText = j[0] + "-" + j[1];
            Button btnJump = new Button(labelText);
            btnJump.setCursor(Cursor.HAND);
            btnJump.setFont(Font.font("Segoe UI", 10));
            btnJump.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                             "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + 
                             "; -fx-background-radius: 3px; -fx-padding: 2px 6px;");
            btnJump.setOnMouseEntered(e -> btnJump.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + "; -fx-text-fill: #FFFFFF; -fx-background-radius: 3px; -fx-padding: 2px 6px;"));
            btnJump.setOnMouseExited(e -> btnJump.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + "; -fx-background-radius: 3px; -fx-padding: 2px 6px;"));
            btnJump.setOnAction(e -> scrollToChannel(j[0]));
            jumpBox.getChildren().add(btnJump);
        }

        header.getChildren().addAll(iconSliders, titleLabel, spacer, jumpBox);

        // Canvas & Entkoppelter Container
        canvas = new Canvas(800, 160);
        canvasPane = new CanvasPane(canvas, this);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        // ScrollBar
        scrollBar = new ScrollBar();
        scrollBar.setMin(0);
        scrollBar.setMax(TOTAL_CHANNELS - VISIBLE_COUNT);
        scrollBar.setVisibleAmount(VISIBLE_COUNT);
        scrollBar.setBlockIncrement(VISIBLE_COUNT);
        scrollBar.setUnitIncrement(1);
        scrollBar.setPadding(new Insets(4, 0, 0, 0));

        scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            scrollOffsetChannels = newVal.doubleValue();
            render();
        });

        // Mausrad-Scrolling (horizontal)
        canvasPane.setOnScroll(e -> {
            double delta = e.getDeltaY() != 0 ? e.getDeltaY() : e.getDeltaX();
            if (delta != 0) {
                double step = (delta > 0 ? -3 : 3);
                double target = Math.max(0, Math.min(scrollBar.getMax(), scrollBar.getValue() + step));
                scrollBar.setValue(target);
            }
        });

        getChildren().addAll(header, canvasPane, scrollBar);
    }

    void handleResize(double w, double h) {
        channelWidth = w / (double) VISIBLE_COUNT;
        barWidth = Math.max(4.0, channelWidth - 6.0);
        LOGGER.info(String.format("ChannelVisualizer Canvas resized: %.1fx%.1f | channelWidth: %.2f | barWidth: %.2f",
                w, h, channelWidth, barWidth));
        render();
    }

    public double getVisualizerWidth() {
        return canvas != null ? canvas.getWidth() : 0.0;
    }

    public double getVisualizerHeight() {
        return canvas != null ? canvas.getHeight() : 0.0;
    }

    public void scrollToChannel(int channelNum) {
        double target = Math.max(0, Math.min(TOTAL_CHANNELS - VISIBLE_COUNT, channelNum - 1));
        scrollBar.setValue(target);
    }

    public void updateChannels(int[] channels) {
        boolean changed = false;
        for (int i = 0; i < TOTAL_CHANNELS && i < channels.length; i++) {
            if (channels[i] != prevChannels[i]) {
                prevChannels[i] = channels[i];
                changed = true;
            }
        }
        if (changed) {
            render();
        }
    }

    public void reset() {
        Arrays.fill(prevChannels, 0);
        render();
    }

    private void render() {
        renderCount++;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.setFill(MaterialTheme.COLOR_SURFACE_1DP);
        gc.fillRect(0, 0, w, h);

        double trackTop = 22;
        double trackBottom = h - 22;
        double trackH = trackBottom - trackTop;

        int startCh = (int) Math.floor(scrollOffsetChannels);
        int endCh = Math.min(TOTAL_CHANNELS, startCh + VISIBLE_COUNT + 1);

        gc.setTextAlign(TextAlignment.CENTER);

        for (int ch = startCh; ch < endCh; ch++) {
            double screenX = (ch - scrollOffsetChannels) * channelWidth;
            if (screenX + channelWidth < 0 || screenX > w) {
                continue;
            }

            int val = (ch < TOTAL_CHANNELS && prevChannels[ch] > 0) ? prevChannels[ch] : 0;
            double colCenterX = screenX + (channelWidth / 2.0);
            double barX = screenX + (channelWidth - barWidth) / 2.0;

            // 1. Wert-Text oben (0..255)
            gc.setFont(Font.font("Consolas", 9));
            if (val > 0) {
                gc.setFill(MaterialTheme.COLOR_PRIMARY);
            } else {
                gc.setFill(MaterialTheme.COLOR_TEXT_MED);
            }
            gc.fillText(String.valueOf(val), colCenterX, 14);

            // 2. Track Background
            gc.setFill(MaterialTheme.COLOR_SURFACE_2DP);
            gc.fillRect(barX, trackTop, barWidth, trackH);

            // 3. Pegelbalken
            if (val > 0 && trackH > 0) {
                double barH = (val / 255.0) * trackH;
                gc.setFill(MaterialTheme.COLOR_PRIMARY);
                gc.fillRect(barX, trackBottom - barH, barWidth, barH);
            }

            // 4. Kanal-Nummer unten (1..512)
            gc.setFont(Font.font("Segoe UI", 8));
            gc.setFill(MaterialTheme.COLOR_TEXT_DISABLED);
            gc.fillText(String.valueOf(ch + 1), colCenterX, h - 8);
        }
    }

    public void updateLocalizedTexts() {
        if (titleLabel != null) {
            titleLabel.setText(de.artnet2dmx.util.I18n.get("vis.title"));
        }
        if (lblBereich != null) {
            lblBereich.setText(de.artnet2dmx.util.I18n.get("vis.range"));
        }
    }

    /**
     * Dedizierter Container für das Canvas.
     * Hält das Canvas im Status 'unmanaged', sodass Änderungen an der Canvas-Breite
     * oder -Höhe niemals ein erneutes Layout des VBox-Elterncontainers anfordern können.
     */
    private static class CanvasPane extends Pane {
        private final Canvas canvas;
        private final ChannelVisualizer visualizer;

        public CanvasPane(Canvas canvas, ChannelVisualizer visualizer) {
            this.canvas = canvas;
            this.visualizer = visualizer;
            getChildren().add(canvas);
            canvas.setManaged(false);

            setMinHeight(60);
            setPrefHeight(160);
            setMaxHeight(Double.MAX_VALUE);
            setMinWidth(100);
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            double w = Math.floor(getWidth());
            double h = Math.floor(getHeight());
            if (w > 0 && h > 0) {
                canvas.relocate(0, 0);
                boolean sizeChanged = false;
                if (Math.abs(canvas.getWidth() - w) > 0.5) {
                    canvas.setWidth(w);
                    sizeChanged = true;
                }
                if (Math.abs(canvas.getHeight() - h) > 0.5) {
                    canvas.setHeight(h);
                    sizeChanged = true;
                }
                if (sizeChanged) {
                    visualizer.handleResize(w, h);
                }
            }
        }
    }
}
