package com.gintechsystems.gincose.messages;

import android.util.Log;

import com.gintechsystems.gincose.CRC;
import com.gintechsystems.gincose.Extensions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by joeginley on 3/30/16.
 */
public class SessionStartRxMessage extends TransmitterMessage {
    byte opcode = 0x27;
    public TransmitterStatus status;
    public int timestamp;


    public SessionStartRxMessage(byte[] packet) {
        if (packet.length >= 17) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status = TransmitterStatus.getBatteryLevel(data.get(1));
                timestamp = data.getInt(2);
            }
        }
    }
}
