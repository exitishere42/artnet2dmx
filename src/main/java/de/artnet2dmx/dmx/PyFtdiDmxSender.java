package de.artnet2dmx.dmx;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * PyFtdiDmxSender.
 * Bindet den bewährten pyftdi-Treiber aus old_artnet2dmx direkt an Java 21 an.
 * Nutzt einen leichtgewichtigen Python-Subprozess, der über stdin mit 512-Byte DMX-Frames
 * versorgt wird und die präzise Hardware-Break- (~180 µs) und MAB- (~30 µs) Generierung
 * mittels time.perf_counter() auf dem FTDI FT232R Chip (JMS USB2DMX PRO) durchführt.
 */
public class PyFtdiDmxSender implements DmxSender {
    private static final Logger LOGGER = Logger.getLogger(PyFtdiDmxSender.class.getName());

    private final String url;
    private Process workerProcess;
    private OutputStream workerIn;
    private Thread errorConsumerThread;
    private volatile boolean running = false;

    public PyFtdiDmxSender(String url) {
        this.url = (url == null || url.isBlank() || url.equalsIgnoreCase("Auto-Detect") || url.contains("tty"))
                ? "ftdi://ftdi:232/1" : url.trim();
    }

    private static String findPythonExecutable() {
        String[] candidates = {
            "/home/regie/old_artnet2dmx/.venv/bin/python",
            "/home/regie/artnet2dmx/.venv/bin/python",
            "python3",
            "python"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                return c;
            }
        }
        return "python3";
    }

    private static File getWorkerScript() throws IOException {
        File local = new File("pyftdi_worker.py");
        if (local.exists()) {
            return local;
        }
        try (InputStream in = PyFtdiDmxSender.class.getResourceAsStream("/scripts/pyftdi_worker.py")) {
            if (in != null) {
                File temp = File.createTempFile("pyftdi_worker_", ".py");
                temp.deleteOnExit();
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                temp.setExecutable(true);
                return temp;
            }
        }
        return local;
    }

    @Override
    public synchronized void open() throws Exception {
        if (isOpen()) {
            return;
        }

        String pythonExe = findPythonExecutable();
        File workerScript = getWorkerScript();

        LOGGER.info(String.format("Starte pyftdi DMX-Treiber mit %s %s (%s)", pythonExe, workerScript.getAbsolutePath(), url));

        ProcessBuilder pb = new ProcessBuilder(pythonExe, workerScript.getAbsolutePath(), url);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        workerProcess = pb.start();
        workerIn = new BufferedOutputStream(workerProcess.getOutputStream(), 1024);

        // Asynchrones Warten auf das READY-Signal
        BufferedReader reader = new BufferedReader(new InputStreamReader(workerProcess.getErrorStream()));
        BlockingQueue<String> lineQueue = new LinkedBlockingQueue<>();

        errorConsumerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineQueue.offer(line);
                    LOGGER.info("[pyftdi] " + line);
                }
            } catch (IOException ignored) {}
        }, "pyftdi-log-thread");
        errorConsumerThread.setDaemon(true);
        errorConsumerThread.start();

        boolean ready = false;
        long deadline = System.currentTimeMillis() + 8000;

        while (System.currentTimeMillis() < deadline) {
            String line = lineQueue.poll(100, TimeUnit.MILLISECONDS);
            if (line != null) {
                if (line.contains("READY")) {
                    ready = true;
                    break;
                }
                if (line.contains("FEHLER") || line.contains("Error")) {
                    throw new IllegalStateException("pyftdi Initialisierungsfehler: " + line);
                }
            }

            if (!workerProcess.isAlive()) {
                int exit = workerProcess.exitValue();
                throw new IllegalStateException("pyftdi Worker vorzeitig beendet (Exit " + exit + "). Bitte Prüfen ob USB-Adapter angeschlossen ist.");
            }
        }

        if (!ready) {
            close();
            throw new TimeoutException("Zeitüberschreitung beim Warten auf pyftdi-Initialisierung.");
        }

        running = true;
        LOGGER.info("pyftdi DMX-Hardwaretreiber erfolgreich geöffnet und betriebsbereit!");
    }

    @Override
    public synchronized void close() {
        running = false;
        if (workerIn != null) {
            try {
                workerIn.close();
            } catch (Exception ignored) {}
            workerIn = null;
        }
        if (workerProcess != null) {
            try {
                if (!workerProcess.waitFor(1500, TimeUnit.MILLISECONDS)) {
                    workerProcess.destroyForcibly();
                }
            } catch (Exception ignored) {}
            workerProcess = null;
        }
        LOGGER.info("pyftdi DMX-Hardwaretreiber geschlossen.");
    }

    @Override
    public void sendFrame(byte[] dmxData) throws Exception {
        if (!isOpen() || workerIn == null) {
            throw new IllegalStateException("pyftdi Worker ist nicht geöffnet.");
        }

        byte[] frame = new byte[512];
        if (dmxData != null) {
            System.arraycopy(dmxData, 0, frame, 0, Math.min(dmxData.length, 512));
        }

        workerIn.write(frame);
        workerIn.flush();
    }

    @Override
    public boolean isOpen() {
        return running && workerProcess != null && workerProcess.isAlive();
    }

    @Override
    public String getName() {
        return "pyftdi (FTDI FT232R USB Direct)";
    }
}
