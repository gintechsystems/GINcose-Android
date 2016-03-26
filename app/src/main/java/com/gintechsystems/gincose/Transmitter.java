package com.gintechsystems.gincose;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.gintechsystems.gincose.messages.AuthChallengeRxMessage;
import com.gintechsystems.gincose.messages.AuthChallengeTxMessage;
import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;
import com.gintechsystems.gincose.messages.BondRequestTxMessage;
import com.gintechsystems.gincose.messages.DisconnectTxMessage;
import com.gintechsystems.gincose.messages.GlucoseRxMessage;
import com.gintechsystems.gincose.messages.GlucoseTxMessage;
import com.gintechsystems.gincose.messages.KeepAliveTxMessage;
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
    private GINcoseWrapper gincoseWrap;

    public String transmitterId = "";

    public Boolean isModeControl = false;

    public Transmitter(Context c) {
        gincoseWrap = (GINcoseWrapper)c.getApplicationContext();
    }

    public Transmitter(Context c, String id) {
        gincoseWrap = (GINcoseWrapper)c.getApplicationContext();

        transmitterId = id;
    }

    public void authenticate(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getValue()[0] == 5 || characteristic.getValue()[0] < 0 ) {
            gincoseWrap.authStatus = new AuthStatusRxMessage(characteristic.getValue());
            if (gincoseWrap.authStatus.authenticated == 1 && gincoseWrap.authStatus.bonded == 1) {
                Log.i("Auth", "Transmitter already authenticated");

                if (gincoseWrap.requestUnbond) {
                    UnbondRequestTxMessage debondRequest = new UnbondRequestTxMessage();
                    characteristic.setValue(debondRequest.byteSequence);
                    gatt.writeCharacteristic(characteristic);

                    unpairDevice(gatt.getDevice());

                    gincoseWrap.requestUnbond = false;
                }

                gatt.setCharacteristicNotification(gincoseWrap.authCharacteristic, false);

                // Looks like we are authenticated and bonded, read the control characteristics.
                control(gatt, characteristic);
                return;
            }
            else if (gincoseWrap.authStatus.authenticated == 1 && gincoseWrap.authStatus.bonded == 2) {
                Log.i("Auth", "Transmitter authenticated, requesting bond");

                KeepAliveTxMessage keepAlive = new KeepAliveTxMessage(25);
                characteristic.setValue(keepAlive.byteSequence);
                gatt.writeCharacteristic(characteristic);

                BondRequestTxMessage bondRequest = new BondRequestTxMessage();
                characteristic.setValue(bondRequest.byteSequence);
                gatt.writeCharacteristic(characteristic);

                // If we don't call this the device does not officially get paired!
                pairDevice(gatt.getDevice());
            }
            else {
                Log.i("Auth", "Transmitter not authenticated");

                gincoseWrap.authRequest = new AuthRequestTxMessage();
                characteristic.setValue(gincoseWrap.authRequest.byteSequence);
                gatt.writeCharacteristic(characteristic);
            }
        }

        if (characteristic.getValue()[0] == 8) {
            Log.i("Auth", "Transmitter not authenticated");

            gincoseWrap.authRequest = new AuthRequestTxMessage();
            characteristic.setValue(gincoseWrap.authRequest.byteSequence);
            gatt.writeCharacteristic(characteristic);
        }

        // Auth challenge and token have been retrieved.
        if (characteristic.getValue()[0] == 3) {
            AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
            if (gincoseWrap.authRequest == null) {
                gincoseWrap.authRequest = new AuthRequestTxMessage();
                return;
            }

            if (!Arrays.equals(authChallenge.tokenHash, calculateHash(gincoseWrap.authRequest.singleUseToken))) {
                Log.i("tokenHash", Arrays.toString(authChallenge.tokenHash));
                Log.i("singleUseToken", Arrays.toString(calculateHash(gincoseWrap.authRequest.singleUseToken)));
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
    }

    public void control(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isModeControl) {
            isModeControl = true;

            gatt.setCharacteristicNotification(gincoseWrap.controlCharacteristic, true);
        }

        GlucoseTxMessage glucoseTx = new GlucoseTxMessage();
        characteristic.setValue(glucoseTx.byteSequence);
        gatt.writeCharacteristic(characteristic);
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
            aesCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
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
            return ("00" + gincoseWrap.defaultTransmitter.transmitterId + "00" + gincoseWrap.defaultTransmitter.transmitterId).getBytes("UTF-8");
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
