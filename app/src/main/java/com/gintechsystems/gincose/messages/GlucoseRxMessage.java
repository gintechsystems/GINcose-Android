package com.gintechsystems.gincose.messages;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class GlucoseRxMessage extends TransmitterMessage {
    byte opcode = 0x31;
    public TransmitterStatus status;
    public int sequence;
    public int timestamp;
    public boolean glucoseIsDisplayOnly;
    public int glucose;
    public int state;
    public int trend;

    public GlucoseRxMessage(byte[] packet) {
        if (packet.length >= 14) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status = TransmitterStatus.getBatteryLevel(data.get(1));

                sequence = data.getInt(2);
                timestamp = data.getInt(6);

                glucose = data.getShort(10);
                glucoseIsDisplayOnly = glucose > 0;

                state = data.get(12);
                trend = data.get(13);
            }
        }
    }
}
