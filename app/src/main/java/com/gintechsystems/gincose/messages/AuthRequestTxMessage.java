package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    byte opcode = 0x1;
    public byte[] singleUseToken;
    byte endByte = 0x2;

    public AuthRequestTxMessage() {
        // Create the singleUseToken from a 16 byte array.
        byte[] uuidBytes = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(uuidBytes, 0, 8);
        singleUseToken = bb.array();

        // Create the byteSequence.
        data = ByteBuffer.allocate(10);
        data.put(opcode);
        data.put(singleUseToken);
        data.put(endByte);

        byteSequence = data.array();
    }
}
