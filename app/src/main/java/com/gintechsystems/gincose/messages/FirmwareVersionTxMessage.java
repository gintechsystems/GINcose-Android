package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/30/16.
 */
public class FirmwareVersionTxMessage extends TransmitterMessage {
    byte opcode = 0x20;

    public FirmwareVersionTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);

        byteSequence = data.array();
    }
}
