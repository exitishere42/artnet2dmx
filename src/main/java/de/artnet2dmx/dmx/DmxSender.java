package de.artnet2dmx.dmx;

/**
 * Schnittstelle für DMX512-Hardware-Sender.
 */
public interface DmxSender extends AutoCloseable {
    void open() throws Exception;
    void close();
    void sendFrame(byte[] dmxData) throws Exception;
    boolean isOpen();
    String getName();
}
