package de.artnet2dmx.dmx;

import de.exit.jusb2dmx.Ftdi232Device;

import java.util.logging.Logger;

/**
 * FtdiJnaDmxSender.
 * DMX-Sender, der den gekapselten Ftdi232Device JNA-Treiber nutzt.
 * Ermöglicht den 100% nativen Java-Betrieb ohne Python-Subprozess.
 */
public class FtdiJnaDmxSender implements DmxSender {
    private static final Logger LOGGER = Logger.getLogger(FtdiJnaDmxSender.class.getName());

    private Ftdi232Device device;

    @Override
    public synchronized void open() throws Exception {
        if (isOpen()) {
            return;
        }
        LOGGER.info("Öffne nativen JNA FTDI DMX-Treiber (libusb-1.0)...");
        device = new Ftdi232Device();
        device.open();
        LOGGER.info("Nativer JNA FTDI DMX-Treiber erfolgreich geöffnet.");
    }

    @Override
    public synchronized void close() {
        if (device != null) {
            device.close();
            device = null;
        }
        LOGGER.info("Nativer JNA FTDI DMX-Treiber geschlossen.");
    }

    @Override
    public void sendFrame(byte[] dmxData) throws Exception {
        if (device == null || !device.isOpen()) {
            throw new IllegalStateException("FtdiJnaDmxSender ist nicht geöffnet.");
        }
        device.sendDmxFrame(dmxData);
    }

    @Override
    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    @Override
    public String getName() {
        return "jna-ftdi (Nativer libusb FTDI-Treiber)";
    }
}
