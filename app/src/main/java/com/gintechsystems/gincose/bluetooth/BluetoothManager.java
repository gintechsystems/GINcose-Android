package com.gintechsystems.gincose.bluetooth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.gintechsystems.gincose.Extensions;
import com.gintechsystems.gincose.GINcoseWrapper;
import com.gintechsystems.gincose.messages.AuthChallengeRxMessage;
import com.gintechsystems.gincose.messages.AuthChallengeTxMessage;
import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;
import com.gintechsystems.gincose.messages.BondRequestTxMessage;
import com.gintechsystems.gincose.messages.KeepAliveTxMessage;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by joeginley on 3/16/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressWarnings("deprecation")
public class BluetoothManager {

    private GINcoseWrapper gincoseWrap;

    private final static int REQUEST_ENABLE_BT = 1;

    private android.bluetooth.BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private BluetoothGatt mGatt;

    private ScanSettings settings;
    private List<ScanFilter> filters;

    public BluetoothManager(GINcoseWrapper c) {
        gincoseWrap = c;

        mBluetoothManager = (android.bluetooth.BluetoothManager)gincoseWrap.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        setupBluetooth();

        //Log.i("Bytes", Arrays.toString(Extensions.hexToBytes("01dbbbaa3f742144c702")));
        //new AuthRequestTxMessage();
    }

    public void setupBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            gincoseWrap.currentAct.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
                //Only look for CGM.
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(BluetoothServices.Advertisement))).build());
            }
            startScan();
        }
    }

    public void stopScan() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    public void startScan() {
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
            // If they match, connect to the device.
            if (btDevice.getName() != null) {
                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(gincoseWrap.defaultTransmitter.transmitterId);
                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());

                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                    //They match, connect to the device.
                    connectToDevice(btDevice);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    gincoseWrap.currentAct.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
                            // If they match, connect to the device.
                            if (device.getName() != null) {
                                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(gincoseWrap.defaultTransmitter.transmitterId);
                                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                    //They match, connect to the device.
                                    connectToDevice(device);
                                }
                            }
                        }
                    });
                }
            };

    private void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(gincoseWrap.currentAct, false, gattCallback);
            stopScan();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt.close();
                    mGatt = null;
                    startScan();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService cgmService = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
            Log.i("onServiceDiscovered", cgmService.getUuid().toString());

            if (gincoseWrap.authStatus != null && gincoseWrap.authStatus.authenticated == 1) {
                BluetoothGattCharacteristic controlCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Control));
                if (!mGatt.readCharacteristic(controlCharacteristic)) {
                    Log.e("onCharacteristicRead", "ReadCharacteristicError");
                }
            }
            else {
                BluetoothGattCharacteristic authCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
                if (!mGatt.readCharacteristic(authCharacteristic)) {
                    Log.e("onCharacteristicRead", "ReadCharacteristicError");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("CharWrite", Arrays.toString(characteristic.getValue()));
            Log.i("CharWrite", Extensions.bytesToHex(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
                Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));

                // Request authentication.
                gincoseWrap.authStatus = new AuthStatusRxMessage(characteristic.getValue());
                if (gincoseWrap.authStatus.authenticated == 1 && gincoseWrap.authStatus.bonded == 1) {
                    Log.i("Auth", "Transmitter already authenticated");
                }
                else {
                    if (gincoseWrap.authRequest == null) {
                        mGatt.setCharacteristicNotification(characteristic, true);

                        gincoseWrap.authRequest = new AuthRequestTxMessage();

                        characteristic.setValue(gincoseWrap.authRequest.byteSequence);
                        mGatt.writeCharacteristic(characteristic);

                        //Extensions.doSleep(200);

                        mGatt.readCharacteristic(characteristic);

                        return;
                    }

                    // Auth challenge and token have been retrieved.
                    if (characteristic.getValue()[0] == 0x3) {
                        AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());

                        if (!Arrays.equals(authChallenge.tokenHash, calculateHash(gincoseWrap.authRequest.singleUseToken))) {
                            Log.d("Auth", Extensions.bytesToHex(authChallenge.tokenHash));
                            Log.d("Auth", Extensions.bytesToHex(calculateHash(gincoseWrap.authRequest.singleUseToken)));
                            Log.e("Auth", "Transmitter failed auth challenge");
                            return;
                        }

                        byte[] challengeHash = calculateHash(authChallenge.challenge);
                        if (challengeHash != null) {
                            AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);

                            characteristic.setValue(authChallengeTx.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            //Extensions.doSleep(200);

                            mGatt.readCharacteristic(characteristic);
                        }
                    }
                    // New auth after completing challenge. We should see pairing here.
                    else if (characteristic.getValue()[0] == 0x5) {
                        gincoseWrap.authStatus = new AuthStatusRxMessage(characteristic.getValue());

                        if (gincoseWrap.authStatus.authenticated != 1) {
                            Log.e("Auth", "Transmitter rejected auth challenge");
                        }

                        if (gincoseWrap.authStatus.bonded != 1) {
                            KeepAliveTxMessage keepAlive = new KeepAliveTxMessage(25);

                            characteristic.setValue(keepAlive.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            mGatt.readCharacteristic(characteristic);

                            BondRequestTxMessage bondRequest = new BondRequestTxMessage();

                            characteristic.setValue(bondRequest.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            //Extensions.doSleep(200);

                            mGatt.readCharacteristic(characteristic);

                            mGatt.setCharacteristicNotification(characteristic, false);
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            Log.d("BondReceiver", "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

            // skip other devices
            if (!device.getAddress().equals(mGatt.getDevice().getAddress()))
                return;

            if (bondState == BluetoothDevice.BOND_BONDED) {
                gincoseWrap.currentAct.unregisterReceiver(this);
            }
        }
    };

    @SuppressLint("GetInstance")
    private byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            Log.e("Decrypt", "Data length should be exactly 8.");
            return  null;
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

            //Log.d("Crypt", Arrays.toString(aesBytes));

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
}
