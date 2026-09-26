package de.artnet2dmx.artnet;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Art-Net 4 DMX & Poll Packet Definition.
 * Basiert auf der Spezifikation von Artistic Licence und dem Python-Vorbild aus old_artnet2dmx.
 */
public record ArtNetPacket(
    int opCode,
    int sequence,
    int physical,
    int net,
    int subnet,
    int universe,
    int portAddress,
    byte[] dmxData
) {
    public static final byte[] ARTNET_HEADER = "Art-Net\0".getBytes(StandardCharsets.US_ASCII);
    public static final int OP_POLL = 0x2000;
    public static final int OP_POLL_REPLY = 0x2100;
    public static final int OP_DMX = 0x5000;

    /**
     * Parst ein empfangenes UDP-Paket.
     * Gibt null zurück, wenn das Paket kein gültiges Art-Net Paket ist.
     */
    public static ArtNetPacket parse(byte[] buffer, int length) {
        if (length < 10) {
            return null;
        }

        // Header-Prüfung: "Art-Net\0"
        for (int i = 0; i < 8; i++) {
            if (buffer[i] != ARTNET_HEADER[i]) {
                return null;
            }
        }

        // OpCode (Little-Endian)
        int opCode = (buffer[8] & 0xFF) | ((buffer[9] & 0xFF) << 8);

        if (opCode == OP_POLL) {
            return new ArtNetPacket(OP_POLL, 0, 0, 0, 0, 0, 0, new byte[0]);
        }

        if (opCode != OP_DMX || length < 18) {
            return null;
        }

        int sequence = buffer[12] & 0xFF;
        int physical = buffer[13] & 0xFF;

        // SubUni: Bits 0-3 = Universe, Bits 4-7 = Subnet
        int subUni = buffer[14] & 0xFF;
        int universe = subUni & 0x0F;
        int subnet = (subUni >> 4) & 0x0F;

        // Net: Bits 0-6
        int net = buffer[15] & 0x7F;

        // 15-Bit Port-Address
        int portAddress = (net << 8) | (subnet << 4) | universe;

        // DMX Datenlänge (Big Endian: MSB, LSB)
        int dmxLength = ((buffer[16] & 0xFF) << 8) | (buffer[17] & 0xFF);
        int availableData = length - 18;
        int actualLength = Math.min(dmxLength, Math.min(availableData, 512));

        if (actualLength <= 0) {
            return null;
        }

        byte[] dmxData = Arrays.copyOfRange(buffer, 18, 18 + actualLength);
        return new ArtNetPacket(OP_DMX, sequence, physical, net, subnet, universe, portAddress, dmxData);
    }

    public boolean isDmx() {
        return opCode == OP_DMX;
    }

    public boolean isPoll() {
        return opCode == OP_POLL;
    }
}
