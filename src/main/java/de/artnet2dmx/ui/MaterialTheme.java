package de.artnet2dmx.ui;

import javafx.scene.paint.Color;

/**
 * Material Design 2 Dark Theme Farbpalette & Style Constants.
 * Referenz: https://m2.material.io/design/color/dark-theme.html#anatomy
 */
public final class MaterialTheme {
    private MaterialTheme() {}

    // Elevation Surfaces
    public static final String HEX_BG = "#121212";               // 0dp Baseline
    public static final String HEX_SURFACE_1DP = "#1E1E1E";      // 1dp Card / Sheet
    public static final String HEX_SURFACE_2DP = "#232323";      // 2dp App Bar / Input Box
    public static final String HEX_SURFACE_4DP = "#272727";      // 4dp Chip / Tooltip / Container
    public static final String HEX_SURFACE_8DP = "#2E2E2E";      // 8dp Elevated / Modal

    // Akzente & Semantik
    public static final String HEX_PRIMARY = "#03DAC6";          // Teal 200: Desaturated Primary
    public static final String HEX_PRIMARY_VARIANT = "#018786";  // Teal 700
    public static final String HEX_ERROR = "#CF6679";            // Error 200
    public static final String HEX_ON_PRIMARY = "#000000";
    public static final String HEX_ON_ERROR = "#000000";

    // Text & Divider
    public static final String HEX_TEXT_HIGH = "#E1E1E1";        // High emphasis (87%)
    public static final String HEX_TEXT_MED = "#9E9E9E";         // Medium emphasis (60%)
    public static final String HEX_TEXT_DISABLED = "#616161";    // Disabled (38%)
    public static final String HEX_DIVIDER = "#2C2C2C";          // Outline / Trennlinie (12%)

    // JavaFX Paint Colors
    public static final Color COLOR_BG = Color.web(HEX_BG);
    public static final Color COLOR_SURFACE_1DP = Color.web(HEX_SURFACE_1DP);
    public static final Color COLOR_SURFACE_2DP = Color.web(HEX_SURFACE_2DP);
    public static final Color COLOR_SURFACE_4DP = Color.web(HEX_SURFACE_4DP);
    public static final Color COLOR_SURFACE_8DP = Color.web(HEX_SURFACE_8DP);

    public static final Color COLOR_PRIMARY = Color.web(HEX_PRIMARY);
    public static final Color COLOR_PRIMARY_VARIANT = Color.web(HEX_PRIMARY_VARIANT);
    public static final Color COLOR_ERROR = Color.web(HEX_ERROR);
    public static final Color COLOR_ON_PRIMARY = Color.web(HEX_ON_PRIMARY);
    public static final Color COLOR_ON_ERROR = Color.web(HEX_ON_ERROR);

    public static final Color COLOR_TEXT_HIGH = Color.web(HEX_TEXT_HIGH);
    public static final Color COLOR_TEXT_MED = Color.web(HEX_TEXT_MED);
    public static final Color COLOR_TEXT_DISABLED = Color.web(HEX_TEXT_DISABLED);
    public static final Color COLOR_DIVIDER = Color.web(HEX_DIVIDER);

    public static final String FONT_FAMILY = "Segoe UI, -apple-system, BlinkMacSystemFont, 'Ubuntu', sans-serif";
    public static final String FONT_MONO = "'DejaVu Sans Mono', 'Consolas', monospace";
}
