package de.artnet2dmx.ui;

import de.artnet2dmx.ArtNet2DmxApp;
import de.artnet2dmx.config.AppConfig;
import de.artnet2dmx.config.ConfigManager;
import de.artnet2dmx.service.UpdateService;
import de.artnet2dmx.ui.component.MaterialButton;
import de.artnet2dmx.ui.icon.LucideIcon;
import de.artnet2dmx.util.I18n;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.logging.Logger;

/**
 * In-App Modal Dialog für Versionsinformationen, Updates und globale Einstellungen.
 * Erscheint direkt als zentriertes Overlay im Hauptfenster (kein separates OS-Fenster).
 * Enthält zwei nebeneinanderliegende Tabs ("Info & Updates" und "Einstellungen").
 */
public class AboutUpdateDialog {
    private static final Logger LOGGER = Logger.getLogger(AboutUpdateDialog.class.getName());

    public static void show(MainWindow mainWindow) {
        show(mainWindow, 0);
    }

    public static void show(MainWindow mainWindow, int initialTab) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_1DP + 
                     "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                     "; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;" +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.75), 24, 0, 0, 8);");
        card.setPrefWidth(470);
        card.setMaxWidth(470);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        // 1. Header (Logo, Titel, Schließen)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        LucideIcon iconLogo = new LucideIcon("sliders", 22, MaterialTheme.COLOR_PRIMARY);

        Label lblTitle = new Label("artnet2dmx");
        lblTitle.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblTitle.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 16));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox btnCloseTop = new HBox();
        btnCloseTop.setAlignment(Pos.CENTER);
        btnCloseTop.setPadding(new Insets(4, 6, 4, 6));
        btnCloseTop.setCursor(Cursor.HAND);
        btnCloseTop.setStyle("-fx-background-radius: 4px; -fx-background-color: transparent;");
        btnCloseTop.setOnMouseEntered(e -> btnCloseTop.setStyle("-fx-background-radius: 4px; -fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + ";"));
        btnCloseTop.setOnMouseExited(e -> btnCloseTop.setStyle("-fx-background-radius: 4px; -fx-background-color: transparent;"));

        LucideIcon iconClose = new LucideIcon("x", 16, MaterialTheme.COLOR_TEXT_MED);
        btnCloseTop.getChildren().add(iconClose);
        btnCloseTop.setOnMouseClicked(e -> mainWindow.hideOverlay());

        header.getChildren().addAll(iconLogo, lblTitle, headerSpacer, btnCloseTop);

        // 2. Tab Bar (Zwei Tabs nebeneinander: Info & Updates | Einstellungen)
        HBox tabBar = new HBox(8);
        tabBar.setAlignment(Pos.CENTER_LEFT);

        LucideIcon iconTabUp = new LucideIcon("arrow-up-circle", 14, MaterialTheme.COLOR_ON_PRIMARY);
        Button btnTabUpdates = new Button(I18n.get("tab.updates"), iconTabUp);

        LucideIcon iconTabSet = new LucideIcon("settings", 14, MaterialTheme.COLOR_TEXT_MED);
        Button btnTabSettings = new Button(I18n.get("tab.settings"), iconTabSet);

        tabBar.getChildren().addAll(btnTabUpdates, btnTabSettings);

        // 3. Tab-Inhalte
        VBox tabContentContainer = new VBox(12);

        // View A: Updates & Info
        VBox viewUpdates = new VBox(12);

        // Versions-Box
        HBox versionCard = new HBox(16);
        versionCard.setAlignment(Pos.CENTER_LEFT);
        versionCard.setPadding(new Insets(10, 14, 10, 14));
        versionCard.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                            "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                            "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        VBox curVerBox = new VBox(2);
        Label lblCurTitle = new Label(I18n.get("dialog.installed"));
        lblCurTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblCurTitle.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 9));
        Label lblCurVal = new Label("v" + UpdateService.CURRENT_VERSION);
        lblCurVal.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblCurVal.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 13));
        curVerBox.getChildren().addAll(lblCurTitle, lblCurVal);

        Region verSpacer = new Region();
        HBox.setHgrow(verSpacer, Priority.ALWAYS);

        VBox latVerBox = new VBox(2);
        latVerBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblLatTitle = new Label(I18n.get("dialog.latest"));
        lblLatTitle.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblLatTitle.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 9));
        Label lblLatVal = new Label("Prüfe...");
        lblLatVal.setTextFill(MaterialTheme.COLOR_PRIMARY);
        lblLatVal.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 13));
        latVerBox.getChildren().addAll(lblLatTitle, lblLatVal);

        versionCard.getChildren().addAll(curVerBox, verSpacer, latVerBox);

        // Status- & Aktionsbereich
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

        Label lblStatus = new Label(I18n.get("dialog.checking"));
        lblStatus.setTextFill(MaterialTheme.COLOR_TEXT_HIGH);
        lblStatus.setFont(Font.font(MaterialTheme.FONT_FAMILY, 12));
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
        lblProgressDetail.setFont(Font.font(MaterialTheme.FONT_FAMILY, 11));
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        actionContent.getChildren().addAll(txtNotes, progressBar, lblProgressDetail, buttonBox);
        statusCard.getChildren().addAll(statusRow, actionContent);
        viewUpdates.getChildren().addAll(versionCard, statusCard);

        // View B: Globale Einstellungen
        VBox viewSettings = buildSettingsView(mainWindow, btnTabUpdates, btnTabSettings, lblCurTitle, lblLatTitle);

        // Tab-Umschaltlogik
        Runnable updateTabViews = () -> {
            boolean isUpdates = (tabContentContainer.getChildren().contains(viewUpdates));
            styleTab(btnTabUpdates, iconTabUp, isUpdates);
            styleTab(btnTabSettings, iconTabSet, !isUpdates);
        };

        btnTabUpdates.setOnAction(e -> {
            tabContentContainer.getChildren().setAll(viewUpdates);
            updateTabViews.run();
        });

        btnTabSettings.setOnAction(e -> {
            tabContentContainer.getChildren().setAll(viewSettings);
            updateTabViews.run();
        });

        if (initialTab == 1) {
            tabContentContainer.getChildren().setAll(viewSettings);
        } else {
            tabContentContainer.getChildren().setAll(viewUpdates);
        }
        updateTabViews.run();

        // 4. Footer (GitHub Link)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        MaterialButton btnGithub = new MaterialButton("GitHub", "external-link",
                MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, false, () -> {
            ArtNet2DmxApp.openWebpage(UpdateService.REPO_URL);
        });

        footer.getChildren().add(btnGithub);

        card.getChildren().addAll(header, tabBar, tabContentContainer, footer);

        // Update-Prüfung ausführen
        Runnable[] checkUpdateTask = new Runnable[1];
        checkUpdateTask[0] = () -> {
            statusRow.getChildren().clear();
            statusRow.getChildren().addAll(spinner, lblStatus);
            lblStatus.setText(I18n.get("dialog.checking"));
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
                    lblStatus.setText(I18n.get("dialog.update_available") + release.tagName());
                    lblStatus.setTextFill(MaterialTheme.COLOR_PRIMARY);
                    statusRow.getChildren().addAll(iconUp, lblStatus);

                    if (release.body() != null && !release.body().isBlank()) {
                        txtNotes.setText(release.body());
                        txtNotes.setVisible(true);
                        txtNotes.setManaged(true);
                    }

                    MaterialButton btnUpdate = new MaterialButton(I18n.get("dialog.update_now"), "download",
                            MaterialTheme.COLOR_PRIMARY, MaterialTheme.COLOR_ON_PRIMARY, 14, 14, 5, 12, true, null);

                    btnUpdate.setAction(() -> {
                        btnUpdate.setDisable(true);
                        progressBar.setVisible(true);
                        progressBar.setManaged(true);
                        lblProgressDetail.setVisible(true);
                        lblProgressDetail.setManaged(true);
                        lblProgressDetail.setText(I18n.get("dialog.starting_download"));

                        UpdateService.performUpdate(
                                release,
                                progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                                status -> Platform.runLater(() -> lblProgressDetail.setText(status))
                        ).exceptionally(ex -> {
                            Platform.runLater(() -> {
                                btnUpdate.setDisable(false);
                                lblProgressDetail.setTextFill(Color.web("#CF6679"));
                                lblProgressDetail.setText(I18n.get("dialog.download_failed") + ex.getMessage());
                            });
                            return null;
                        });
                    });

                    buttonBox.getChildren().clear();
                    buttonBox.getChildren().add(btnUpdate);
                } else {
                    // Bereits auf dem neuesten Stand
                    LucideIcon iconOk = new LucideIcon("check-circle", 18, Color.web("#00E676"));
                    lblStatus.setText(I18n.get("dialog.up_to_date"));
                    lblStatus.setTextFill(Color.web("#00E676"));
                    statusRow.getChildren().addAll(iconOk, lblStatus);

                    MaterialButton btnRecheck = new MaterialButton(I18n.get("dialog.check_again"), "refresh-cw",
                            MaterialTheme.COLOR_SURFACE_4DP, MaterialTheme.COLOR_TEXT_HIGH, 12, 10, 4, 11, false, () -> checkUpdateTask[0].run());

                    buttonBox.getChildren().clear();
                    buttonBox.getChildren().add(btnRecheck);
                }
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusRow.getChildren().clear();
                    LucideIcon iconErr = new LucideIcon("alert-triangle", 18, Color.web("#CF6679"));
                    lblStatus.setText(I18n.get("dialog.offline"));
                    lblStatus.setTextFill(Color.web("#CF6679"));
                    statusRow.getChildren().addAll(iconErr, lblStatus);
                    lblLatVal.setText("Unbekannt");

                    MaterialButton btnRetry = new MaterialButton(I18n.get("dialog.check_again"), "refresh-cw",
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

    private static VBox buildSettingsView(MainWindow mainWindow,
                                         Button btnTabUpdates,
                                         Button btnTabSettings,
                                         Label lblCurTitle,
                                         Label lblLatTitle) {
        AppConfig config = mainWindow.getConfig();
        VBox card = new VBox(14);
        card.setPadding(new Insets(14, 16, 16, 16));
        card.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                      "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + 
                      "; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        // 1. Sprache / Language
        VBox langBox = new VBox(6);
        Label lblLang = new Label(I18n.get("settings.language"));
        lblLang.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblLang.setFont(Font.font(MaterialTheme.FONT_FAMILY, FontWeight.BOLD, 10));

        ComboBox<String> cbLang = new ComboBox<>();
        cbLang.getItems().addAll("Deutsch", "English");
        cbLang.setValue("en".equalsIgnoreCase(config.getLanguage()) ? "English" : "Deutsch");
        cbLang.setMaxWidth(Double.MAX_VALUE);
        cbLang.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + 
                        "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + 
                        "; -fx-border-color: " + MaterialTheme.HEX_DIVIDER + "; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        cbLang.setOnAction(e -> {
            String selected = cbLang.getValue();
            String code = "English".equalsIgnoreCase(selected) ? "en" : "de";
            config.setLanguage(code);
            I18n.setLanguage(code);
            ConfigManager.saveConfig(config);

            // Lokalisierte Texte der Dialog-Elemente sofort auffrischen
            lblLang.setText(I18n.get("settings.language"));
            btnTabUpdates.setText(I18n.get("tab.updates"));
            btnTabSettings.setText(I18n.get("tab.settings"));
            lblCurTitle.setText(I18n.get("dialog.installed"));
            lblLatTitle.setText(I18n.get("dialog.latest"));
        });
        langBox.getChildren().addAll(lblLang, cbLang);

        // 2. Autostart
        VBox autostartBox = new VBox(6);
        CheckBox chkAutostart = new CheckBox(I18n.get("settings.autostart"));
        chkAutostart.setSelected(config.isAutostart());
        chkAutostart.setStyle("-fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + "; -fx-font-size: 12px;");
        chkAutostart.setOnAction(e -> {
            config.setAutostart(chkAutostart.isSelected());
            ConfigManager.saveConfig(config);
        });
        autostartBox.getChildren().add(chkAutostart);

        // 3. Info / Speicher-Hinweis
        HBox noteRow = new HBox(6);
        noteRow.setAlignment(Pos.CENTER_LEFT);
        noteRow.setPadding(new Insets(4, 0, 0, 0));
        LucideIcon iconOk = new LucideIcon("check-circle", 12, MaterialTheme.COLOR_PRIMARY);
        Label lblNote = new Label(I18n.get("settings.saved"));
        lblNote.setTextFill(MaterialTheme.COLOR_TEXT_MED);
        lblNote.setFont(Font.font(MaterialTheme.FONT_FAMILY, 10));
        noteRow.getChildren().addAll(iconOk, lblNote);

        card.getChildren().addAll(langBox, autostartBox, noteRow);
        return card;
    }

    private static void styleTab(Button btn, LucideIcon icon, boolean active) {
        btn.setCursor(Cursor.HAND);
        if (active) {
            btn.setStyle("-fx-background-color: " + MaterialTheme.HEX_PRIMARY + 
                         "; -fx-text-fill: " + MaterialTheme.HEX_ON_PRIMARY + 
                         "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-padding: 6px 14px; -fx-border-width: 0;");
            icon.setColor(MaterialTheme.COLOR_ON_PRIMARY);
            btn.setOnMouseEntered(null);
            btn.setOnMouseExited(null);
        } else {
            btn.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                         "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_MED + 
                         "; -fx-font-weight: normal; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-padding: 6px 14px; -fx-border-width: 0;");
            icon.setColor(MaterialTheme.COLOR_TEXT_MED);
            btn.setOnMouseEntered(e -> {
                btn.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_4DP + 
                             "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_HIGH + 
                             "; -fx-font-weight: normal; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-padding: 6px 14px; -fx-border-width: 0;");
                icon.setColor(MaterialTheme.COLOR_TEXT_HIGH);
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle("-fx-background-color: " + MaterialTheme.HEX_SURFACE_2DP + 
                             "; -fx-text-fill: " + MaterialTheme.HEX_TEXT_MED + 
                             "; -fx-font-weight: normal; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-padding: 6px 14px; -fx-border-width: 0;");
                icon.setColor(MaterialTheme.COLOR_TEXT_MED);
            });
        }
    }
}
