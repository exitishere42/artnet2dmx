package de.artnet2dmx;

import de.artnet2dmx.artnet.ArtNetPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArtNetPacketTest {

    @Test
    void testValidArtNetPacketParsing() {
        byte[] buffer = new byte[18 + 512];
        System.arraycopy(ArtNetPacket.ARTNET_HEADER, 0, buffer, 0, 8);
        buffer[8] = 0x00;
        buffer[9] = 0x50; // OpDmx 0x5000
        buffer[10] = 0x00;
        buffer[11] = 14; // ProtVer 14
        buffer[12] = 1;  // Sequence
        buffer[13] = 0;  // Physical
        buffer[14] = (byte) ((2 << 4) | 3); // Subnet 2, Universe 3 -> 0x23
        buffer[15] = 1;  // Net 1
        buffer[16] = 0x02; // Length 512 (0x0200)
        buffer[17] = 0x00;

        buffer[18] = (byte) 255; // Ch 1
        buffer[19] = (byte) 128; // Ch 2

        ArtNetPacket packet = ArtNetPacket.parse(buffer, buffer.length);
        assertNotNull(packet);
        assertEquals(3, packet.universe());
        assertEquals(2, packet.subnet());
        assertEquals(1, packet.net());
        assertEquals(512, packet.dmxData().length);
        assertEquals((byte) 255, packet.dmxData()[0]);
        assertEquals((byte) 128, packet.dmxData()[1]);
    }

    @Test
    void testInvalidHeaderRejected() {
        byte[] buffer = new byte[64];
        buffer[0] = 'X';
        assertNull(ArtNetPacket.parse(buffer, buffer.length));
    }

    @Test
    void testTooShortPacketRejected() {
        byte[] buffer = new byte[10];
        assertNull(ArtNetPacket.parse(buffer, buffer.length));
    }
}
