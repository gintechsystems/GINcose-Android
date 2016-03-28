package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class KeepAliveTxMessage extends TransmitterMessage {
    byte opcode = 0x6;
    int time;

    public KeepAliveTxMessage(int time) {
        this.time = time;

        data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        data.put(new byte[]{ opcode, (byte)this.time });
        byteSequence = data.array();
    }
}
