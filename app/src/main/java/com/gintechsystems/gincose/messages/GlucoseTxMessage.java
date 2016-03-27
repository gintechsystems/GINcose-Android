package com.gintechsystems.gincose.messages;

import android.util.Log;

import com.gintechsystems.gincose.CRC;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class GlucoseTxMessage extends TransmitterMessage {
    byte opcode = 0x30;
    byte[] crc = CRC.calculate(opcode);

    public GlucoseTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);

        byteSequence = data.array();
    }
}
