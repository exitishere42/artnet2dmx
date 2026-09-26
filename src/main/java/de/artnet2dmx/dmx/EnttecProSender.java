package de.artnet2dmx.dmx;

import com.fazecast.jSerialComm.SerialPort;

import java.util.logging.Logger;

/**
 * Enttec DMX USB Pro Protokoll-Treiber (SOM 0x7E ... EOM 0xE7).
 */
public class EnttecProSender implements DmxSender {
    private static final Logger LOGGER = Logger.getLogger(EnttecProSender.class.getName());

    private static final byte SOM = 0x7E;
    private static final byte EOM = (byte) 0xE7;
    private static final byte LABEL_OUTPUT_DMX = 6;

    private final String portName;
    private SerialPort serialPort;

    public EnttecProSender(String portName) {
        this.portName = portName;
    }

    @Override
    public void open() throws Exception {
        if (isOpen()) {
            return;
        }

        String targetPort = (portName == null || portName.isBlank() || portName.equalsIgnoreCase("Auto-Detect"))
                ? SerialOpenDmxSender.listAvailablePorts().stream().map(SerialOpenDmxSender.PortInfo::systemPortName).findFirst().orElse("/dev/ttyUSB0")
                : portName;

        serialPort = SerialPort.getCommPort(targetPort);
        serialPort.setBaudRate(57_600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Konnte Enttec Pro Port " + targetPort + " nicht öffnen.");
        }

        LOGGER.info(() -> "Enttec Pro Sender geöffnet auf " + serialPort.getSystemPortName());
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
            throw new IllegalStateException("Port nicht geöffnet.");
        }

        int dataLen = Math.min(dmxData.length, 512);
        int payloadLen = dataLen + 1; // +1 für Start Code 0x00

        byte[] packet = new byte[5 + payloadLen];
        packet[0] = SOM;
        packet[1] = LABEL_OUTPUT_DMX;
        packet[2] = (byte) (payloadLen & 0xFF);
        packet[3] = (byte) ((payloadLen >> 8) & 0xFF);
        packet[4] = 0x00; // DMX Start Code
        System.arraycopy(dmxData, 0, packet, 5, dataLen);
        packet[packet.length - 1] = EOM;

        serialPort.writeBytes(packet, packet.length);
    }

    @Override
    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    @Override
    public String getName() {
        return "Enttec Pro (" + (serialPort != null ? serialPort.getSystemPortName() : portName) + ")";
    }
}
