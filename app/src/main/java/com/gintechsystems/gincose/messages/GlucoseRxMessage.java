package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;
import com.gintechsystems.gincose.Extensions;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class GlucoseRxMessage extends TransmitterMessage {
    byte opcode = 0x31;
    byte status;
    byte[] sequence;
    byte[] timestamp;
    Boolean glucoseIsDisplayOnly;
    int glucose;
    byte state;
    byte trend;

    public GlucoseRxMessage(byte[] data) {
        if (data.length >= 14) {
            if (data[0] == opcode) {
                status = data[1];
                sequence = Arrays.copyOfRange(data, 2, 5);
                timestamp = Arrays.copyOfRange(data, 6, 9);

                byte[] glucoseBytes = Arrays.copyOfRange(data, 10, 11);
                glucoseIsDisplayOnly = (Extensions.byteArrayToInt(glucoseBytes) & 0xf000) > 0;
                glucose = Extensions.byteArrayToInt(glucoseBytes) & 0xfff;

                state = data[12];
                trend = data[13];
            }
        }
    }
}
