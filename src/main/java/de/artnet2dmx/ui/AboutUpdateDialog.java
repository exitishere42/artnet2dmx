package de.artnet2dmx.ui;

import de.artnet2dmx.service.UpdateService;
import de.artnet2dmx.ui.component.MaterialButton;
import de.artnet2dmx.ui.icon.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

/**
 * In-App Modal Dialog für Versionsinformationen und automatische Updates.
 * Erscheint direkt als zentriertes Overlay im Hauptfenster (kein separates OS-Fenster).
 */
public class AboutUpdateDialog {
    private static final Logger LOGGER = Logger.getLogger(AboutUpdateDialog.class.getName());

    public static void show(MainWindow mainWindow) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_1DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;" +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.75), 24, 0, 0, 8);");
        card.setPrefWidth(460);
        card.setMaxWidth(460);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        // 1. Header (Logo, Titel, Schließen)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon iconLogo = new LucideIcon("sliders", 22, MaterialTheme.COLOR_PRIMARY);

        VBox titleBox = new VBox(1);
        Label lblTitle = new Label("artnet2dmx");
        lblTitle.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        Label lblSub = new Label("Art-Net 4 zu DMX512 Hardware Bridge");
        lblSub.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblSub.setFont(Font.font("Segoe UI", 11));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        LucideIcon iconClose = new LucideIcon("x", 16, MaterialTheme.COLOR_TEXT_MED);
        iconClose.setCursor(Cursor.HAND);
        iconClose.setOnMouseClicked(e -> mainWindow.hideOverlay());

        header.getChildren().addAll(iconLogo, titleBox, headerSpacer, iconClose);

        // 2. Versions-Box
        HBox versionCard = new HBox(16);
        versionCard.setAlignment(Pos.CENTER_LEFT);
        versionCard.setPadding(new Insets(10, 14, 10, 14));
        versionCard.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                            "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                            "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        VBox curVerBox = new VBox(2);
        Label lblCurTitle = new Label("INSTALLIERT");
        lblCurTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblCurTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        Label lblCurVal = new Label("v" + UpdateService.CURRENT_VERSION);
        lblCurVal.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblCurVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        curVerBox.getChildren().addAll(lblCurTitle, lblCurVal);

        Region verSpacer = new Region();
        HBox.setHgrow(verSpacer, Priority.ALWAYS);

        VBox latVerBox = new VBox(2);
        latVerBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblLatTitle = new Label("NEUESTE VERSION");
        lblLatTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblLatTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        Label lblLatVal = new Label("Prüfe...");
        lblLatVal.setTextFill(MaterialTheme.COLOR_PRIMARY);
        lblLatVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        latVerBox.getChildren().addAll(lblLatTitle, lblLatVal);

        versionCard.getChildren().addAll(curVerBox, verSpacer, latVerBox);

        // 3. Status- & Aktionsbereich
        VBox statusCard = new VBox(10);
        statusCard.setPadding(new Insets(12, 14, 12, 14));
        statusCard.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                           "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                           "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(16, 16);
        spinner.setStyle("-fx-progress-color: " + MaterialTheme.HEX_PRIMARY + ";");

        Label lblStatus = new Label("Prüfe auf Updates bei GitHub...");
        lblStatus.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblStatus.setFont(Font.font("Segoe UI", 12));
        statusRow.getChildren().addAll(spinner, lblStatus);

        VBox actionContent = new VBox(8);

        TextArea txtNotes = new TextArea();
        txtNotes.setEditable(false);
        txtNotes.setWrapText(true);
        txtNotes.setPrefRowCount(4);
        txtNotes.setStyle("-fx-control-inner-background: #181818; -fx-text-fill: #E1E1E1; -fx-font-size: 11px; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + ";");
        txtNotes.setVisible(false);
        txtNotes.setManaged(false);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + MaterialTheme.HEX_PRIMARY + ";");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        Label lblProgressDetail = new Label();
        lblProgressDetail.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblProgressDetail.setFont(Font.font("Segoe UI", 11));
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        actionContent.getChildren().addAll(txtNotes, progressBar, lblProgressDetail, buttonBox);
        statusCard.getChildren().addAll(statusRow, actionContent);

        // 4. Footer (GitHub Link & Schließen)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        MaterialButton btnGithub = new MaterialButton("GitHub", "external-link",
                MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, false, () -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(UpdateService.REPO_URL));
                }
            } catch (Exception e) {
                LOGGER.warning("Konnte Browser nicht öffnen: " + e.getMessage());
            }
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        MaterialButton btnClose = new MaterialButton("Schließen", "x",
                MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 12, 4, 11, false, mainWindow::hideOverlay);

        footer.getChildren().addAll(btnGithub, footerSpacer, btnClose);

        card.getChildren().addAll(header, versionCard, statusCard, footer);

        // Update-Prüfung ausführen
        Runnable[] checkUpdateTask = new Runnable[1];
        checkUpdateTask[0] = () -> {
            statusRow.getChildren().clear();
            statusRow.getChildren().addAll(spinner, lblStatus);
            lblStatus.setText("Prüfe auf Updates bei GitHub...");
            lblLatVal.setText("Prüfe...");
            buttonBox.getChildren().clear();
            txtNotes.setVisible(false);
            txtNotes.setManaged(false);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            lblProgressDetail.setVisible(false);
            lblProgressDetail.setManaged(false);

            UpdateService.checkLatestRelease().thenAccept(release -> Platform.runLater(() -> {
                statusRow.getChildren().clear();
                lblLatVal.setText(release.tagName());

                if (release.isNewer()) {
                    // Neue Version verfügbar!
                    LucideIcon iconUp = new LucideIcon("arrow-up-circle", 18, MaterialTheme.COLOR_PRIMARY);
                    lblStatus.setText("Neue Version verfügbar: " + release.tagName());
                    lblStatus.setTextFill(MaterialTheme.COLOR_PRIMARY);
                    statusRow.getChildren().addAll(iconUp, lblStatus);

                    if (release.body() != null && !release.body().isBlank()) {
                        txtNotes.setText(release.body());
                        txtNotes.setVisible(true);
                        txtNotes.setManaged(true);
                    }

                    MaterialButton btnUpdate = new MaterialButton("Jetzt aktualisieren", "download",
                            MaterialTheme.COLOR_PRIMARY, MaterialTheme.COLOR_ON_PRIMARY, 14, 14, 5, 12, true, null);

                    btnUpdate.setAction(() -> {
                        btnUpdate.setDisable(true);
                        progressBar.setVisible(true);
                        progressBar.setManaged(true);
                        lblProgressDetail.setVisible(true);
                        lblProgressDetail.setManaged(true);
                        lblProgressDetail.setText("Starte Download...");

                        UpdateService.performUpdate(
                                release,
                                progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                                status -> Platform.runLater(() -> lblProgressDetail.setText(status))
                        ).exceptionally(ex -> {
                            Platform.runLater(() -> {
                                btnUpdate.setDisable(false);
                                lblProgressDetail.setTextFill(Color.web("#CF6679"));
                                lblProgressDetail.setText("Update fehlgeschlagen: " + ex.getMessage());
                            });
                            return null;
                        });
                    });

                    buttonBox.getChildren().clear();
                    buttonBox.getChildren().add(btnUpdate);
                } else {
                    // Bereits auf dem neuesten Stand
                    LucideIcon iconOk = new LucideIcon("check-circle", 18, Color.web("#00E676"));
                    lblStatus.setText("artnet2dmx ist auf dem neuesten Stand.");
                    lblStatus.setTextFill(Color.web("#00E676"));
                    statusRow.getChildren().addAll(iconOk, lblStatus);

                    MaterialButton btnRecheck = new MaterialButton("Erneut prüfen", "refresh-cw",
                            MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, false, () -> checkUpdateTask[0].run());

                    buttonBox.getChildren().clear();
                    buttonBox.getChildren().add(btnRecheck);
                }
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusRow.getChildren().clear();
                    LucideIcon iconErr = new LucideIcon("alert-triangle", 18, Color.web("#CF6679"));
                    lblStatus.setText("Update-Prüfung nicht möglich (offline).");
                    lblStatus.setTextFill(Color.web("#CF6679"));
                    statusRow.getChildren().addAll(iconErr, lblStatus);
                    lblLatVal.setText("Unbekannt");

                    MaterialButton btnRetry = new MaterialButton("Erneut prüfen", "refresh-cw",
                            MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, false, () -> checkUpdateTask[0].run());

                    buttonBox.getChildren().clear();
                    buttonBox.getChildren().add(btnRetry);
                });
                return null;
            });
        };

        mainWindow.showOverlay(card);
        checkUpdateTask[0].run();
    }
}
