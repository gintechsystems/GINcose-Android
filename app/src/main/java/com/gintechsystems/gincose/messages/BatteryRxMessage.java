package com.gintechsystems.gincose.messages;


import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/26/16.
 */
public class BatteryRxMessage extends TransmitterMessage {
    byte opcode = 0x23;
    public int battery;

    public BatteryRxMessage(byte[] packet) {
        if (packet.length >= 12) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                battery = data.get(1);

                byteSequence = data.array();
            }
        }
    }
}
