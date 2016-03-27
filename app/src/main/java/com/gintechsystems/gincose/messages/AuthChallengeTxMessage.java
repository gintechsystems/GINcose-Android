package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    byte opcode = 0x4;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;

        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);

        byteSequence = data.array();
    }
}
