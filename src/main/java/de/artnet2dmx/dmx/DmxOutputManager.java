package de.artnet2dmx.dmx;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DmxOutputManager.
 * Portierung von old_artnet2dmx/dmx_sender.py (Klasse DmxOutputManager) nach Java 21.
 * Verwaltet den kontinuierlichen 512-Kanal Frame-Puffer und sendet DMX512-Frames
 * mit stabiler Bildwiederholrate (z. B. 35 oder 44 FPS) an das Hardware-Interface.
 * Verhindert Scheinwerfer-Blackouts bei Signalpausen im Netzwerk.
 */
public class DmxOutputManager {
    private static final Logger LOGGER = Logger.getLogger(DmxOutputManager.class.getName());

    public static final int TOTAL_CHANNELS = 512;

    private final DmxSender sender;
    private final int targetFps;
    private final long frameIntervalMillis;

    private final byte[] dmxBuffer = new byte[TOTAL_CHANNELS];
    private final Object bufferLock = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    private volatile double actualFps = 0.0;
    private long framesSent = 0;
    private long lastFpsCalcTime = System.currentTimeMillis();
    private long lastFramesCount = 0;
    private long lastHardwareLogTime = 0;
    private volatile String lastError = null;

    public DmxOutputManager(DmxSender sender, int targetFps) {
        this.sender = sender;
        this.targetFps = Math.max(5, Math.min(targetFps, 44));
        this.frameIntervalMillis = Math.max(1, 1000 / this.targetFps);
    }

    public synchronized void start() throws Exception {
        if (running.get()) {
            return;
        }

        sender.open();
        running.set(true);

        workerThread = new Thread(this::senderLoop, "DmxSenderThread");
        workerThread.setDaemon(true);
        workerThread.start();
        LOGGER.info(String.format("DMX Output Manager gestartet (%d FPS Zielrate an %s).", targetFps, sender.getName()));
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }

        try {
            sender.close();
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Schließen des Senders: " + e.getMessage());
        }
        LOGGER.info("DMX Output Manager gestoppt.");
    }

    public void updateChannels(byte[] data) {
        if (data == null) {
            return;
        }
        synchronized (bufferLock) {
            int len = Math.min(data.length, TOTAL_CHANNELS);
            System.arraycopy(data, 0, dmxBuffer, 0, len);
        }
    }

    public int[] getChannelSummary(int count) {
        int[] result = new int[count];
        synchronized (bufferLock) {
            for (int i = 0; i < count && i < TOTAL_CHANNELS; i++) {
                result[i] = dmxBuffer[i] & 0xFF;
            }
        }
        return result;
    }

    public int[] getAllChannels() {
        int[] result = new int[TOTAL_CHANNELS];
        synchronized (bufferLock) {
            for (int i = 0; i < TOTAL_CHANNELS; i++) {
                result[i] = dmxBuffer[i] & 0xFF;
            }
        }
        return result;
    }

    private void senderLoop() {
        byte[] frameToSend = new byte[TOTAL_CHANNELS];

        while (running.get()) {
            long startTime = System.currentTimeMillis();

            synchronized (bufferLock) {
                System.arraycopy(dmxBuffer, 0, frameToSend, 0, TOTAL_CHANNELS);
            }

            try {
                sender.sendFrame(frameToSend);
                framesSent++;
                lastError = null;
            } catch (Exception e) {
                if (!running.get()) {
                    break;
                }
                lastError = e.getMessage();
                LOGGER.log(Level.SEVERE, "Fehler beim Senden des DMX-Frames an " + sender.getName() + ": " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }

            // FPS-Berechnung alle 500 ms
            long now = System.currentTimeMillis();
            long dt = now - lastFpsCalcTime;
            if (dt >= 500) {
                long sentDelta = framesSent - lastFramesCount;
                actualFps = (sentDelta * 1000.0) / dt;
                lastFpsCalcTime = now;
                lastFramesCount = framesSent;
            }

            // Periodischer Hardware-Log alle 4 Sekunden
            if (now - lastHardwareLogTime >= 4000) {
                lastHardwareLogTime = now;
                int nonZero = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < TOTAL_CHANNELS; i++) {
                    int val = frameToSend[i] & 0xFF;
                    if (val > 0) {
                        nonZero++;
                        if (nonZero <= 8) {
                            if (!sb.isEmpty()) sb.append(", ");
                            sb.append("Ch ").append(i + 1).append("=").append(val);
                        }
                    }
                }
                if (nonZero > 8) {
                    sb.append(", ... (+").append(nonZero - 8).append(" weitere)");
                }

                LOGGER.info(String.format(
                    "[DMX-OUT Hardware] %s | %.1f FPS (Ziel: %d) | Gesendet: %,d Frames | Aktive Kanäle: %d/512 %s",
                    sender.getName(), actualFps, targetFps, framesSent, nonZero,
                    nonZero > 0 ? ("[" + sb + "]") : "[ALLE 512 KANÄLE SIND 0]"
                ));

                if (nonZero == 0) {
                    LOGGER.warning("[DMX-OUT HINWEIS] Ausgabe an Hardware läuft, aber alle DMX-Werte sind 0! Moving Heads bleiben dunkel.");
                }
            }

            // Restliche Zeit bis zum nächsten Frame pausieren
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = frameIntervalMillis - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public double getActualFps() {
        return actualFps;
    }

    public long getFramesSent() {
        return framesSent;
    }

    public String getLastError() {
        return lastError;
    }

    public DmxSender getSender() {
        return sender;
    }
}
