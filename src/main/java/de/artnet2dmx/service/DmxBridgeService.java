package de.artnet2dmx.service;

import de.artnet2dmx.artnet.ArtNetReceiver;
import de.artnet2dmx.config.AppConfig;
import de.artnet2dmx.dmx.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * DmxBridgeService.
 * Zentrale Orchestrierungs- und Service-Klasse der Art-Net zu DMX Bridge.
 * Entkoppelt die Benutzeroberfläche (JavaFX) vollständig von der Verarbeitungslogik.
 * Basiert auf der Architektur des Python-Vorbilds old_artnet2dmx/main.py.
 */
public class DmxBridgeService {
    private static final Logger LOGGER = Logger.getLogger(DmxBridgeService.class.getName());

    private final AtomicBoolean running = new AtomicBoolean(false);

    private DmxSender sender;
    private DmxOutputManager outputManager;
    private ArtNetReceiver receiver;

    public synchronized void start(AppConfig config) throws Exception {
        if (running.get()) {
            return;
        }

        String driverName = (config.getDriver() != null) ? config.getDriver().trim().toLowerCase() : "pyftdi";
        String port = config.getPort();

        // 1. DMX-Sender instanziieren
        sender = switch (driverName) {
            case "jna-ftdi", "jna", "libusb", "native" -> {
                LOGGER.info("Wähle nativen JNA FTDI DMX-Treiber (libusb-1.0)");
                yield new FtdiJnaDmxSender();
            }
            case "dummy" -> new DummySender();
            case "enttec-pro" -> new EnttecProSender(port);
            case "serial" -> new SerialOpenDmxSender(port);
            default -> {
                LOGGER.info("Wähle pyftdi DMX-Hardwaretreiber (JMS USB2DMX PRO / FT232R)");
                yield new PyFtdiDmxSender(port);
            }
        };

        // 2. DMX Output Manager (Puffer & kontinuierlicher Hardware-Takt)
        outputManager = new DmxOutputManager(sender, config.getFps());

        // 3. Art-Net UDP Empfänger
        receiver = new ArtNetReceiver(
            config.getBind(),
            config.getUdpPort(),
            config.getUniverse(),
            config.getSubnet(),
            config.getNet(),
            data -> {
                if (outputManager != null) {
                    outputManager.updateChannels(data);
                }
            }
        );

        outputManager.start();
        receiver.start();

        running.set(true);
        LOGGER.info(String.format(
            "DmxBridgeService erfolgreich gestartet! Treiber=%s, Port=%s, Universum=%s, FPS=%d, Bind=%s:%d",
            sender.getName(), port != null ? port : "Auto-Detect",
            config.getUniverse() == -1 ? "Alle" : config.getUniverse(),
            config.getFps(), config.getBind(), config.getUdpPort()
        ));
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);

        LOGGER.info("Halte DmxBridgeService an...");

        if (receiver != null) {
            try {
                receiver.stop();
            } catch (Exception ignored) {}
            receiver = null;
        }

        if (outputManager != null) {
            try {
                outputManager.stop();
            } catch (Exception ignored) {}
            outputManager = null;
        }

        sender = null;
        LOGGER.info("DmxBridgeService gestoppt.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public double getActualFps() {
        return (outputManager != null) ? outputManager.getActualFps() : 0.0;
    }

    public double getActualPps() {
        return (receiver != null) ? receiver.getActualPps() : 0.0;
    }

    public long getPacketsTargetUniverse() {
        return (receiver != null) ? receiver.getPacketsTargetUniverse() : 0;
    }

    public long getFramesSent() {
        return (outputManager != null) ? outputManager.getFramesSent() : 0;
    }

    public int[] getChannelSummary(int count) {
        return (outputManager != null) ? outputManager.getChannelSummary(count) : new int[count];
    }

    public int[] getAllChannels() {
        return (outputManager != null) ? outputManager.getAllChannels() : new int[512];
    }

    public String getSenderName() {
        return (sender != null) ? sender.getName() : "Keiner";
    }
}
