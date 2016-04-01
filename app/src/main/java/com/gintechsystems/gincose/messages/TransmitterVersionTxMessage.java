package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/30/16.
 */
public class TransmitterVersionTxMessage extends TransmitterMessage {
    byte opcode = 0x4a;
    byte[] crc = CRC.calculate(opcode);

    public TransmitterVersionTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
