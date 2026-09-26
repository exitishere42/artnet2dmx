package de.artnet2dmx.ui.icon;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Standardkonforme Vektor-Icons aus dem Lucide Icon Set (lucide.dev).
 * Strikte Einhaltung der Zero-Emoji-Regel.
 */
public class LucideIcon extends Canvas {
    private String name;
    private Color color;
    private final double size;

    public LucideIcon(String name, double size, Color color) {
        super(size, size);
        this.name = name;
        this.size = size;
        this.color = color;
        draw();
    }

    public void setIcon(String name, Color color) {
        this.name = name;
        this.color = color;
        draw();
    }

    public void setColor(Color color) {
        this.color = color;
        draw();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        double scale = size / 24.0;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(2.0 * scale);

        switch (name.toLowerCase()) {
            case "play" -> {
                // Lucide play: polygon points="6 3 20 12 6 21 6 3"
                double[] x = {7 * scale, 19 * scale, 7 * scale};
                double[] y = {4 * scale, 12 * scale, 20 * scale};
                gc.fillPolygon(x, y, 3);
                gc.strokePolygon(x, y, 3);
            }
            case "square" -> {
                // Lucide square (Stop): x=5, y=5, w=14, h=14
                gc.fillRect(5 * scale, 5 * scale, 14 * scale, 14 * scale);
                gc.strokeRect(5 * scale, 5 * scale, 14 * scale, 14 * scale);
            }
            case "activity" -> {
                // Lucide activity: 22 12h-4l-3 9L9 3l-3 9H2
                double[] ptsX = {2, 6, 9, 14, 17, 22};
                double[] ptsY = {12, 12, 3, 21, 12, 12};
                for (int i = 0; i < ptsX.length - 1; i++) {
                    gc.strokeLine(ptsX[i] * scale, ptsY[i] * scale, ptsX[i + 1] * scale, ptsY[i + 1] * scale);
                }
            }
            case "trending-up" -> {
                // Lucide trending-up: polyline 23 6 13.5 15.5 8.5 10.5 1 18, polyline 17 6 23 6 23 12
                double[] ptsX = {1, 8.5, 13.5, 23};
                double[] ptsY = {18, 10.5, 15.5, 6};
                for (int i = 0; i < ptsX.length - 1; i++) {
                    gc.strokeLine(ptsX[i] * scale, ptsY[i] * scale, ptsX[i + 1] * scale, ptsY[i + 1] * scale);
                }
                gc.strokeLine(17 * scale, 6 * scale, 23 * scale, 6 * scale);
                gc.strokeLine(23 * scale, 6 * scale, 23 * scale, 12 * scale);
            }
            case "refresh-cw" -> {
                // Lucide refresh-cw: circular arc + arrow
                gc.strokeArc(4 * scale, 4 * scale, 16 * scale, 16 * scale, 30, 280, ArcType.OPEN);
                double[] ax = {19 * scale, 19 * scale, 14 * scale};
                double[] ay = {4 * scale, 10 * scale, 7 * scale};
                gc.fillPolygon(ax, ay, 3);
            }
            case "sliders" -> {
                // Lucide sliders
                gc.strokeLine(6 * scale, 3 * scale, 6 * scale, 21 * scale);
                gc.strokeLine(12 * scale, 3 * scale, 12 * scale, 21 * scale);
                gc.strokeLine(18 * scale, 3 * scale, 18 * scale, 21 * scale);

                gc.strokeLine(3 * scale, 8 * scale, 9 * scale, 8 * scale);
                gc.strokeLine(9 * scale, 16 * scale, 15 * scale, 16 * scale);
                gc.strokeLine(15 * scale, 10 * scale, 21 * scale, 10 * scale);
            }
            case "circle" -> {
                // Lucide circle
                gc.strokeOval(4 * scale, 4 * scale, 16 * scale, 16 * scale);
            }
            case "circle-dot" -> {
                // Lucide circle-dot
                gc.strokeOval(3 * scale, 3 * scale, 18 * scale, 18 * scale);
                gc.fillOval(9 * scale, 9 * scale, 6 * scale, 6 * scale);
            }
            case "circle-help", "help-circle" -> {
                // Lucide circle-help: Kreis + Fragezeichen
                gc.strokeOval(3 * scale, 3 * scale, 18 * scale, 18 * scale);
                gc.strokeArc(8.5 * scale, 5.5 * scale, 7 * scale, 7 * scale, 0, 200, ArcType.OPEN);
                gc.strokeLine(12 * scale, 12.5 * scale, 12 * scale, 14.5 * scale);
                gc.fillOval(11.2 * scale, 16.5 * scale, 1.6 * scale, 1.6 * scale);
            }
            case "check-circle", "check" -> {
                // Lucide check-circle: Kreis + Haekchen
                gc.strokeOval(3 * scale, 3 * scale, 18 * scale, 18 * scale);
                gc.strokeLine(8 * scale, 12 * scale, 11 * scale, 15 * scale);
                gc.strokeLine(11 * scale, 15 * scale, 16 * scale, 9 * scale);
            }
            case "arrow-up-circle", "upload-cloud" -> {
                // Lucide arrow-up-circle
                gc.strokeOval(3 * scale, 3 * scale, 18 * scale, 18 * scale);
                gc.strokeLine(12 * scale, 16 * scale, 12 * scale, 8 * scale);
                gc.strokeLine(8 * scale, 12 * scale, 12 * scale, 8 * scale);
                gc.strokeLine(16 * scale, 12 * scale, 12 * scale, 8 * scale);
            }
            case "download" -> {
                // Lucide download: Pfeil nach unten + Schale
                gc.strokeLine(12 * scale, 4 * scale, 12 * scale, 14 * scale);
                gc.strokeLine(8 * scale, 10 * scale, 12 * scale, 14 * scale);
                gc.strokeLine(16 * scale, 10 * scale, 12 * scale, 14 * scale);
                gc.strokeLine(5 * scale, 16 * scale, 5 * scale, 19 * scale);
                gc.strokeLine(5 * scale, 19 * scale, 19 * scale, 19 * scale);
                gc.strokeLine(19 * scale, 19 * scale, 19 * scale, 16 * scale);
            }
            case "alert-triangle" -> {
                // Lucide alert-triangle: Dreieck + Ausrufezeichen
                double[] tx = {12 * scale, 22 * scale, 2 * scale};
                double[] ty = {3 * scale, 20 * scale, 20 * scale};
                gc.strokePolygon(tx, ty, 3);
                gc.strokeLine(12 * scale, 9 * scale, 12 * scale, 14 * scale);
                gc.fillOval(11.2 * scale, 16.5 * scale, 1.6 * scale, 1.6 * scale);
            }
            case "info" -> {
                // Lucide info: Kreis + i
                gc.strokeOval(3 * scale, 3 * scale, 18 * scale, 18 * scale);
                gc.fillOval(11.2 * scale, 7.5 * scale, 1.6 * scale, 1.6 * scale);
                gc.strokeLine(12 * scale, 11 * scale, 12 * scale, 16.5 * scale);
            }
            case "external-link" -> {
                // Lucide external-link
                gc.strokeLine(18 * scale, 13 * scale, 18 * scale, 19 * scale);
                gc.strokeLine(18 * scale, 19 * scale, 5 * scale, 19 * scale);
                gc.strokeLine(5 * scale, 19 * scale, 5 * scale, 6 * scale);
                gc.strokeLine(5 * scale, 6 * scale, 11 * scale, 6 * scale);
                gc.strokeLine(14 * scale, 4 * scale, 20 * scale, 4 * scale);
                gc.strokeLine(20 * scale, 4 * scale, 20 * scale, 10 * scale);
                gc.strokeLine(10 * scale, 14 * scale, 20 * scale, 4 * scale);
            }
            case "x" -> {
                // Lucide x
                gc.strokeLine(6 * scale, 6 * scale, 18 * scale, 18 * scale);
                gc.strokeLine(18 * scale, 6 * scale, 6 * scale, 18 * scale);
            }
            case "settings", "gear" -> {
                gc.strokeOval(8.5 * scale, 8.5 * scale, 7 * scale, 7 * scale);
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4.0;
                    double x1 = 12 * scale + Math.cos(angle) * 7.0 * scale;
                    double y1 = 12 * scale + Math.sin(angle) * 7.0 * scale;
                    double x2 = 12 * scale + Math.cos(angle) * 10.5 * scale;
                    double y2 = 12 * scale + Math.sin(angle) * 10.5 * scale;
                    gc.strokeLine(x1, y1, x2, y2);
                }
            }
            case "globe" -> {
                gc.strokeOval(2 * scale, 2 * scale, 20 * scale, 20 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 22 * scale, 12 * scale);
                gc.strokeArc(6.5 * scale, 2 * scale, 11 * scale, 20 * scale, 0, 360, ArcType.OPEN);
            }
            default -> {
                // Fallback Circle
                gc.strokeOval(4 * scale, 4 * scale, 16 * scale, 16 * scale);
            }
        }
    }
}
