package com.gintechsystems.gincose.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.gintechsystems.gincose.Extensions;
import com.gintechsystems.gincose.GINcoseWrapper;
import com.gintechsystems.gincose.messages.FirmwareVersionRxMessage;
import com.gintechsystems.gincose.messages.FirmwareVersionTxMessage;
import com.gintechsystems.gincose.messages.SessionStartRxMessage;
import com.gintechsystems.gincose.messages.TransmitterStatus;
import com.gintechsystems.gincose.messages.BatteryRxMessage;
import com.gintechsystems.gincose.messages.BatteryTxMessage;
import com.gintechsystems.gincose.messages.GlucoseRxMessage;
import com.gintechsystems.gincose.messages.GlucoseTxMessage;
import com.gintechsystems.gincose.messages.SensorRxMessage;
import com.gintechsystems.gincose.messages.SensorTxMessage;
import com.gintechsystems.gincose.messages.TransmitterTimeRxMessage;
import com.gintechsystems.gincose.messages.DisconnectTxMessage;
import com.gintechsystems.gincose.messages.TransmitterVersionTxMessage;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressWarnings("deprecation")
public class BluetoothManager {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;

    public BluetoothGatt mGatt;
    public BluetoothDevice mDevice;

    private ScanSettings settings;
    private List<ScanFilter> filters;

    // API >= 20
    private ScanCallback mScanCallback;

    public BluetoothManager() {
        android.bluetooth.BluetoothManager mBluetoothManager = (android.bluetooth.BluetoothManager) GINcoseWrapper.getSharedInstance().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }

