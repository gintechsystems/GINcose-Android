package com.gintechsystems.gincose.messages;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public abstract class TransmitterMessage {
    public byte[] byteSequence = null;
    public ByteBuffer data = null;
}
