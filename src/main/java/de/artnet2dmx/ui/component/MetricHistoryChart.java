package de.artnet2dmx.ui.component;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;

/**
 * Kompaktes 5-Minuten Verlaufsdiagramm (300 Sekunden Rolling Buffer)
 * für eine einzelne Metrik (z. B. Eingang pkt/s oder Ausgang FPS).
 * Wird direkt neben der jeweiligen Metrik-Spalte platziert.
 * 
 * Verwendet einen unmanaged Canvas im Pane, um Layout-Schleifen und Größenflackern
 * vollständig auszuschließen.
 */
public class MetricHistoryChart extends Pane {
    public static final int HISTORY_SECONDS = 300; // 5 Minuten = 300 Sekunden

    private static final Color COLOR_BG = Color.web("#141414");
    private static final Color COLOR_BORDER = Color.rgb(255, 255, 255, 0.08);
    private static final Color COLOR_GRID = Color.rgb(255, 255, 255, 0.06);
    private static final Color COLOR_TEXT_AXIS = Color.rgb(255, 255, 255, 0.45);

    private final double[] history = new double[HISTORY_SECONDS];
    private int samplesCount = 0;
    private String title;
    private final Color lineColor;
    private final double defaultMax;

    private final Canvas canvas;

    public MetricHistoryChart(String title, Color lineColor, double defaultMax) {
        this.title = title;
        this.lineColor = lineColor;
        this.defaultMax = defaultMax;

        this.canvas = new Canvas(200, 88);
        this.canvas.setManaged(false);
        getChildren().add(canvas);

        setMinHeight(55);
        setPrefHeight(88);
        setMaxHeight(Double.MAX_VALUE);

        setMinWidth(120);
        setPrefWidth(260);
        setMaxWidth(Double.MAX_VALUE);

        render();
    }

    public void setTitle(String title) {
        this.title = title;
        render();
    }

    public MetricHistoryChart(Color lineColor, double defaultMax) {
        this(null, lineColor, defaultMax);
    }

    /**
     * Fügt einen neuen Sekunden-Messwert hinzu und verschiebt die Historie um 1 Sekunde.
     */
    public void addSample(double value) {
        if (samplesCount < HISTORY_SECONDS) {
            history[samplesCount] = Math.max(0.0, value);
            samplesCount++;
        } else {
            System.arraycopy(history, 1, history, 0, HISTORY_SECONDS - 1);
            history[HISTORY_SECONDS - 1] = Math.max(0.0, value);
        }
        render();
    }

    public void reset() {
        Arrays.fill(history, 0.0);
        samplesCount = 0;
        render();
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
                render();
            }
        }
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 20 || h <= 15) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double marginLeft = 24.0;
        double marginRight = 6.0;
        double marginTop = 6.0;
        double marginBottom = 14.0;

        double plotW = w - marginLeft - marginRight;
        double plotH = h - marginTop - marginBottom;

        // Plot-Hintergrund
        gc.setFill(COLOR_BG);
        gc.fillRoundRect(marginLeft, marginTop, plotW, plotH, 3, 3);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(marginLeft, marginTop, plotW, plotH, 3, 3);

        // Optionaler Titel oben links im Plot
        if (title != null && !title.isEmpty()) {
            gc.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(Color.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0.85));
            gc.fillText(title, marginLeft + 5, marginTop + 10);
        }

        // Skalierung ermitteln (mindestens defaultMax)
        double maxVal = defaultMax;
        for (int i = 0; i < samplesCount; i++) {
            if (history[i] > maxVal) {
                maxVal = history[i];
            }
        }
        maxVal = Math.ceil(maxVal / 10.0) * 10.0;

        // 1. Horizontale Gitterlinien & Y-Achse
        gc.setFont(Font.font("Segoe UI", 9));
        gc.setTextAlign(TextAlignment.RIGHT);
        int gridLines = 2; // 0, max/2, max
        for (int i = 0; i <= gridLines; i++) {
            double ratio = (double) i / gridLines;
            double y = marginTop + plotH - (ratio * plotH);
            double val = ratio * maxVal;

            gc.setStroke(COLOR_GRID);
            gc.setLineWidth(1.0);
            gc.strokeLine(marginLeft, y, marginLeft + plotW, y);

            gc.setFill(COLOR_TEXT_AXIS);
            gc.fillText(String.format("%.0f", val), marginLeft - 3, y + 3);
        }

        // 2. Vertikale Zeitmarken (-5m, -2.5m, Jetzt)
        gc.setTextAlign(TextAlignment.CENTER);
        double[] timeRatios = {0.0, 0.5, 1.0};
        String[] timeLabels = {"-5m", "-2.5m", "Jetzt"};
        for (int t = 0; t < timeRatios.length; t++) {
            double x = marginLeft + (timeRatios[t] * plotW);
            gc.setStroke(COLOR_GRID);
            gc.strokeLine(x, marginTop, x, marginTop + plotH);

            gc.setFill(COLOR_TEXT_AXIS);
            gc.fillText(timeLabels[t], x, h - 1);
        }

        if (samplesCount < 1) {
            return;
        }

        int n = samplesCount;
        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            // X: Zeitachse (rechts = neuester Wert, links = ältester)
            double timeRatio = (double) (HISTORY_SECONDS - (n - i)) / (double) HISTORY_SECONDS;
            xs[i] = marginLeft + (timeRatio * plotW);

            double valRatio = Math.min(1.0, Math.max(0.0, history[i] / maxVal));
            ys[i] = marginTop + plotH - (valRatio * plotH);
        }

        if (n >= 2) {
            // Weicher Verlauf unter der Kurve (Area-Fill)
            double[] polyX = new double[n + 2];
            double[] polyY = new double[n + 2];
            System.arraycopy(xs, 0, polyX, 0, n);
            System.arraycopy(ys, 0, polyY, 0, n);
            polyX[n] = xs[n - 1];
            polyY[n] = marginTop + plotH;
            polyX[n + 1] = xs[0];
            polyY[n + 1] = marginTop + plotH;

            LinearGradient areaGrad = new LinearGradient(
                0, marginTop, 0, marginTop + plotH, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0.22)),
                new Stop(1, Color.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0.02))
            );
            gc.setFill(areaGrad);
            gc.fillPolygon(polyX, polyY, n + 2);

            // Linie zeichnen
            gc.setStroke(lineColor);
            gc.setLineWidth(1.6);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            for (int i = 0; i < n - 1; i++) {
                gc.strokeLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }
        }

        // Aktuellen Punkt (letzter Wert) hervorheben
        double lastX = xs[n - 1];
        double lastY = ys[n - 1];
        gc.setFill(lineColor);
        gc.fillOval(lastX - 2.0, lastY - 2.0, 4.0, 4.0);
    }
}