        setupBluetooth();
    }

    public void setupBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            GINcoseWrapper.getSharedInstance().currentAct.startActivityForResult(enableBtIntent, 0);
        }
        else {
            GINcoseWrapper.getSharedInstance().isDeviceBonded = GINcoseWrapper.getSharedInstance().getTransmitterBondState(GINcoseWrapper.getSharedInstance().currentAct);

            updateBondingStatus();

            // Start scanning!
            startScan();
        }
    }

    public void stopScan() {
        mLEScanner.stopScan(mScanCallback);
    }

    public void startScan() {
        setupScanCallback();

        setupSettingsFilters();

        mLEScanner.startScan(filters, settings, mScanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            stopScan();

            mGatt = device.connectGatt(GINcoseWrapper.getSharedInstance().currentAct, false, gattCallback);
            mDevice = device;

            // Read bonding state changes for the device.
            if (!GINcoseWrapper.getSharedInstance().isBondingReceiverRegistered) {
                GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterAddress = device.getAddress();

                GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterName = device.getName();

                GINcoseWrapper.getSharedInstance().currentAct.registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
                GINcoseWrapper.getSharedInstance().isBondingReceiverRegistered = true;
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    mGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    GINcoseWrapper.getSharedInstance().newAuthFromUnbond = false;
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService cgmService = mGatt.getService(BluetoothServices.CGMService);
                Log.i("onServiceDiscovered", cgmService.getUuid().toString());

                GINcoseWrapper.getSharedInstance().authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
                GINcoseWrapper.getSharedInstance().controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
                GINcoseWrapper.getSharedInstance().communicationCharacteristic = cgmService.getCharacteristic(BluetoothServices.Communication);

                if (GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeControl) {
                    mGatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().controlCharacteristic, true);

                    BluetoothGattDescriptor descriptor = GINcoseWrapper.getSharedInstance().controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    mGatt.writeDescriptor(descriptor);

                    // Control
                    GINcoseWrapper.getSharedInstance().defaultTransmitter.control(gatt, GINcoseWrapper.getSharedInstance().controlCharacteristic);
                }
                else if (GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeCommunication) {
                    mGatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().communicationCharacteristic, true);

                    // Communicate
                    if (!mGatt.readCharacteristic(GINcoseWrapper.getSharedInstance().communicationCharacteristic)) {
                        Log.e("CommCharacteristic", "CommReadError");
                    }
                }
                else {
                    mGatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().authCharacteristic, true);

                    // Authentication
                    if (!mGatt.readCharacteristic(GINcoseWrapper.getSharedInstance().authCharacteristic)) {
                        Log.e("AuthCharacteristic", "AuthReadError");
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Log.i("DescriptWriteBytes", Arrays.toString(descriptor.getCharacteristic().getValue()));
            //Log.i("DescriptWriteHex", Extensions.bytesToHex(descriptor.getCharacteristic().getValue()));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGatt.writeCharacteristic(descriptor.getCharacteristic());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("CharChange", Arrays.toString(characteristic.getValue()));
            Log.i("CharChange", Extensions.bytesToHex(characteristic.getValue()));

            byte firstByte = characteristic.getValue()[0];

            if (GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeControl) {
                // Glucose
                if (firstByte == 0x31) {
                    GlucoseRxMessage glucoseRx = new GlucoseRxMessage(characteristic.getValue());
                    Log.i("GlucoseValue", String.valueOf(glucoseRx.glucose));

                    //long newTime = GINcoseWrapper.getSharedInstance().startTimeInterval + glucoseRx.timestamp;
                    //DateTime dt = new DateTime(newTime, DateTimeZone.getDefault());
                    //Log.i("GlucoseDate", dt.toString("yyyy-MM-dd hh:mm:ss"));

                    SensorTxMessage sensorTx = new SensorTxMessage();
                    characteristic.setValue(sensorTx.byteSequence);
                    mGatt.writeCharacteristic(characteristic);
                }
                // Sensor
                else if (firstByte == 0x2f) {
                    SensorRxMessage sensorRx = new SensorRxMessage(characteristic.getValue());

                    //long newTime = GINcoseWrapper.getSharedInstance().startTimeInterval + sensorRx.timestamp;
                    //DateTime dt = new DateTime(newTime, DateTimeZone.getDefault());
                    //Log.i("SensorDate", dt.toString("yyyy-MM-dd hh:mm:ss"));

                    BatteryTxMessage batteryTx = new BatteryTxMessage();
                    characteristic.setValue(batteryTx.byteSequence);
                    mGatt.writeCharacteristic(characteristic);
                }
                // Transmitter Time
                else if (firstByte == 0x25) {
                    TransmitterTimeRxMessage transmitterTime = new TransmitterTimeRxMessage(characteristic.getValue());

                    if (GINcoseWrapper.getSharedInstance().startTimeInterval == -1) {
                        GINcoseWrapper.getSharedInstance().startTimeInterval = new DateTime().getMillis() - transmitterTime.currentTime;
                        //Log.i("StartTime", String.valueOf(GINcoseWrapper.getSharedInstance().startTimeInterval));
                    }

                    GlucoseTxMessage glucoseTx = new GlucoseTxMessage();
                    characteristic.setValue(glucoseTx.byteSequence);
                    mGatt.writeCharacteristic(characteristic);
                }
                // Battery
                else if (firstByte == 0x23) {
                    BatteryRxMessage batteryRx = new BatteryRxMessage(characteristic.getValue());
                    GINcoseWrapper.getSharedInstance().latestTransmitterStatus = TransmitterStatus.getBatteryLevel(batteryRx.battery);
                    //Log.i("TransmitterStatus", GINcoseWrapper.getSharedInstance().latestTransmitterStatus.toString());

                    FirmwareVersionTxMessage firmwareTx = new FirmwareVersionTxMessage();
                    characteristic.setValue(firmwareTx.byteSequence);
                    mGatt.writeCharacteristic(characteristic);
                }
                // Firmware
                else if (firstByte == 0x21) {
                    FirmwareVersionRxMessage firmwareRx = new FirmwareVersionRxMessage(characteristic.getValue());

                    TransmitterVersionTxMessage transmitterVersionTx = new TransmitterVersionTxMessage();
                    characteristic.setValue(transmitterVersionTx.byteSequence);
                    mGatt.writeCharacteristic(characteristic);
                }
                // Session Start
                else if (firstByte == 0x27) {
                    SessionStartRxMessage sessionStartRx = new SessionStartRxMessage(characteristic.getValue());

                    GINcoseWrapper.getSharedInstance().defaultTransmitter.readNewSession();

                    mGatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().controlCharacteristic, false);

                    GINcoseWrapper.getSharedInstance().doDisconnectMessage(gatt, characteristic);
                }
                else {
                    mGatt.setCharacteristicNotification(GINcoseWrapper.getSharedInstance().controlCharacteristic, false);

                    GINcoseWrapper.getSharedInstance().doDisconnectMessage(gatt, characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("CharWrite", Arrays.toString(characteristic.getValue()));
            Log.i("CharWrite", Extensions.bytesToHex(characteristic.getValue()));

            if (!GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeControl) {
                // We do not want to read if the written bytes were a unbond / bond request.
                // Complete the bond process with the callback and then read the characteristic once bonded.
                if (characteristic.getValue() != null && characteristic.getValue()[0] != 0x7) {
                    mGatt.readCharacteristic(characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
                Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));

                if (!GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeControl) {
                    if (GINcoseWrapper.getSharedInstance().defaultTransmitter.isModeCommunication) {
                        // Communicate
                        GINcoseWrapper.getSharedInstance().defaultTransmitter.communicate(gatt, characteristic);
                    }
                    else {
                        // Authenticate
                        GINcoseWrapper.getSharedInstance().defaultTransmitter.authenticate(gatt, characteristic);
                    }
                }
            }
        }
    };

    // Checks if the bonding state should be updated.
    private void updateBondingStatus() {
        boolean isDeviceBondedWithSystem = getTransmitterBondStatus(mBluetoothAdapter);

        if (GINcoseWrapper.getSharedInstance().isDeviceBonded && !isDeviceBondedWithSystem) {
            Log.i("BondProcess", "Unpaired");

            GINcoseWrapper.getSharedInstance().saveTransmitterBondState(GINcoseWrapper.getSharedInstance().currentAct, false);
            GINcoseWrapper.getSharedInstance().isDeviceBonded = false;

            GINcoseWrapper.getSharedInstance().requestUnbond = true;

            GINcoseWrapper.getSharedInstance().showPermissionToast(GINcoseWrapper.getSharedInstance().currentAct, GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterName + " has been unpaired.");
        }
    }

    // Check if a transmitter is bonded to the system.
    public boolean getTransmitterBondStatus(BluetoothAdapter adapter) {
        boolean isBonded = false;

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {
                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                    if (deviceNameLastTwo.toUpperCase().equals(transmitterIdLastTwo.toUpperCase())) {
                        isBonded = true;
                    }
                }
            }
        }

        return isBonded;
    }

    //This receiver detects when the bonding state for a bt device has changed.
    public final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Make sure the device that is detected as pairing is our transmitter.
                if (!GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterAddress.equals(device.getAddress())) {
                    Log.e("BondProcess", "UnknownDevice");
                    return;
                }

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (GINcoseWrapper.getSharedInstance().isDeviceBonded) {
                    Log.i("BondProcess", "Unpaired");

                    GINcoseWrapper.getSharedInstance().saveTransmitterBondState(GINcoseWrapper.getSharedInstance().currentAct, false);
                    GINcoseWrapper.getSharedInstance().isDeviceBonded = false;

                    GINcoseWrapper.getSharedInstance().requestUnbond = true;

                    GINcoseWrapper.getSharedInstance().showUnbondedMessage = true;
                }
                else {
                    if (state == BluetoothDevice.BOND_BONDED) {
                        Log.i("BondProcess", "Paired");

                        GINcoseWrapper.getSharedInstance().saveTransmitterBondState(GINcoseWrapper.getSharedInstance().currentAct, true);
                        GINcoseWrapper.getSharedInstance().isDeviceBonded = true;

                        GINcoseWrapper.getSharedInstance().showPermissionToast(GINcoseWrapper.getSharedInstance().currentAct, "Transmitter found, paired with " + GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterName + ".");

                        // The device is now paired, read the auth once more.
                        mGatt.readCharacteristic(GINcoseWrapper.getSharedInstance().authCharacteristic);
                    }
                    else if (state == BluetoothDevice.BOND_BONDING) {
                        Log.i("BondProcess", "Bonding");
                    }
                    else {
                        Log.i("BondProcess", "Unknown");
                    }
                }
            }
        }
    };

    // API >= 20 Functions
    private void setupScanCallback() {
        if (mScanCallback == null) {
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.i("result", result.toString());
                    BluetoothDevice device = result.getDevice();

                    // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                    if (device.getName() != null) {
                        String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId);
                        String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                        if (deviceNameLastTwo.toUpperCase().equals(transmitterIdLastTwo.toUpperCase())) {
                            connectToDevice(device);
                        }
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e("ScanFailedErrorCode", String.valueOf(errorCode));
                }
            };
        }
    }

    private void setupSettingsFilters() {
        if (mLEScanner == null) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();

            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            filters = new ArrayList<>();

            // Only look for CGM.
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothServices.Advertisement)).build());
        }
    }

}
