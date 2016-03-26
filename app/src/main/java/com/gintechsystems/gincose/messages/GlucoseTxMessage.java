package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;
import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class GlucoseTxMessage extends TransmitterMessage {
    byte opcode = 0x30;
    byte[] crc = CRC.calculate(ByteBuffer.allocate(1).put(opcode).array());

    public GlucoseTxMessage() {
        data = ByteBuffer.allocate(crc.length + 1);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
