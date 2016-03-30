package com.gintechsystems.gincose.messages;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/28/16.
 */
public class CalibrationRxMessage extends TransmitterMessage {
    byte opcode = 0x35;

    public CalibrationRxMessage(byte[] packet) {
        if (packet.length >= 5) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);


            }
        }
    }
}
