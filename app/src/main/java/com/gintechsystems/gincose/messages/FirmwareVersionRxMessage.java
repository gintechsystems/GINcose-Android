package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/30/16.
 */
public class FirmwareVersionRxMessage extends TransmitterMessage {
    byte opcode = 0x1c;
    public int version;

    public FirmwareVersionRxMessage(byte[] packet) {
        if (packet.length >= 3) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                version = data.getInt(1);
            }
        }
    }
}
