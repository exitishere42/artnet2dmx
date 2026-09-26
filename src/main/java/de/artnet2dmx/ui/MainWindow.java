package de.artnet2dmx.ui;

import de.artnet2dmx.artnet.ArtNetReceiver;
import de.artnet2dmx.config.AppConfig;
import de.artnet2dmx.config.ConfigManager;
import de.artnet2dmx.dmx.*;
import de.artnet2dmx.ui.component.*;
import de.artnet2dmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.logging.Logger;

/**
 * Hauptansicht der artnet2dmx Anwendung im Material Design 2 Dark Theme.
 * 1:1 identisches Layout zur Python-Referenz.
 */
public class MainWindow extends StackPane {
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());

    private final AppConfig config;
    private final VBox contentBox = new VBox();
    private final StackPane overlayPane = new StackPane();

    // Top Bar
    private LucideIcon chipIcon;
    private Label chipLabel;

    // Metriken
    private Label lblPps;
    private Label lblFps;
    private Label lblTotal;
    private Label lblDriver;

    // Controls
    private ComboBox<String> cbDriver;
    private TextField txtPort;
    private Spinner<Integer> spUniverse;
    private Spinner<Integer> spFps;
    private MaterialButton btnAction;

    // Visualizer, Charts & Status
    private MetricHistoryChart chartPps;
    private MetricHistoryChart chartFps;
    private ChannelVisualizer visualizer;
    private Label statusLabel;

    // Backend Runtime (Zentraler Bridge Service)
    private final de.artnet2dmx.service.DmxBridgeService bridgeService = new de.artnet2dmx.service.DmxBridgeService();
    private boolean isRunning = false;

    private long lastDiagnosticLog = 0;
    private long lastChartTime = 0;

    public MainWindow() {
        this.config = ConfigManager.loadConfig();

        contentBox.setStyle("-fx-background-color: " + MaterialTheme.HEX_BG + ";");
        contentBox.setSpacing(0);
        contentBox.setPadding(new Insets(0, 0, 4, 0));

        buildTopAppBar();

        Region metricsNode = buildMetricsCard();
        Region controlsNode = buildControlsCard();
        Region channelsNode = buildChannelsCard();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Die 3 anpassbaren Module mit minimalem und maximalem Höhen-Limit
        // Obere Lücke zur Logobar entspricht exakt der 12px Trenner-Lücke der Pille:
        VBox wrapMetrics = wrapSplitItem(metricsNode, new Insets(12, 16, 5, 16), 72, 350);
        VBox wrapControls = wrapSplitItem(controlsNode, new Insets(5, 16, 5, 16), 88, 170);
        VBox wrapChannels = wrapSplitItem(channelsNode, new Insets(5, 16, 8, 16), 140, Double.MAX_VALUE);

        splitPane.getItems().addAll(wrapMetrics, wrapControls, wrapChannels);
        SplitPane.setResizableWithParent(wrapMetrics, false);
        SplitPane.setResizableWithParent(wrapControls, false);
        SplitPane.setResizableWithParent(wrapChannels, true);

        Platform.runLater(() -> splitPane.setDividerPositions(0.22, 0.405));

        contentBox.getChildren().add(splitPane);
        buildStatusBar();

        // In-App Modal Overlay Layer
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        overlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayPane) {
                hideOverlay();
            }
        });

        getChildren().addAll(contentBox, overlayPane);

        if (config.getPort() == null || config.getPort().equalsIgnoreCase("Auto-Detect")) {
            scanPorts();
        }
    }

    public void showOverlay(Node modalContent) {
        overlayPane.getChildren().setAll(modalContent);
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
        modalContent.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), modalContent);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public void hideOverlay() {
        if (!overlayPane.getChildren().isEmpty()) {
            Node modalContent = overlayPane.getChildren().get(0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(120), modalContent);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                overlayPane.getChildren().clear();
                overlayPane.setVisible(false);
                overlayPane.setManaged(false);
            });
            ft.play();
        } else {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
        }
    }

    private VBox wrapSplitItem(Region content, Insets insets, double contentMinH, double contentMaxH) {
        VBox wrap = new VBox(content);
        wrap.setPadding(insets);
        double totalMinH = contentMinH + insets.getTop() + insets.getBottom();
        double totalMaxH = (contentMaxH == Double.MAX_VALUE) ? Double.MAX_VALUE : (contentMaxH + insets.getTop() + insets.getBottom());
        wrap.setMinHeight(totalMinH);
        wrap.setMaxHeight(totalMaxH);
        VBox.setVgrow(content, Priority.ALWAYS);
        content.setMinHeight(contentMinH);
        content.setMaxHeight(Double.MAX_VALUE);
        content.setMaxWidth(Double.MAX_VALUE);
        return wrap;
    }

    public AppConfig getConfig() {
        return config;
    }

    private void buildTopAppBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 0 0 1px 0;");

        // Klickbarer Logo- & Versionsbereich für Info & Updates
        HBox logoBox = new HBox(8);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setCursor(Cursor.HAND);
        logoBox.setPadding(new Insets(3, 8, 3, 6));
        logoBox.setStyle("-fx-background-radius: 6px; -fx-background-color: transparent;");
        logoBox.setOnMouseEntered(e -> logoBox.setStyle("-fx-background-radius: 6px; -fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + ";"));
        logoBox.setOnMouseExited(e -> logoBox.setStyle("-fx-background-radius: 6px; -fx-background-color: transparent;"));

        LucideIcon iconLogo = new LucideIcon("sliders", 20, MaterialTheme.COLOR_PRIMARY);

        Label title = new Label("artnet2dmx");
        title.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

        Label versionBadge = new Label("v" + de.artnet2dmx.service.UpdateService.CURRENT_VERSION);
        versionBadge.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + 
                             "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_MED + 
                             "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 8px;");

        logoBox.getChildren().addAll(iconLogo, title, versionBadge);
        logoBox.setOnMouseClicked(e -> AboutUpdateDialog.show(MainWindow.this));
        MaterialTooltip.install(logoBox, "artnet2dmx", "Klicken für Versionsinformationen & automatische Updates");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Chip
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(4, 10, 4, 10));
        chip.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + "; -fx-background-radius: 12px;");

        chipIcon = new LucideIcon("circle", 10, MaterialTheme.COLOR_TEXT_MED);
        chipLabel = new Label("BEREIT");
        chipLabel.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        chipLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        chip.getChildren().addAll(chipIcon, chipLabel);

        bar.getChildren().addAll(logoBox, spacer, chip);
        contentBox.getChildren().add(bar);
    }

    private Region buildMetricsCard() {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_1DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");
        card.setMinHeight(68);
        card.setPrefHeight(105);
        card.setMaxHeight(Double.MAX_VALUE);

        // 1. EINGANG BEREICH (Art-Net)
        HBox inputSection = new HBox(10);
        inputSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputSection, Priority.ALWAYS);

        VBox inputLabels = new VBox(6);
        inputLabels.setAlignment(Pos.CENTER_LEFT);
        inputLabels.setMinWidth(120);

        // Eingang Rate
        VBox colInRate = new VBox(1);
        HBox hdrIn = new HBox(4);
        hdrIn.setAlignment(Pos.CENTER_LEFT);
        LucideIcon iconIn = new LucideIcon("activity", 12, MaterialTheme.COLOR_PRIMARY);
        Label lblInTitle = new Label("EINGANG");
        lblInTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblInTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        hdrIn.getChildren().addAll(iconIn, lblInTitle);

        lblPps = new Label("0.0 pkt/s");
        lblPps.setStyle("-fx-text-fill: " + MaterialTheme.HEX_PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        colInRate.getChildren().addAll(hdrIn, lblPps);

        // Pakete
        VBox colPakete = new VBox(1);
        Label lblPaketeHdr = new Label("PAKETE");
        lblPaketeHdr.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblPaketeHdr.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));

        lblTotal = new Label("0");
        lblTotal.setStyle("-fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        colPakete.getChildren().addAll(lblPaketeHdr, lblTotal);

        inputLabels.getChildren().addAll(colInRate, colPakete);

        chartPps = new MetricHistoryChart("EINGANG (PKT/S)", MaterialTheme.COLOR_PRIMARY, 50.0);
        HBox.setHgrow(chartPps, Priority.ALWAYS);

        inputSection.getChildren().addAll(inputLabels, chartPps);

        // Vertikaler Trenner zwischen Eingang und Ausgang
        Region divider = new Region();
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefWidth(1);
        divider.setStyle("-fx-background-color: " + MaterialTheme.HEX_DIVIDER + ";");
        HBox.setMargin(divider, new Insets(2, 4, 2, 4));
        VBox.setVgrow(divider, Priority.ALWAYS);

        // 2. AUSGANG BEREICH (DMX512)
        HBox outputSection = new HBox(10);
        outputSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(outputSection, Priority.ALWAYS);

        VBox outputLabels = new VBox(6);
        outputLabels.setAlignment(Pos.CENTER_LEFT);
        outputLabels.setMinWidth(120);

        // Ausgang Rate
        VBox colOutRate = new VBox(1);
        HBox hdrOut = new HBox(4);
        hdrOut.setAlignment(Pos.CENTER_LEFT);
        LucideIcon iconOut = new LucideIcon("trending-up", 12, Color.web("#00E676"));
        Label lblOutTitle = new Label("AUSGANG");
        lblOutTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblOutTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        hdrOut.getChildren().addAll(iconOut, lblOutTitle);

        lblFps = new Label("0.0 fps");
        lblFps.setStyle("-fx-text-fill: #00E676; -fx-font-weight: bold; -fx-font-size: 15px;");
        colOutRate.getChildren().addAll(hdrOut, lblFps);

        // Treiber
        VBox colTreiber = new VBox(1);
        Label lblTreiberHdr = new Label("TREIBER");
        lblTreiberHdr.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblTreiberHdr.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));

        lblDriver = new Label(config.getDriver() != null ? config.getDriver() : "jna-ftdi");
        lblDriver.setStyle("-fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        colTreiber.getChildren().addAll(lblTreiberHdr, lblDriver);

        outputLabels.getChildren().addAll(colOutRate, colTreiber);

        chartFps = new MetricHistoryChart("AUSGANG (FPS)", Color.web("#00E676"), 45.0);
        HBox.setHgrow(chartFps, Priority.ALWAYS);

        outputSection.getChildren().addAll(outputLabels, chartFps);

        card.getChildren().addAll(inputSection, divider, outputSection);
        return card;
    }

    private Region buildControlsCard() {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_1DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");
        card.setMinHeight(88);
        card.setPrefHeight(92);
        card.setMaxHeight(170);
        card.setFillHeight(true);

        // 1. Treiber Box
        VBox boxDrv = createControlBox("TREIBER", "DMX-Ausgabetreiber",
            "Wählt die Schnittstelle zum DMX-Interface:\n\n• jna-ftdi: Nativer Java-Treiber via JNA/libusb (100% ohne Python!)\n• pyftdi: Python-Hardwaretreiber via libusb (Referenz aus old_artnet2dmx)\n• serial: Virtueller COM-Port /dev/ttyUSB0\n• enttec-pro: Für Enttec DMX USB Pro kompatible Adapter\n• dummy: Simulation ohne Hardware");
        boxDrv.setMinWidth(130);
        cbDriver = new ComboBox<>();
        cbDriver.getItems().addAll("jna-ftdi", "pyftdi", "serial", "enttec-pro", "dummy");
        cbDriver.setValue(config.getDriver() != null ? config.getDriver() : "jna-ftdi");
        cbDriver.setMaxWidth(Double.MAX_VALUE);
        boxDrv.getChildren().add(cbDriver);

        // 2. Port / Adapter Box (Expandierend)
        VBox boxPort = createControlBox("PORT / ADAPTER", "Port / Adapter-Adresse",
            "Gibt den Gerätepfad an:\n\n• Auto-Detect: Erkennt DMX-Adapter automatisch\n• Scan: Durchsucht USB-Anschlüsse nach DMX-Hardware\n• Manuelle Eingabe: z. B. /dev/ttyUSB0 oder COM3");
        HBox.setHgrow(boxPort, Priority.ALWAYS);

        HBox rowPort = new HBox(6);
        rowPort.setAlignment(Pos.CENTER_LEFT);
        txtPort = new TextField(config.getPort() != null ? config.getPort() : "Auto-Detect");
        HBox.setHgrow(txtPort, Priority.ALWAYS);

        MaterialButton btnScan = new MaterialButton("Scan", "refresh-cw", 
            MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, true, this::scanPorts);
        rowPort.getChildren().addAll(txtPort, btnScan);
        boxPort.getChildren().add(rowPort);

        // 3. Universum Box
        VBox boxUni = createControlBox("UNIVERSUM", "Art-Net Universum",
            "Filtert eingehende Art-Net DMX512-Pakete:\n\n• 0: Erstes Standard-Universum (z. B. QLC+, SoundSwitch)\n• 0 bis 15: Spezifisches Universum abhören\n• -1: Alle eingehenden Universen verarbeiten");
        boxUni.setMinWidth(100);
        spUniverse = new Spinner<>(-1, 15, config.getUniverse());
        spUniverse.setEditable(true);
        spUniverse.setMaxWidth(Double.MAX_VALUE);
        boxUni.getChildren().add(spUniverse);

        // 4. FPS Box
        VBox boxFps = createControlBox("ZIEL-FPS", "DMX512 Bildwiederholrate",
            "Wiederholrate der DMX512-Frames an die Scheinwerfer:\n\n• 35 Hz: Empfohlener Standardwert (flüssig für Moving Heads)\n• 10–44 Hz: Nach DMX512-Norm\n• Kontinuierlicher Puffer verhindert Scheinwerfer-Blackouts");
        boxFps.setMinWidth(90);
        spFps = new Spinner<>(10, 44, config.getFps());
        spFps.setEditable(true);
        spFps.setMaxWidth(Double.MAX_VALUE);
        boxFps.getChildren().add(spFps);

        // 5. Start/Stop Action Box
        VBox boxAction = createControlBox("AKTION", "Bridge-Aktivierung",
            "Startet oder stoppt den DMX-Ausgabedienst:\n\n• Start: Verbindet mit DMX-Adapter und leitet Art-Net live weiter\n• Stop: Hält DMX-Ausgabe an und trennt die Schnittstelle sicher");
        boxAction.setMinWidth(110);
        btnAction = new MaterialButton("Start", "play", 
            MaterialTheme.COLOR_PRIMARY, MaterialTheme.COLOR_ON_PRIMARY, 15, 16, 4, 12, true, this::toggleService);
        btnAction.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(btnAction, Priority.ALWAYS);
        boxAction.getChildren().add(btnAction);

        card.getChildren().addAll(boxDrv, boxPort, boxUni, boxFps, boxAction);
        return card;
    }

    private VBox createControlBox(String title, String tooltipTitle, String tooltipText) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(6, 10, 8, 10));
        box.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        HBox hdr = new HBox();
        hdr.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(title);
        lbl.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HelpBadge help = new HelpBadge(tooltipTitle, tooltipText);
        hdr.getChildren().addAll(lbl, spacer, help);

        box.getChildren().add(hdr);
        return box;
    }

    private Region buildChannelsCard() {
        visualizer = new ChannelVisualizer();
        visualizer.setMinHeight(120);
        visualizer.setMaxHeight(Double.MAX_VALUE);
        return visualizer;
    }

    private void buildStatusBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 16, 6, 16));

        statusLabel = new Label("Bereit. Klicken Sie auf Start.");
        statusLabel.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        statusLabel.setFont(Font.font("Segoe UI", 11));

        bar.getChildren().add(statusLabel);
        contentBox.getChildren().add(bar);
    }

    private void scanPorts() {
        List<SerialOpenDmxSender.PortInfo> ports = SerialOpenDmxSender.listAvailablePorts();
        if (ports.isEmpty()) {
            statusLabel.setText("Keine seriellen Anschlüsse gefunden.");
            return;
        }

        SerialOpenDmxSender.PortInfo match = ports.stream()
                .filter(SerialOpenDmxSender.PortInfo::isLikelyDmx)
                .findFirst()
                .orElse(ports.get(0));

        txtPort.setText(match.systemPortName());
        statusLabel.setText("Adapter gewählt: " + match.systemPortName() + " (" + match.descriptivePortName() + ")");
    }

    public void toggleService() {
        if (isRunning) {
            stopService();
        } else {
            startService();
        }
    }

    private void startService() {
        try {
            config.setDriver(cbDriver.getValue());
            String p = txtPort.getText().trim();
            config.setPort(p.equalsIgnoreCase("Auto-Detect") ? null : p);
            config.setUniverse(spUniverse.getValue());
            config.setFps(spFps.getValue());
            ConfigManager.saveConfig(config);

            bridgeService.start(config);
            isRunning = true;

            btnAction.setState("Stop", "square", MaterialTheme.COLOR_ERROR, MaterialTheme.COLOR_ON_ERROR);
            chipIcon.setIcon("circle-dot", MaterialTheme.COLOR_PRIMARY);
            chipLabel.setText("AKTIV");
            chipLabel.setTextFill(MaterialTheme.COLOR_PRIMARY);
            lblDriver.setText(config.getDriver());
            statusLabel.setText("Lauscht auf UDP " + config.getUdpPort() + " | Universum: " + 
                (config.getUniverse() == -1 ? "Alle" : config.getUniverse()) + " | Ausgabe: " + bridgeService.getSenderName());
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Starten der DMX-Bridge: " + e.getMessage());
            statusLabel.setText("Fehler beim Start: " + e.getMessage());
            stopService();
        }
    }

    public void stopService() {
        if (isRunning) {
            LOGGER.info("DMX-Bridge wird angehalten...");
        }
        isRunning = false;
        bridgeService.stop();

        btnAction.setState("Start", "play", MaterialTheme.COLOR_PRIMARY, MaterialTheme.COLOR_ON_PRIMARY);
        chipIcon.setIcon("circle", MaterialTheme.COLOR_TEXT_MED);
        chipLabel.setText("BEREIT");
        chipLabel.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblPps.setText("0.0 pkt/s");
        lblFps.setText("0.0 fps");
        lblTotal.setText("0");
        lblDriver.setText(config.getDriver() != null ? config.getDriver() : "jna-ftdi");
        statusLabel.setText("Bridge gestoppt.");

        visualizer.reset();
        if (chartPps != null) {
            chartPps.reset();
        }
        if (chartFps != null) {
            chartFps.reset();
        }
        LOGGER.info("DMX-Bridge gestoppt.");
    }

    public void tick() {
        if (isRunning && bridgeService.isRunning()) {
            double pps = bridgeService.getActualPps();
            double fps = bridgeService.getActualFps();
            long total = bridgeService.getPacketsTargetUniverse();

            lblPps.setText(String.format("%.1f pkt/s", pps));
            lblFps.setText(String.format("%.1f fps", fps));
            lblTotal.setText(String.format("%,d", total));
            lblDriver.setText(bridgeService.getSenderName());

            // 5-Minuten Telemetrie-Charts im 1-Sekunden-Takt aktualisieren
            long now = System.currentTimeMillis();
            if (now - lastChartTime >= 1000) {
                lastChartTime = now;
                if (chartPps != null) {
                    chartPps.addSample(pps);
                }
                if (chartFps != null) {
                    chartFps.addSample(fps);
                }
            }

            // Visualizer mit aktuellen 512 Kanälen aktualisieren
            int[] channels = bridgeService.getAllChannels();
            visualizer.updateChannels(channels);

            if (now - lastDiagnosticLog >= 5000) {
                lastDiagnosticLog = now;
                LOGGER.info(String.format("[Diagnostics] AKTIV | ArtNet: %.1f pkt/s (Total: %,d) | DMX Out: %.1f fps",
                        pps, total, fps));
            }

            if (total == 0) {
                statusLabel.setText(String.format(
                    "Warte auf Art-Net Pakete auf Port %d (Filter: Universum %s) -> DMX: %s",
                    config.getUdpPort(), (config.getUniverse() == -1 ? "Alle (-1)" : String.valueOf(config.getUniverse())),
                    bridgeService.getSenderName()
                ));
            } else {
                statusLabel.setText(String.format(
                    "Art-Net aktiv (%.1f pkt/s) | DMX: %s (%.1f fps, %,d Frames)",
                    pps,
                    bridgeService.getSenderName(),
                    fps,
                    bridgeService.getFramesSent()
                ));
            }
        }
    }
}
