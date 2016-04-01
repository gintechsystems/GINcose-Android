package com.gintechsystems.gincose.messages;

import android.util.Log;

import com.gintechsystems.gincose.Extensions;
import com.gintechsystems.gincose.GINcoseWrapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/30/16.
 */
public class TransmitterVersionRxMessage extends TransmitterMessage {
    byte opcode = 0x4b;

    public TransmitterVersionRxMessage(byte[] packet) {
        if (packet.length >= 19) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            }
        }
    }
}
