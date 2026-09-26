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
        // --- Tabs ---
        DE.put("tab.updates", "Info & Updates");
        EN.put("tab.updates", "Info & Updates");
        DE.put("tab.settings", "Einstellungen");
        EN.put("tab.settings", "Settings");

        // --- Globale Einstellungen ---
        DE.put("settings.language", "Sprache");
        EN.put("settings.language", "Language");
        DE.put("settings.autostart", "Dienst bei App-Start aktivieren");
        EN.put("settings.autostart", "Enable service on app startup");
        DE.put("settings.saved", "Einstellungen werden automatisch gespeichert.");
        EN.put("settings.saved", "Settings are saved automatically.");

        // --- Dialog Versionen & Update ---
        DE.put("dialog.installed", "INSTALLIERT");
        EN.put("dialog.installed", "INSTALLED");
        DE.put("dialog.latest", "NEUESTE VERSION");
        EN.put("dialog.latest", "LATEST VERSION");
        DE.put("dialog.checking", "Prüfe auf Updates bei GitHub...");
        EN.put("dialog.checking", "Checking GitHub for updates...");
        DE.put("dialog.checking_short", "Prüfe...");
        EN.put("dialog.checking_short", "Checking...");
        DE.put("dialog.unknown", "Unbekannt");
        EN.put("dialog.unknown", "Unknown");
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

        // --- Hauptfenster Status & Buttons ---
        DE.put("status.ready", "BEREIT");
        EN.put("status.ready", "READY");
        DE.put("status.active", "AKTIV");
        EN.put("status.active", "ACTIVE");
        DE.put("btn.start", "Start");
        EN.put("btn.start", "Start");
        DE.put("btn.stop", "Stop");
        EN.put("btn.stop", "Stop");
        DE.put("btn.scan", "Scan");
        EN.put("btn.scan", "Scan");
        DE.put("tooltip.logo", "Klicken für Versionsinformationen, Updates & globale Einstellungen");
        EN.put("tooltip.logo", "Click for version info, updates & global settings");

        // --- Metriken ---
        DE.put("metric.input", "EINGANG");
        EN.put("metric.input", "INPUT");
        DE.put("metric.packets", "PAKETE");
        EN.put("metric.packets", "PACKETS");
        DE.put("metric.chart_in", "EINGANG (PKT/S)");
        EN.put("metric.chart_in", "INPUT (PKT/S)");
        DE.put("metric.output", "AUSGANG");
        EN.put("metric.output", "OUTPUT");
        DE.put("metric.driver", "TREIBER");
        EN.put("metric.driver", "DRIVER");
        DE.put("metric.chart_out", "AUSGANG (FPS)");
        EN.put("metric.chart_out", "OUTPUT (FPS)");

        // --- Steuerung (Controls) ---
        DE.put("ctrl.driver", "TREIBER");
        EN.put("ctrl.driver", "DRIVER");
        DE.put("ctrl.driver.tt_title", "DMX-Ausgabetreiber");
        EN.put("ctrl.driver.tt_title", "DMX Output Driver");
        DE.put("ctrl.driver.tt_desc", "Wählt die Schnittstelle zum DMX-Interface:\n\n• jna-ftdi: Nativer Java-Treiber via JNA/libusb (100% ohne Python!)\n• pyftdi: Python-Hardwaretreiber via libusb\n• serial: Virtueller COM-Port /dev/ttyUSB0\n• enttec-pro: Für Enttec DMX USB Pro kompatible Adapter\n• dummy: Simulation ohne Hardware");
        EN.put("ctrl.driver.tt_desc", "Selects the interface to the DMX hardware:\n\n• jna-ftdi: Native Java driver via JNA/libusb (100% pure Java!)\n• pyftdi: Python hardware driver via libusb\n• serial: Virtual COM port /dev/ttyUSB0\n• enttec-pro: For Enttec DMX USB Pro compatible adapters\n• dummy: Simulation without hardware");

        DE.put("ctrl.port", "PORT / ADAPTER");
        EN.put("ctrl.port", "PORT / ADAPTER");
        DE.put("ctrl.port.tt_title", "Port / Adapter-Adresse");
        EN.put("ctrl.port.tt_title", "Port / Adapter Address");
        DE.put("ctrl.port.tt_desc", "Gibt den Gerätepfad an:\n\n• Auto-Detect: Erkennt DMX-Adapter automatisch\n• Scan: Durchsucht USB-Anschlüsse nach DMX-Hardware\n• Manuelle Eingabe: z. B. /dev/ttyUSB0 oder COM3");
        EN.put("ctrl.port.tt_desc", "Specifies device port/path:\n\n• Auto-Detect: Detects DMX adapters automatically\n• Scan: Scans USB ports for DMX hardware\n• Manual input: e.g. /dev/ttyUSB0 or COM3");

        DE.put("ctrl.universe", "UNIVERSUM");
        EN.put("ctrl.universe", "UNIVERSE");
        DE.put("ctrl.universe.tt_title", "Art-Net Universum");
        EN.put("ctrl.universe.tt_title", "Art-Net Universe");
        DE.put("ctrl.universe.tt_desc", "Filtert eingehende Art-Net DMX512-Pakete:\n\n• 0: Erstes Standard-Universum (z. B. QLC+, SoundSwitch)\n• 0 bis 15: Spezifisches Universum abhören\n• -1: Alle eingehenden Universen verarbeiten");
        EN.put("ctrl.universe.tt_desc", "Filters incoming Art-Net DMX512 packets:\n\n• 0: First default universe (e.g. QLC+, SoundSwitch)\n• 0 to 15: Listen to specific universe\n• -1: Process all incoming universes");

        DE.put("ctrl.fps", "ZIEL-FPS");
        EN.put("ctrl.fps", "TARGET FPS");
        DE.put("ctrl.fps.tt_title", "DMX512 Bildwiederholrate");
        EN.put("ctrl.fps.tt_title", "DMX512 Refresh Rate");
        DE.put("ctrl.fps.tt_desc", "Wiederholrate der DMX512-Frames an die Scheinwerfer:\n\n• 35 Hz: Empfohlener Standardwert (flüssig für Moving Heads)\n• 10–44 Hz: Nach DMX512-Norm\n• Kontinuierlicher Puffer verhindert Scheinwerfer-Blackouts");
        EN.put("ctrl.fps.tt_desc", "Refresh rate of DMX512 frames sent to fixtures:\n\n• 35 Hz: Recommended default (smooth for moving heads)\n• 10–44 Hz: According to DMX512 standard\n• Continuous buffer prevents fixture blackouts");

        DE.put("ctrl.action", "AKTION");
        EN.put("ctrl.action", "ACTION");
        DE.put("ctrl.action.tt_title", "Bridge-Aktivierung");
        EN.put("ctrl.action.tt_title", "Bridge Activation");
        DE.put("ctrl.action.tt_desc", "Startet oder stoppt den DMX-Ausgabedienst:\n\n• Start: Verbindet mit DMX-Adapter und leitet Art-Net live weiter\n• Stop: Hält DMX-Ausgabe an und trennt die Schnittstelle sicher");
        EN.put("ctrl.action.tt_desc", "Starts or stops the DMX output service:\n\n• Start: Connects to DMX adapter and forwards Art-Net live\n• Stop: Halts DMX output and safely disconnects interface");

        // --- Visualizer ---
        DE.put("vis.title", "DMX512 KANÄLE (1 - 512)");
        EN.put("vis.title", "DMX512 CHANNELS (1 - 512)");
        DE.put("vis.range", "Bereich:");
        EN.put("vis.range", "Range:");

        // --- Statusleiste ---
        DE.put("status.ready_hint", "Bereit. Klicken Sie auf Start.");
        EN.put("status.ready_hint", "Ready. Click Start.");
        DE.put("status.bridge_stopped", "Bridge gestoppt.");
        EN.put("status.bridge_stopped", "Bridge stopped.");
        DE.put("status.listening", "Lauscht auf UDP %d | Universum: %s | Ausgabe: %s");
        EN.put("status.listening", "Listening on UDP %d | Universe: %s | Output: %s");
        DE.put("status.waiting", "Warte auf Art-Net Pakete auf Port %d (Filter: Universum %s) -> DMX: %s");
        EN.put("status.waiting", "Waiting for Art-Net packets on port %d (Filter: Universe %s) -> DMX: %s");
        DE.put("status.active_live", "Art-Net aktiv (%.1f pkt/s) | DMX: %s (%.1f fps, %,d Frames)");
        EN.put("status.active_live", "Art-Net active (%.1f pkt/s) | DMX: %s (%.1f fps, %,d Frames)");
        DE.put("status.no_ports", "Keine seriellen Anschlüsse gefunden.");
        EN.put("status.no_ports", "No serial ports found.");
        DE.put("status.port_selected", "Adapter gewählt: %s (%s)");
        EN.put("status.port_selected", "Adapter selected: %s (%s)");
        DE.put("status.error_start", "Fehler beim Start: %s");
        EN.put("status.error_start", "Error starting: %s");
        DE.put("status.all_universes", "Alle");
        EN.put("status.all_universes", "All");
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

    public static String get(String key, Object... args) {
        String val = get(key);
        if (args != null && args.length > 0) {
            try {
                return String.format(val, args);
            } catch (Exception e) {
                return val;
            }
        }
        return val;
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
