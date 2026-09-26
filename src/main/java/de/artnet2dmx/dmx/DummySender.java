package de.artnet2dmx.dmx;

/**
 * Dummy Sender für Simulations- und Testzwecke.
 */
public class DummySender implements DmxSender {
    private boolean open = false;

    @Override
    public void open() {
        open = true;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void sendFrame(byte[] dmxData) throws Exception {
        Thread.sleep(20);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getName() {
        return "Dummy (Simulation)";
    }
}
