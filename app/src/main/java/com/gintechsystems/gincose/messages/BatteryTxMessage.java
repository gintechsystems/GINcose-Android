package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/26/16.
 */
public class BatteryTxMessage extends TransmitterMessage {
    byte opcode = 0x22;
    byte[] crc = CRC.calculate(opcode);


    public BatteryTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}
