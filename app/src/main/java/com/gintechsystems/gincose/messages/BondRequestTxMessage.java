package com.gintechsystems.gincose.messages;

/**
 * Created by joeginley on 3/16/16.
 */
public class BondRequestTxMessage extends TransmitterMessage {
    static int opcode = 0x7;
    public byte[] byteSequence = { (byte)opcode };
}
