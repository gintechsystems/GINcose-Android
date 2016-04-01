package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/30/16.
 */
public class SessionStopTxMessage extends TransmitterMessage {
    byte opcode = 0x28;

    public SessionStopTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);
        byteSequence = data.array();
    }
}
