package de.artnet2dmx.ui.component;

import de.artnet2dmx.ui.MaterialTheme;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Supplier;

/**
 * Dezentes Fragezeichen-Symbol (14x14) rechts oben mit Material-Tooltip auf Hover/Klick.
 * Unterstützt statische Strings sowie dynamische Supplier<String> für Live-Übersetzungen.
 */
public class HelpBadge extends Canvas {
    private Supplier<String> titleSupplier;
    private Supplier<String> textSupplier;

    public HelpBadge(String title, String text) {
        this(() -> title, () -> text);
    }

    public HelpBadge(Supplier<String> titleSupplier, Supplier<String> textSupplier) {
        super(14, 14);
        this.titleSupplier = titleSupplier;
        this.textSupplier = textSupplier;

        setCursor(Cursor.HAND);
        draw(MaterialTheme.COLOR_TEXT_DISABLED);

        setOnMouseEntered(e -> {
            draw(MaterialTheme.COLOR_PRIMARY);
            MaterialTooltip.show(this, getHelpTitle(), getHelpText());
        });

        setOnMouseExited(e -> {
            draw(MaterialTheme.COLOR_TEXT_DISABLED);
            MaterialTooltip.scheduleHide();
        });

        setOnMouseClicked(e -> MaterialTooltip.show(this, getHelpTitle(), getHelpText()));
    }

    public void updateContent(String title, String text) {
        this.titleSupplier = () -> title;
        this.textSupplier = () -> text;
    }

    public void updateContent(Supplier<String> titleSupplier, Supplier<String> textSupplier) {
        this.titleSupplier = titleSupplier;
        this.textSupplier = textSupplier;
    }

    private String getHelpTitle() {
        return titleSupplier != null ? titleSupplier.get() : "";
    }

    private String getHelpText() {
        return textSupplier != null ? textSupplier.get() : "";
    }

    private void draw(Color color) {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Kreislinie
        gc.setStroke(color);
        gc.setLineWidth(1.0);
        gc.strokeOval(1, 1, 12, 12);

        // Fragezeichen zentriert
        gc.setFill(color);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        gc.fillText("?", 4.5, 10.5);
    }
}
