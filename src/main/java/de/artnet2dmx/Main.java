package de.artnet2dmx;

/**
 * Standard Java Einstiegspunkt für die Fat JAR Ausführung.
 * Startet standardmäßig die JavaFX GUI, oder wechselt automatisch in den CLI/Headless-Modus,
 * falls kein X11/Wayland-Display vorhanden ist oder --cli übergeben wurde.
 */
public class Main {
    public static void main(String[] args) {
        boolean cliMode = false;
        for (String arg : args) {
            if (arg.equals("--cli") || arg.equals("--headless") || arg.equals("-c")) {
                cliMode = true;
                break;
            }
        }

        // Automatischer Wechsel in den Headless CLI-Modus unter Linux ohne Display
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux") && System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            cliMode = true;
        }

        if (cliMode) {
            HeadlessRunner.run(args);
        } else {
            ArtNet2DmxApp.main(args);
        }
    }
}
