package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/30/16.
 */
public class SessionStartTxMessage extends TransmitterMessage {
    byte opcode = 0x26;
    byte[] crc = CRC.calculate(opcode);

    public SessionStartTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
