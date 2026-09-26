package de.artnet2dmx;

import de.artnet2dmx.config.AppConfig;
import de.artnet2dmx.config.ConfigManager;
import de.artnet2dmx.service.DmxBridgeService;

import java.util.logging.Logger;

/**
 * Headless CLI Runner für Server-, Systemd- und SSH-Betrieb ohne X11-Display.
 * Ermöglicht den Betrieb der Bridge mit identischer Logik wie in old_artnet2dmx/main.py.
 */
public class HeadlessRunner {
    private static final Logger LOGGER = Logger.getLogger(HeadlessRunner.class.getName());

    public static void run(String[] args) {
        AppConfig config = ConfigManager.loadConfig();

        // CLI Argumente parsen
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--driver") && i + 1 < args.length) {
                config.setDriver(args[++i]);
            } else if (arg.equals("--port") && i + 1 < args.length) {
                config.setPort(args[++i]);
            } else if (arg.equals("--universe") && i + 1 < args.length) {
                config.setUniverse(Integer.parseInt(args[++i]));
            } else if (arg.equals("--fps") && i + 1 < args.length) {
                config.setFps(Integer.parseInt(args[++i]));
            } else if (arg.equals("--bind") && i + 1 < args.length) {
                config.setBind(args[++i]);
            } else if (arg.equals("--udp-port") && i + 1 < args.length) {
                config.setUdpPort(Integer.parseInt(args[++i]));
            }
        }

        System.out.println("==================================================================");
        System.out.println("     artnet2dmx - Art-Net zu DMX512 (Headless CLI Modus)");
        System.out.println("==================================================================");
        System.out.println("  Treiber:         " + config.getDriver());
        System.out.println("  Port / Adapter:  " + (config.getPort() != null ? config.getPort() : "Auto-Detect"));
        System.out.println("  Art-Net Ziel:    Net=" + config.getNet() + ", Subnet=" + config.getSubnet() + ", Universe=" + (config.getUniverse() == -1 ? "Alle (-1)" : config.getUniverse()));
        System.out.println("  UDP-Bind:        " + config.getBind() + ":" + config.getUdpPort());
        System.out.println("  DMX Ziel-FPS:    " + config.getFps() + " Hz");
        System.out.println("==================================================================");

        DmxBridgeService bridgeService = new DmxBridgeService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nBeende artnet2dmx Bridge...");
            bridgeService.stop();
            System.out.println("Auf Wiedersehen!");
        }));

        try {
            bridgeService.start(config);
            System.out.println("[✓] DMX-Bridge erfolgreich gestartet. Drücken Sie Strg+C zum Beenden.\n");

            long lastTime = System.currentTimeMillis();
            while (bridgeService.isRunning()) {
                Thread.sleep(1000);
                long now = System.currentTimeMillis();
                double dt = (now - lastTime) / 1000.0;
                lastTime = now;

                double pps = bridgeService.getActualPps();
                double fps = bridgeService.getActualFps();
                long pkts = bridgeService.getPacketsTargetUniverse();
                int[] channels = bridgeService.getChannelSummary(6);

                StringBuilder chStr = new StringBuilder();
                for (int c = 0; c < 6; c++) {
                    int val = channels[c];
                    chStr.append(String.format(" Ch%d:%3d", c + 1, val));
                }

                System.out.printf("\r[Art-Net] %5.1f Pkt/s (%,6d) │ [DMX] %4.1f FPS │%s",
                        pps, pkts, fps, chStr.toString());
                System.out.flush();
            }
        } catch (Exception e) {
            System.err.println("\n[!] FEHLER beim Starten der Bridge: " + e.getMessage());
            e.printStackTrace();
            bridgeService.stop();
            System.exit(1);
        }
    }
}
