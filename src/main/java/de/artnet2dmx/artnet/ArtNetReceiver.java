package de.artnet2dmx.artnet;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Art-Net Receiver Modul.
 * Portierung von old_artnet2dmx/artnet_receiver.py nach modernem Java 21.
 * Empfängt Art-Net-Daten (UDP Port 6454), extrahiert DMX512-Werte und beantwortet ArtPoll-Anfragen
 * mit ArtPollReply zur automatischen Node-Erkennung durch Lichtpulte (QLC+, GrandMA, dot2 etc.).
 */
public class ArtNetReceiver {
    private static final Logger LOGGER = Logger.getLogger(ArtNetReceiver.class.getName());

    public static final int DEFAULT_PORT = 6454;

    private final String bindIp;
    private final int port;
    private final int targetUniverse;
    private final int targetSubnet;
    private final int targetNet;
    private final int targetPortAddress;
    private final boolean acceptAnyUniverse;
    private final Consumer<byte[]> onDmxFrame;

    private final String nodeShortName;
    private final String nodeLongName;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong packetsTotal = new AtomicLong(0);
    private final AtomicLong packetsTargetUniverse = new AtomicLong(0);

    private volatile double actualPps = 0.0;
    private long lastPpsTime = System.currentTimeMillis();
    private long lastPacketsCount = 0;
    private long lastDataLogTime = 0;
    private long lastMismatchLogTime = 0;

    private DatagramSocket socket;
    private Thread receiverThread;

    public ArtNetReceiver(
        String bindIp,
        int port,
        int targetUniverse,
        int targetSubnet,
        int targetNet,
        Consumer<byte[]> onDmxFrame
    ) {
        this(bindIp, port, targetUniverse, targetSubnet, targetNet, onDmxFrame, "ArtNet-USB2DMX", "Art-Net to JMS USB2DMX Pro Bridge");
    }

    public ArtNetReceiver(
        String bindIp,
        int port,
        int targetUniverse,
        int targetSubnet,
        int targetNet,
        Consumer<byte[]> onDmxFrame,
        String nodeShortName,
        String nodeLongName
    ) {
        this.bindIp = (bindIp == null || bindIp.isBlank()) ? "0.0.0.0" : bindIp.trim();
        this.port = port <= 0 ? DEFAULT_PORT : port;
        this.targetUniverse = targetUniverse;
        this.targetSubnet = targetSubnet & 0x0F;
        this.targetNet = targetNet & 0x7F;
        this.acceptAnyUniverse = (targetUniverse < 0);
        this.targetPortAddress = acceptAnyUniverse ? -1 : ((this.targetNet << 8) | (this.targetSubnet << 4) | (this.targetUniverse & 0x0F));
        this.onDmxFrame = onDmxFrame;
        this.nodeShortName = nodeShortName;
        this.nodeLongName = nodeLongName;
    }

