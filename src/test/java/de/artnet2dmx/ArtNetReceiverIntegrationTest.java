package de.artnet2dmx;

import de.artnet2dmx.artnet.ArtNetPacket;
import de.artnet2dmx.artnet.ArtNetReceiver;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ArtNetReceiverIntegrationTest {

    @Test
    void testReceiveDmxFrame() throws Exception {
        int testPort = 16454;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> receivedData = new AtomicReference<>();

        ArtNetReceiver receiver = new ArtNetReceiver(
            "127.0.0.1",
            testPort,
            0, // Universe 0
            0, // Subnet 0
            0, // Net 0
            data -> {
                receivedData.set(data);
                latch.countDown();
            }
        );

        receiver.start();
        Thread.sleep(100);

        // Erstelle Test-ArtNet-Paket
        byte[] payload = new byte[18 + 16];
        System.arraycopy(ArtNetPacket.ARTNET_HEADER, 0, payload, 0, 8);
        payload[8] = 0x00;
        payload[9] = 0x50; // OpDmx
        payload[10] = 0x00;
        payload[11] = 14;
        payload[12] = 1;
        payload[13] = 0;
        payload[14] = 0; // Subnet 0, Universe 0
        payload[15] = 0; // Net 0
        payload[16] = 0x00;
        payload[17] = 16; // 16 Kanäle
        payload[18] = (byte) 255;
        payload[19] = (byte) 128;

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(payload, payload.length, InetAddress.getByName("127.0.0.1"), testPort);
            socket.send(packet);
        }

        boolean received = latch.await(2, TimeUnit.SECONDS);
        receiver.stop();

        assertTrue(received, "Art-Net Paket sollte innerhalb von 2 Sekunden empfangen werden");
        assertNotNull(receivedData.get());
        assertEquals(16, receivedData.get().length);
        assertEquals((byte) 255, receivedData.get()[0]);
        assertEquals((byte) 128, receivedData.get()[1]);
    }

    @Test
    void testReceiveArtPollAndSendReply() throws Exception {
        int testPort = 16455;
        ArtNetReceiver receiver = new ArtNetReceiver(
            "127.0.0.1",
            testPort,
            0, 0, 0,
            data -> {}
        );
        receiver.start();
        Thread.sleep(100);

        byte[] pollPayload = new byte[14];
        System.arraycopy(ArtNetPacket.ARTNET_HEADER, 0, pollPayload, 0, 8);
        pollPayload[8] = 0x00;
        pollPayload[9] = 0x20; // OpPoll (0x2000)
        pollPayload[10] = 0x00;
        pollPayload[11] = 14;  // ProtVer
        pollPayload[12] = 0x00; // TalkToMe
        pollPayload[13] = 0x00; // Priority

        byte[] replyBuffer = new byte[1024];
        DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setSoTimeout(2000);
            DatagramPacket sendPacket = new DatagramPacket(pollPayload, pollPayload.length, InetAddress.getByName("127.0.0.1"), testPort);
            clientSocket.send(sendPacket);

            clientSocket.receive(replyPacket);
        }

        receiver.stop();

        assertTrue(replyPacket.getLength() >= 207, "ArtPollReply muss mind. 207 Bytes lang sein");
        String header = new String(replyBuffer, 0, 8);
        assertEquals("Art-Net\0", header);
        int opCode = (replyBuffer[8] & 0xFF) | ((replyBuffer[9] & 0xFF) << 8);
        assertEquals(ArtNetPacket.OP_POLL_REPLY, opCode, "OpCode muss OpPollReply (0x2100) sein");
    }
}
