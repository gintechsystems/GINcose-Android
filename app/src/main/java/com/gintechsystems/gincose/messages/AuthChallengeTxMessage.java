package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    int opcode = 0x4;
    byte[] challengeHash;

    public AuthChallengeTxMessage() {

    }
}