    public synchronized void start() throws SocketException {
        if (running.get()) {
            return;
        }

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.setBroadcast(true);
        socket.setReceiveBufferSize(64 * 1024);
        socket.bind(new InetSocketAddress(bindIp, port));

        running.set(true);
        receiverThread = new Thread(this::receiveLoop, "ArtNetReceiverThread");
        receiverThread.setDaemon(true);
        receiverThread.start();

        LOGGER.info(String.format(
            "Art-Net Empfänger lauscht auf %s:%d (Ziel: Net=%d, SubNet=%d, Universe=%s | 15-Bit Addr=0x%04X)",
            bindIp, port, targetNet, targetSubnet,
            acceptAnyUniverse ? "Alle (-1)" : String.valueOf(targetUniverse),
            targetPortAddress & 0xFFFF
        ));
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (receiverThread != null) {
            receiverThread.interrupt();
            try {
                receiverThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            receiverThread = null;
        }

        LOGGER.info("Art-Net Empfänger gestoppt.");
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running.get() && socket != null && !socket.isClosed()) {
            try {
                socket.receive(packet);
                packetsTotal.incrementAndGet();

                ArtNetPacket artPacket = ArtNetPacket.parse(packet.getData(), packet.getLength());
                if (artPacket == null) {
                    continue;
                }

                if (artPacket.isDmx()) {
                    handleArtDmx(artPacket, packet.getAddress(), packet.getPort());
                } else if (artPacket.isPoll()) {
                    handleArtPoll(packet.getAddress(), packet.getPort());
                }

                // PPS-Berechnung
                long now = System.currentTimeMillis();
                long dt = now - lastPpsTime;
                if (dt >= 500) {
                    long delta = packetsTargetUniverse.get() - lastPacketsCount;
                    actualPps = (delta * 1000.0) / dt;
                    lastPpsTime = now;
                    lastPacketsCount = packetsTargetUniverse.get();
                }

            } catch (SocketException e) {
                if (!running.get()) {
                    break;
                }
                LOGGER.log(Level.FINE, "Socket geschlossen im Empfangs-Loop.");
            } catch (Exception e) {
                if (running.get()) {
                    LOGGER.log(Level.WARNING, "Fehler beim Empfang von Art-Net Daten: " + e.getMessage());
                }
            }
        }
    }

    private void handleArtDmx(ArtNetPacket artPacket, InetAddress senderAddress, int senderPort) {
        boolean match = acceptAnyUniverse || (artPacket.portAddress() == targetPortAddress);

        if (!match) {
            long now = System.currentTimeMillis();
            if (now - lastMismatchLogTime >= 3000) {
                lastMismatchLogTime = now;
                LOGGER.warning(String.format(
                    "Art-Net Paket für anderes Universum empfangen! Empfangen: Net=%d, SubNet=%d, Universe=%d | Konfiguriert: Net=%d, SubNet=%d, Universe=%d",
                    artPacket.net(), artPacket.subnet(), artPacket.universe(),
                    targetNet, targetSubnet, targetUniverse
                ));
            }
            return;
        }

        packetsTargetUniverse.incrementAndGet();
        byte[] dmxData = artPacket.dmxData();

        if (onDmxFrame != null) {
            try {
                onDmxFrame.accept(dmxData);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler im onDmxFrame Callback: " + e.getMessage(), e);
            }
        }

        // Periodischer Diagnose-Log
        long now = System.currentTimeMillis();
        if (packetsTargetUniverse.get() == 1 || now - lastDataLogTime >= 4000) {
            lastDataLogTime = now;
            int nonZero = 0;
            StringBuilder activeChannels = new StringBuilder();
            for (int i = 0; i < dmxData.length; i++) {
                int val = dmxData[i] & 0xFF;
                if (val > 0) {
                    nonZero++;
                    if (nonZero <= 8) {
                        if (!activeChannels.isEmpty()) activeChannels.append(", ");
                        activeChannels.append("Ch ").append(i + 1).append("=").append(val);
                    }
                }
            }
            if (nonZero > 8) {
                activeChannels.append(", ... (+").append(nonZero - 8).append(" weitere)");
            }

            LOGGER.info(String.format(
                "[Art-Net IN] Paket #%,d von %s:%d | Net=%d, Subnet=%d, Universe=%d | %d Bytes | %d aktive Kanäle (>0): [%s]",
                packetsTargetUniverse.get(), senderAddress.getHostAddress(), senderPort,
                artPacket.net(), artPacket.subnet(), artPacket.universe(),
                dmxData.length, nonZero,
                nonZero > 0 ? activeChannels.toString() : "ALLE 512 KANÄLE SIND 0"
            ));

            if (nonZero == 0) {
                LOGGER.warning("[Art-Net HINWEIS] Alle 512 DMX-Werte im Paket sind 0! Prüfe Grand Master / Fader in deiner Lichtsoftware.");
            } else if (nonZero == 1 && (dmxData[0] & 0xFF) > 0) {
                LOGGER.warning(String.format(
                    "[Art-Net HINWEIS] Es ist NUR Kanal 1 aktiv (Wert %d)! Alle anderen 511 Kanäle (inkl. Dimmer/Shutter/Tilt) sind 0. Bei Moving Heads müssen Dimmer (>0) und Shutter (oft 255) geöffnet sein!",
                    dmxData[0] & 0xFF
                ));
            }
        }
    }

