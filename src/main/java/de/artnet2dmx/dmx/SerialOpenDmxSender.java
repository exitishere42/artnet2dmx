package de.artnet2dmx.dmx;

import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Open-DMX Treiber über serielle Schnittstelle (COM / /dev/ttyUSB) mit jSerialComm.
 * Unterstützt JMS USB2DMX Pro, Enttec Open DMX und FTDI-kompatible Adapter.
 * Erzeugt Break (~180 µs), Mark After Break (~30 µs), Start-Code 0x00 bei 250.000 Baud 8N2.
 */
public class SerialOpenDmxSender implements DmxSender {
    private static final Logger LOGGER = Logger.getLogger(SerialOpenDmxSender.class.getName());

    private final String portName;
    private final long breakNanos;
    private final long mabNanos;

    private SerialPort serialPort;

    public SerialOpenDmxSender(String portName) {
        this(portName, 180_000L, 30_000L); // 180 µs Break, 30 µs MAB
    }

    public SerialOpenDmxSender(String portName, long breakNanos, long mabNanos) {
        this.portName = portName;
        this.breakNanos = breakNanos;
        this.mabNanos = mabNanos;
    }

    @Override
    public void open() throws Exception {
        if (isOpen()) {
            return;
        }

        String targetPort = portName;
        if (targetPort == null || targetPort.isBlank() || targetPort.equalsIgnoreCase("Auto-Detect")) {
            List<PortInfo> ports = listAvailablePorts();
            if (ports.isEmpty()) {
                throw new IllegalStateException("Kein USB-DMX-Adapter gefunden. Bitte USB-Kabel einstecken oder 'dummy' Treiber wählen.");
            }
            // Bevorzuge DMX-Adapter
            targetPort = ports.stream()
                    .filter(PortInfo::isLikelyDmx)
                    .findFirst()
                    .map(PortInfo::systemPortName)
                    .orElse(ports.get(0).systemPortName());
        }

        serialPort = SerialPort.getCommPort(targetPort);
        serialPort.setBaudRate(250_000);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.TWO_STOP_BITS);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Konnte seriellen Port " + targetPort + " nicht öffnen (Zugriff verweigert oder belegt).");
        }

        // RS-485 Transceiver Driver Enable (DE/RE via RTS / DTR) für JMS USB2DMX & Open-DMX
        serialPort.setRTS();
        serialPort.setDTR();

        LOGGER.info(() -> "DMX Serial Sender geöffnet auf " + serialPort.getSystemPortName() + " (250.000 Baud, 8N2, RTS/DTR aktiv)");
    }

    @Override
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.closePort();
            } catch (Exception ignored) {
            }
        }
        serialPort = null;
    }

    @Override
    public void sendFrame(byte[] dmxData) throws Exception {
        if (!isOpen()) {
            throw new IllegalStateException("Serieller Port ist nicht geöffnet.");
        }

        // 1. DMX512 Break (~180 µs TX Space/Low)
        serialPort.setBreak();
        busySleep(breakNanos);

        // 2. Mark After Break (~30 µs TX Mark/High)
        serialPort.clearBreak();
        busySleep(mabNanos);

        // 3. Start-Code 0x00 + bis zu 512 DMX-Kanalwerte
        int len = Math.min(dmxData.length, 512);
        byte[] frame = new byte[1 + len];
        frame[0] = 0x00; // DMX512 Start Code
        System.arraycopy(dmxData, 0, frame, 1, len);

        serialPort.writeBytes(frame, frame.length);
    }

    @Override
    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    @Override
    public String getName() {
        return "Serial Open-DMX (" + (serialPort != null ? serialPort.getSystemPortName() : portName) + ")";
    }

    private static void busySleep(long nanos) {
        long target = System.nanoTime() + nanos;
        while (System.nanoTime() < target) {
            Thread.onSpinWait();
        }
    }

    public record PortInfo(String systemPortName, String descriptivePortName, boolean isLikelyDmx) {}

    public static List<PortInfo> listAvailablePorts() {
        List<PortInfo> list = new ArrayList<>();
        for (SerialPort sp : SerialPort.getCommPorts()) {
            String name = sp.getSystemPortName();
            if (name.matches("ttyS\\d+")) {
                continue; // Onboard-Header überspringen
            }
            String desc = sp.getDescriptivePortName().toLowerCase();
            boolean isDmx = desc.contains("dmx") || desc.contains("ftdi") ||
                            desc.contains("usb serial") || desc.contains("ft232") ||
                            name.toLowerCase().contains("ttyusb");
            list.add(new PortInfo(name, sp.getDescriptivePortName(), isDmx));
        }
        return list;
    }
}
