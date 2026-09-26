package de.artnet2dmx.ui.component;

import de.artnet2dmx.ui.MaterialTheme;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Dezentes Fragezeichen-Symbol (14x14) rechts oben mit Material-Tooltip auf Hover/Klick.
 */
public class HelpBadge extends Canvas {
    private final String title;
    private final String text;

    public HelpBadge(String title, String text) {
        super(14, 14);
        this.title = title;
        this.text = text;

        setCursor(Cursor.HAND);
        draw(MaterialTheme.COLOR_TEXT_DISABLED);

        setOnMouseEntered(e -> {
            draw(MaterialTheme.COLOR_PRIMARY);
            MaterialTooltip.show(this, title, text);
        });

        setOnMouseExited(e -> {
            draw(MaterialTheme.COLOR_TEXT_DISABLED);
            MaterialTooltip.scheduleHide();
        });

        setOnMouseClicked(e -> MaterialTooltip.show(this, title, text));
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
