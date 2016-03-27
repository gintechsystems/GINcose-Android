package com.gintechsystems.gincose.messages;

import java.util.Arrays;

/**
 * Created by joeginley on 3/26/16.
 */
public class SensorRxMessage extends TransmitterMessage {
    byte opcode = 0x2f;
    int status;
    byte[] timestamp;
    byte[] unfiltered;
    byte[] filtered;

    public SensorRxMessage(byte[] data) {
        if (data.length >= 14) {
            if (data[0] == opcode) {
                status = data[1];
                timestamp = Arrays.copyOfRange(data, 2, 5);

                unfiltered = Arrays.copyOfRange(data, 6, 9);
                filtered = Arrays.copyOfRange(data, 10, 13);
            }
        }
    }
}
