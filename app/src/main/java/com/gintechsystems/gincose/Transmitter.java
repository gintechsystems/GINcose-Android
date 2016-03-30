package com.gintechsystems.gincose;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.gintechsystems.gincose.bluetooth.BluetoothServices;
import com.gintechsystems.gincose.messages.AuthChallengeRxMessage;
import com.gintechsystems.gincose.messages.AuthChallengeTxMessage;
import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;
import com.gintechsystems.gincose.messages.BondRequestTxMessage;
import com.gintechsystems.gincose.messages.KeepAliveTxMessage;
import com.gintechsystems.gincose.messages.TransmitterTimeTxMessage;
import com.gintechsystems.gincose.messages.UnbondRequestTxMessage;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by joeginley on 3/19/16.
 */
public class Transmitter {
    public String transmitterId;

    public Boolean isModeControl = false;
    public Boolean isModeCommunication = false;

    public Transmitter() {}

    public Transmitter(String id) {
        transmitterId = id;
    }

    public void authenticate(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte firstByte = characteristic.getValue()[0];

        if (firstByte == 0x5 || firstByte <= 0x0) {
            GINcoseWrapper.getSharedInstance().authStatus = new AuthStatusRxMessage(characteristic.getValue());
            if (GINcoseWrapper.getSharedInstance().authStatus.authenticated == 1 && GINcoseWrapper.getSharedInstance().authStatus.bonded == 1) {
                Log.i("Auth", "Transmitter already authenticated");

                if (GINcoseWrapper.getSharedInstance().requestUnbond) {
                    UnbondRequestTxMessage unbondRequest = new UnbondRequestTxMessage();
                    characteristic.setValue(unbondRequest.byteSequence);
                    gatt.writeCharacteristic(characteristic);

                    unpairDevice(gatt.getDevice());

                    GINcoseWrapper.getSharedInstance().requestUnbond = false;
                    return;
                }

                gatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().authCharacteristic, false);

                // Looks like we are authenticated and bonded, write the control characteristics.
                control(gatt, characteristic);
                return;
            }
            else if (GINcoseWrapper.getSharedInstance().authStatus.authenticated == 1 && GINcoseWrapper.getSharedInstance().authStatus.bonded == 2) {
                Log.i("Auth", "Transmitter authenticated, requesting bond");

                KeepAliveTxMessage keepAliveTx = new KeepAliveTxMessage(25);
                characteristic.setValue(keepAliveTx.byteSequence);
                gatt.writeCharacteristic(characteristic);

                BondRequestTxMessage bondRequestTx = new BondRequestTxMessage();
                characteristic.setValue(bondRequestTx.byteSequence);
                gatt.writeCharacteristic(characteristic);

                // If we don't call this the device does not officially get paired!
                pairDevice(gatt.getDevice());
            }
            else {
                Log.i("Auth", "Transmitter not authenticated");

                GINcoseWrapper.getSharedInstance().authRequest = new AuthRequestTxMessage();
                characteristic.setValue(GINcoseWrapper.getSharedInstance().authRequest.byteSequence);
                gatt.writeCharacteristic(characteristic);
            }
        }

        // Auth challenge and token have been retrieved.
        if (firstByte == 0x3) {
            AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
            if (GINcoseWrapper.getSharedInstance().authRequest == null) {
                GINcoseWrapper.getSharedInstance().authRequest = new AuthRequestTxMessage();
                return;
            }

            if (!Arrays.equals(authChallenge.tokenHash, calculateHash(GINcoseWrapper.getSharedInstance().authRequest.singleUseToken))) {
                Log.i("tokenHash", Arrays.toString(authChallenge.tokenHash));
                Log.i("singleUseToken", Arrays.toString(calculateHash(GINcoseWrapper.getSharedInstance().authRequest.singleUseToken)));
                Log.e("Auth", "Transmitter failed auth challenge");
            }

            byte[] challengeHash = calculateHash(authChallenge.challenge);
            if (challengeHash != null) {
                AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);
                android.util.Log.i("AuthChallenge",  Arrays.toString(authChallengeTx.byteSequence));
                characteristic.setValue(authChallengeTx.byteSequence);
                gatt.writeCharacteristic(characteristic);
            }
        }
        // Init auth.
        else if (firstByte == 0x8) {
            if (GINcoseWrapper.getSharedInstance().authRequest == null) {
                Log.i("Auth", "Transmitter not authenticated");
            }

            GINcoseWrapper.getSharedInstance().authRequest = new AuthRequestTxMessage();
            characteristic.setValue(GINcoseWrapper.getSharedInstance().authRequest.byteSequence);
            gatt.writeCharacteristic(characteristic);
        }
    }

    public void control(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isModeControl) {
            isModeControl = true;

            gatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().controlCharacteristic, true);

            BluetoothGattDescriptor descriptor = GINcoseWrapper.getSharedInstance().controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        TransmitterTimeTxMessage timeTx = new TransmitterTimeTxMessage();
        characteristic.setValue(timeTx.byteSequence);
        gatt.writeCharacteristic(characteristic);
    }

    public void communicate(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isModeCommunication) {
            isModeCommunication = true;
            isModeControl = false;

            gatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().communicationCharacteristic, true);

            BluetoothGattDescriptor descriptor = GINcoseWrapper.getSharedInstance().communicationCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    @SuppressLint("GetInstance")
    private byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            Log.e("Decrypt", "Data length should be exactly 8.");
            return null;
        }

        byte[] key = cryptKey();
        if (key == null)
            return null;

        byte[] doubleData;
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(data);
        bb.put(data);

        doubleData = bb.array();

        Cipher aesCipher;
        try {
            aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] aesBytes = aesCipher.doFinal(doubleData, 0, doubleData.length);

            bb = ByteBuffer.allocate(8);
            bb.put(aesBytes, 0, 8);

            return bb.array();
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] cryptKey() {
        try {
            return ("00" + GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId + "00" + GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId).getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    // These methods work with API < 19 - Bonding Methods

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
