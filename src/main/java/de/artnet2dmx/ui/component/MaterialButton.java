package de.artnet2dmx.ui.component;

import de.artnet2dmx.ui.MaterialTheme;
import de.artnet2dmx.ui.icon.LucideIcon;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Interaktiver Material Design 2 Button mit Lucide Vektor-Icon und Hover-Effekten.
 */
public class MaterialButton extends HBox {
    private final LucideIcon icon;
    private final Label label;
    private Color bgNormal;
    private Color bgHover;
    private Color bgActive;
    private Color fgColor;
    private Runnable onAction;

    public MaterialButton(
        String text,
        String iconName,
        Color bg,
        Color fg,
        double iconSize,
        double padX,
        double padY,
        double fontSize,
        boolean bold,
        Runnable onAction
    ) {
        this.onAction = onAction;
        this.bgNormal = bg;
        this.fgColor = fg;
        this.bgHover = tint(bg, 0.15);
        this.bgActive = tint(bg, -0.15);

        setAlignment(Pos.CENTER);
        setSpacing(6);
        setCursor(Cursor.HAND);
        setStyle(getBackgroundStyle(bgNormal) + String.format("; -fx-padding: %.1f %.1f %.1f %.1f; -fx-background-radius: 4px;", padY, padX, padY, padX));

        icon = new LucideIcon(iconName, iconSize, fg);
        label = new Label(text);
        label.setTextFill(fg);
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize));

        getChildren().addAll(icon, label);

        setOnMouseEntered(e -> setStyle(getBackgroundStyle(bgHover) + String.format("; -fx-padding: %.1f %.1f %.1f %.1f; -fx-background-radius: 4px;", padY, padX, padY, padX)));
        setOnMouseExited(e -> setStyle(getBackgroundStyle(bgNormal) + String.format("; -fx-padding: %.1f %.1f %.1f %.1f; -fx-background-radius: 4px;", padY, padX, padY, padX)));
        setOnMousePressed(e -> setStyle(getBackgroundStyle(bgActive) + String.format("; -fx-padding: %.1f %.1f %.1f %.1f; -fx-background-radius: 4px;", padY, padX, padY, padX)));
        setOnMouseReleased(e -> {
            setStyle(getBackgroundStyle(bgHover) + String.format("; -fx-padding: %.1f %.1f %.1f %.1f; -fx-background-radius: 4px;", padY, padX, padY, padX));
            if (this.onAction != null) {
                this.onAction.run();
            }
        });
    }

    public void setAction(Runnable onAction) {
        this.onAction = onAction;
    }

    public void setText(String text) {
        this.label.setText(text);
    }

    public void setState(String text, String iconName, Color bg, Color fg) {
        this.bgNormal = bg;
        this.fgColor = fg;
        this.bgHover = tint(bg, 0.15);
        this.bgActive = tint(bg, -0.15);

        label.setText(text);
        label.setTextFill(fg);
        icon.setIcon(iconName, fg);
        setStyle(getBackgroundStyle(bgNormal) + "; -fx-background-radius: 4px;");
    }

    private String getBackgroundStyle(Color c) {
        return String.format("-fx-background-color: #%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private static Color tint(Color c, double factor) {
        double r = Math.max(0, Math.min(1, c.getRed() + factor));
        double g = Math.max(0, Math.min(1, c.getGreen() + factor));
        double b = Math.max(0, Math.min(1, c.getBlue() + factor));
        return new Color(r, g, b, c.getOpacity());
    }
}
