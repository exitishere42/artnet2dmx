package de.artnet2dmx.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zentraler Internationalisierungsdienst für artnet2dmx (Deutsch / English).
 */
public class I18n {
    public interface LanguageChangeListener {
        void onLanguageChanged(String newLang);
    }

    private static String currentLanguage = "de";
    private static final List<LanguageChangeListener> listeners = new ArrayList<>();

    private static final Map<String, String> DE = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        // Tabs
        DE.put("tab.updates", "Info & Updates");
        EN.put("tab.updates", "Info & Updates");
        DE.put("tab.settings", "Einstellungen");
        EN.put("tab.settings", "Settings");

        // Globale Einstellungen
        DE.put("settings.language", "Sprache");
        EN.put("settings.language", "Language");
        DE.put("settings.autostart", "Dienst bei App-Start aktivieren");
        EN.put("settings.autostart", "Enable service on app startup");
        DE.put("settings.saved", "Einstellungen werden automatisch gespeichert.");
        EN.put("settings.saved", "Settings are saved automatically.");

        // Dialog Versionen & Update
        DE.put("dialog.installed", "INSTALLIERT");
        EN.put("dialog.installed", "INSTALLED");
        DE.put("dialog.latest", "NEUESTE VERSION");
        EN.put("dialog.latest", "LATEST VERSION");
        DE.put("dialog.checking", "Prüfe auf Updates bei GitHub...");
        EN.put("dialog.checking", "Checking GitHub for updates...");
        DE.put("dialog.up_to_date", "artnet2dmx ist auf dem neuesten Stand.");
        EN.put("dialog.up_to_date", "artnet2dmx is up to date.");
        DE.put("dialog.update_available", "Neue Version verfügbar: ");
        EN.put("dialog.update_available", "New version available: ");
        DE.put("dialog.update_now", "Jetzt aktualisieren");
        EN.put("dialog.update_now", "Update now");
        DE.put("dialog.check_again", "Erneut prüfen");
        EN.put("dialog.check_again", "Check again");
        DE.put("dialog.offline", "Update-Prüfung nicht möglich (offline).");
        EN.put("dialog.offline", "Update check unavailable (offline).");
        DE.put("dialog.starting_download", "Starte Download...");
        EN.put("dialog.starting_download", "Starting download...");
        DE.put("dialog.download_failed", "Update fehlgeschlagen: ");
        EN.put("dialog.download_failed", "Update failed: ");

        // Hauptfenster Status & Buttons
        DE.put("status.ready", "BEREIT");
        EN.put("status.ready", "READY");
        DE.put("status.active", "AKTIV");
        EN.put("status.active", "ACTIVE");
        DE.put("btn.start", "Start");
        EN.put("btn.start", "Start");
        DE.put("btn.stop", "Stop");
        EN.put("btn.stop", "Stop");
        DE.put("tooltip.logo", "Klicken für Versionsinformationen, Updates & globale Einstellungen");
        EN.put("tooltip.logo", "Click for version info, updates & global settings");
    }

    public static synchronized String getLanguage() {
        return currentLanguage;
    }

    public static synchronized void setLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            lang = "de";
        }
        lang = lang.trim().toLowerCase();
        if (!lang.equals("en")) {
            lang = "de";
        }
        if (!lang.equals(currentLanguage)) {
            currentLanguage = lang;
            notifyListeners();
        }
    }

    public static String get(String key) {
        Map<String, String> dict = "en".equals(currentLanguage) ? EN : DE;
        String val = dict.get(key);
        if (val == null) {
            val = DE.get(key);
        }
        return val != null ? val : key;
    }

    public static synchronized void addListener(LanguageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void removeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (LanguageChangeListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onLanguageChanged(currentLanguage);
            } catch (Exception ignored) {
            }
        }
    }
}
