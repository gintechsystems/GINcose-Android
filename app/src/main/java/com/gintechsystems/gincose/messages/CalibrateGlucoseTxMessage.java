package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/28/16.
 */
public class CalibrateGlucoseTxMessage extends TransmitterMessage {
    byte opcode = 0x34;
    byte[] crc = CRC.calculate(opcode);

    public CalibrateGlucoseTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
