package de.artnet2dmx.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static File getConfigFile() {
        return new File(CONFIG_FILE_NAME);
    }

    public static AppConfig loadConfig() {
        File file = getConfigFile();
        AppConfig config = new AppConfig();
        if (file.exists()) {
            try {
                config = MAPPER.readValue(file, AppConfig.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Lesen von config.json: " + e.getMessage());
            }
        }
        // HARDWARE-SCHUTZ: Echten Modus erzwingen
        config.setDummy(false);
        return config;
    }

    public static void saveConfig(AppConfig config) {
        try {
            config.setDummy(false); // Niemals Dummy-Modus speichern
            MAPPER.writeValue(getConfigFile(), config);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Speichern von config.json: " + e.getMessage());
        }
    }
}
