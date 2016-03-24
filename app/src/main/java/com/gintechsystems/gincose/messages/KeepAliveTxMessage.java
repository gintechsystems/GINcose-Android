package com.gintechsystems.gincose.messages;

/**
 * Created by joeginley on 3/16/16.
 */
public class KeepAliveTxMessage extends TransmitterMessage {
    int opcode = 0x6;
    int time = 25;
}