    private void handleArtPoll(InetAddress senderAddress, int senderPort) {
        try {
            byte[] reply = buildArtPollReply();
            DatagramPacket replyPacket = new DatagramPacket(reply, reply.length, senderAddress, senderPort);
            if (socket != null && !socket.isClosed()) {
                socket.send(replyPacket);
                LOGGER.fine(() -> "ArtPollReply an " + senderAddress.getHostAddress() + ":" + senderPort + " gesendet.");
            }
        } catch (Exception e) {
            LOGGER.fine(() -> "Fehler beim Senden von ArtPollReply: " + e.getMessage());
        }
    }

    private byte[] buildArtPollReply() {
        byte[] packet = new byte[239];
        // 0..7: "Art-Net\0"
        System.arraycopy(ArtNetPacket.ARTNET_HEADER, 0, packet, 0, 8);
        // 8..9: OpCode 0x2100 (Little Endian: 0x00, 0x21)
        packet[8] = 0x00;
        packet[9] = 0x21;

        // 10..13: Local IP
        try {
            InetAddress local = InetAddress.getLocalHost();
            byte[] ipBytes = local.getAddress();
            System.arraycopy(ipBytes, 0, packet, 10, 4);
        } catch (Exception ignored) {
            packet[10] = 127; packet[11] = 0; packet[12] = 0; packet[13] = 1;
        }

        // 14..15: Port (Little Endian: 6454 = 0x1936 -> 0x36, 0x19)
        packet[14] = (byte) (port & 0xFF);
        packet[15] = (byte) ((port >> 8) & 0xFF);

        // 16..17: VersInfo 1.0
        packet[16] = 0x01;
        packet[17] = 0x00;

        // 18..19: NetSwitch, SubSwitch
        packet[18] = (byte) (targetNet & 0x7F);
        packet[19] = (byte) (targetSubnet & 0x0F);

        // 20..22: Oem (0x00FF), UbeaVersion (0)
        packet[20] = 0x00;
        packet[21] = (byte) 0xFF;
        packet[22] = 0x00;

        // 23: Status1 (Indicators Normal = 0xD0)
        packet[23] = (byte) 0xD0;

        // 24..25: EstaMan
        packet[24] = 0x00;
        packet[25] = 0x00;

        // 26..43: ShortName (18 Bytes)
        byte[] sn = nodeShortName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(sn, 0, packet, 26, Math.min(sn.length, 17));

        // 44..107: LongName (64 Bytes)
        byte[] ln = nodeLongName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(ln, 0, packet, 44, Math.min(ln.length, 63));

        // 108..171: NodeReport (64 Bytes)
        byte[] nr = "#0001 [0000] JMS USB2DMX Pro Active".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(nr, 0, packet, 108, Math.min(nr.length, 63));

        // 172..173: NumPorts (1 Port)
        packet[172] = 0x00;
        packet[173] = 0x01;

        // 174..177: PortTypes (Port 1 = DMX Output 0x80)
        packet[174] = (byte) 0x80;

        // 178..181: GoodInput
        // 182..185: GoodOutput (Port 1 = Transmitting 0x80)
        packet[182] = (byte) 0x80;

        // 186..189: SwIn
        // 190..193: SwOut (Port 1 = targetUniverse)
        packet[190] = (byte) (Math.max(0, targetUniverse) & 0x0F);

        return packet;
    }

    public boolean isRunning() {
        return running.get();
    }

    public double getActualPps() {
        return actualPps;
    }

    public long getPacketsTargetUniverse() {
        return packetsTargetUniverse.get();
    }

    public long getPacketsTotal() {
        return packetsTotal.get();
    }
}
