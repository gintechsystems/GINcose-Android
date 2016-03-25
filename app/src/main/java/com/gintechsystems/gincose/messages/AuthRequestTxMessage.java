package com.gintechsystems.gincose.messages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    int opcode = 0x1;
    public byte[] singleUseToken;
    int endByte = 0x2;

    public AuthRequestTxMessage() {
        // Create the singleUseToken from a random UUID.
        //byte[] uuidRequiredBytes = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        UUID uuid = UUID.randomUUID();
        try {
            byte[] uuidBytes = uuid.toString().getBytes("UTF-8");

            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.put(uuidBytes, 0, 8);
            singleUseToken = bb.array();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Create the byteSequence.
        data = ByteBuffer.allocate(10);
        data.put((byte)opcode);
        data.put(singleUseToken);
        data.put((byte)endByte);

        byteSequence = data.array();
    }
}
