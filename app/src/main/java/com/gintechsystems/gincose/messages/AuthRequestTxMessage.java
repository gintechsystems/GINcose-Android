package com.gintechsystems.gincose.messages;

import android.util.Log;

import com.gintechsystems.gincose.Extensions;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    int opcode = 0x1;
    public byte[] singleUseToken;
    int endByte = 0x2;

    byte[] byteSequence;

    public AuthRequestTxMessage() {
        UUID uuid = UUID.randomUUID();
        singleUseToken = uuid.toString().getBytes();

        // Create the byteSequence.
        data = ByteBuffer.allocate(10);
        data.put((byte)opcode);
        data.put(singleUseToken, 0, 8);
        data.put((byte)endByte);

        byteSequence = data.array();
        //Log.i("ByteSequence", Arrays.toString(byteSequence));
        //Log.i("HexSequence", Extensions.bytesToHex(byteSequence));
    }
}
