package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/28/16.
 */
public class CalibrationTxMessage extends TransmitterMessage {
    byte opcode = 0x34;
    byte[] crc = CRC.calculate(opcode);

    public CalibrationTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
