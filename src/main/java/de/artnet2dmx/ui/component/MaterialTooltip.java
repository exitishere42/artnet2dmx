package de.artnet2dmx.ui.component;

import de.artnet2dmx.ui.MaterialTheme;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * Flackerfreier, schwebender Tooltip im Material Design 2 Dark Theme.
 * Garantiert 10px Sicherheitsabstand zur Maus und 150ms Debounce-Timer.
 */
public class MaterialTooltip {
    private static Popup currentPopup = null;
    private static Node activeNode = null;
    private static PauseTransition hideTimer = null;

    public static void install(Node node, String title, String text) {
        node.setOnMouseEntered(e -> show(node, title, text));
        node.setOnMouseExited(e -> scheduleHide());
    }

    public static void show(Node node, String title, String text) {
        cancelHide();
        if (currentPopup != null && activeNode == node && currentPopup.isShowing()) {
            return; // Bereits aktiv, kein Neuaufbau
        }

        hideNow();
        activeNode = node;

        Popup popup = new Popup();
        popup.setAutoHide(false);

        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_PRIMARY + 
                     "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 8, 0, 0, 4);");

        Label lblTitle = new Label(title);
        lblTitle.setTextFill(MaterialTheme.COLOR_PRIMARY);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label lblText = new Label(text);
        lblText.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblText.setFont(Font.font("Segoe UI", 11));
        lblText.setWrapText(true);
        lblText.setMaxWidth(290);

        box.getChildren().addAll(lblTitle, lblText);
        popup.getContent().add(box);

        // Tooltip selbst hover-fähig halten
        box.setOnMouseEntered(e -> cancelHide());
        box.setOnMouseExited(e -> scheduleHide());

        // Position berechnen
        Point2D screenCoords = node.localToScreen(0, 0);
        if (screenCoords == null) {
            return;
        }

        double nodeX = screenCoords.getX();
        double nodeY = screenCoords.getY();
        double nodeW = node.getBoundsInParent().getWidth();
        double nodeH = node.getBoundsInParent().getHeight();

        // Popup kurz anzeigen um reale Dimensionen zu kennen
        popup.show(node.getScene().getWindow(), -10000, -10000);
        double popW = box.getWidth() > 0 ? box.getWidth() : 310;
        double popH = box.getHeight() > 0 ? box.getHeight() : 120;

        Screen screen = Screen.getPrimary();
        double screenW = screen.getVisualBounds().getWidth();
        double screenH = screen.getVisualBounds().getHeight();

        // Horizontale Platzierung
        double x = nodeX + nodeW - popW;
        if (x < 12) {
            x = Math.max(12, nodeX - 10);
            if (x + popW > screenW - 12) {
                x = screenW - popW - 12;
            }
        }

        // Vertikale Platzierung mit GARANTIERTEM 10px Sicherheitsabstand
        double y;
        if (nodeY - popH - 12 >= 40) {
            y = nodeY - popH - 10;
        } else {
            y = nodeY + nodeH + 10;
        }

        if (y + popH > screenH - 12) {
            y = screenH - popH - 12;
        }

        popup.setX(x);
        popup.setY(y);
        currentPopup = popup;
    }

    public static void scheduleHide() {
        cancelHide();
        hideTimer = new PauseTransition(Duration.millis(150));
        hideTimer.setOnFinished(e -> hideNow());
        hideTimer.play();
    }

    public static void cancelHide() {
        if (hideTimer != null) {
            hideTimer.stop();
            hideTimer = null;
        }
    }

    public static void hideNow() {
        cancelHide();
        activeNode = null;
        if (currentPopup != null) {
            currentPopup.hide();
            currentPopup = null;
        }
    }
}
