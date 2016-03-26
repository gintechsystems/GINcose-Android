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
import com.gintechsystems.gincose.messages.GlucoseRxMessage;
import com.gintechsystems.gincose.messages.UnbondRequestTxMessage;
import com.gintechsystems.gincose.messages.DisconnectTxMessage;
import com.gintechsystems.gincose.messages.KeepAliveTxMessage;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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
    }

    public void setupBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // First time using the app or bluetooth was turned off?
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
                // Only look for CGM.
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothServices.Advertisement)).build());
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

            // Required when attempting to bond.
            gincoseWrap.currentAct.registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
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
            BluetoothGattService cgmService = gatt.getService(BluetoothServices.CGMService);
            Log.i("onServiceDiscovered", cgmService.getUuid().toString());

            gincoseWrap.authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
            gincoseWrap.controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);

            if (gincoseWrap.defaultTransmitter.isModeControl) {
                mGatt.setCharacteristicNotification(gincoseWrap.controlCharacteristic, true);

                // Control
                gincoseWrap.defaultTransmitter.control(gatt, gincoseWrap.controlCharacteristic);
            }
            else {
                mGatt.setCharacteristicNotification(gincoseWrap.authCharacteristic, true);

                if (!mGatt.readCharacteristic(gincoseWrap.authCharacteristic)) {
                    Log.e("AuthCharacteristic", "AuthReadError");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("CharChange", Arrays.toString(characteristic.getValue()));
            Log.i("CharChange", Extensions.bytesToHex(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("CharWrite", Arrays.toString(characteristic.getValue()));
            Log.i("CharWrite", Extensions.bytesToHex(characteristic.getValue()));

            if (!gincoseWrap.defaultTransmitter.isModeControl) {
                // We do not want to read if the written bytes were a unbond / bond request.
                // Complete the bond process with the callback and then read the characteristic once bonded.
                if (characteristic.getValue() != null && characteristic.getValue()[0] != 7 && characteristic.getValue()[0] != 6) {
                    mGatt.readCharacteristic(characteristic);
                }
            }
            else {
                Log.i("GlucoseData", Arrays.toString(characteristic.getValue()));
                GlucoseRxMessage glucoseRx = new GlucoseRxMessage(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
                Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));

                if (!gincoseWrap.defaultTransmitter.isModeControl) {
                    // Authenticate
                    gincoseWrap.defaultTransmitter.authenticate(gatt, characteristic);
                }
            }
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                //final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.i("BondProcess", "Paired");

                    if (mGatt.getDevice().getName() != null) {
                        gincoseWrap.showPermissionToast(gincoseWrap.currentAct, "Transmitter found, paired with " + mGatt.getDevice().getName() + ".");
                    }
                    else {
                        gincoseWrap.showPermissionToast(gincoseWrap.currentAct, "Transmitter found & paired.");
                    }

                    // The device is paired, read the auth once more.
                    mGatt.readCharacteristic(gincoseWrap.authCharacteristic);

                    gincoseWrap.currentAct.unregisterReceiver(this);
                }
                else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.i("BondProcess", "Bonding");
                }
                else {
                    Log.i("BondProcess", "Unknown");
                }
            }
        }
    };
}
