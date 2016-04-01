package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class UnbondRequestTxMessage extends TransmitterMessage {
    public UnbondRequestTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(new byte[]{ 0x5, 0x1, 0x2 });

        byteSequence = data.array();
    }
}
