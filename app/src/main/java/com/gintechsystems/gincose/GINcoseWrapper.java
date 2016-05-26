package com.gintechsystems.gincose;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gintechsystems.gincose.bluetooth.BluetoothManager;
import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;
import com.gintechsystems.gincose.messages.DisconnectTxMessage;
import com.gintechsystems.gincose.messages.TransmitterStatus;

import java.util.Set;

/**
 * Created by joeginley on 3/16/16.
 */
public class GINcoseWrapper extends Application {
    private static GINcoseWrapper singleton;

    public Activity currentAct;

    public BluetoothManager btManager;

    public Transmitter defaultTransmitter;

    public AuthStatusRxMessage authStatus;
    public AuthRequestTxMessage authRequest;

    public BluetoothGattCharacteristic authCharacteristic;
    public BluetoothGattCharacteristic controlCharacteristic;
    public BluetoothGattCharacteristic communicationCharacteristic;

    // Manually switch this on to unpair a transmitter.
    public boolean requestUnbond = false;

    // Boolean for starting a new sensor session.
    public boolean requestNewSession = false;

    // Boolean for new auth from an unpair request.
    public boolean newAuthFromUnbond = false;

    // Boolean for showing a toast after device bond state changed outside of the app.
    public boolean showUnbondedMessage = false;

    // Boolean for bonding receiver.
    public boolean isBondingReceiverRegistered = false;

    // Boolean for device bond state.
    public boolean isDeviceBonded = false;

    // Transmitter start time, required to get the correct timestamps.
    public long startTimeInterval = -1;

    // Latest transmitter battery status.
    public TransmitterStatus latestTransmitterStatus = TransmitterStatus.UNKNOWN;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }

    @SuppressLint("InflateParams")
    public void showPermissionToast(Activity act, String message) {
        View toastLayout = act.getLayoutInflater().inflate(R.layout.toast_layout, null);

        TextView toastText = (TextView)toastLayout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(act);
        toast.setView(toastLayout);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public static GINcoseWrapper getSharedInstance(){
        return singleton;
    }

    public Boolean locationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void saveTransmitterAddress(Activity act, String transmitterAddress) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.putString("defaultTransmitterAddress", transmitterAddress);
        prefsEdit.apply();
    }

    public String getStoredTransmitterAddress(Activity act) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        return defaultPrefs.getString("defaultTransmitterAddress", null);
    }

    public void saveTransmitterId(Activity act, String transmitterId) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.putString("defaultTransmitterId", transmitterId);
        prefsEdit.apply();
    }

    public String getStoredTransmitterId(Activity act) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        return defaultPrefs.getString("defaultTransmitterId", null);
    }

    public void saveTransmitterName(Activity act, String transmitterName) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.putString("defaultTransmitterName", transmitterName);
        prefsEdit.apply();
    }

    public String getStoredTransmitterName(Activity act) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        return defaultPrefs.getString("defaultTransmitterName", null);
    }

    public void saveTransmitterBondState(Activity act, boolean transmitterBondState) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.putBoolean("defaultTransmitterState", transmitterBondState);
        prefsEdit.apply();
    }

    public boolean getTransmitterBondState(Activity act) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        return defaultPrefs.getBoolean("defaultTransmitterState", false);
    }

    public void saveSensorSession(Activity act, boolean sessionStarted) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.putBoolean("defaultSensorSession", sessionStarted);
        prefsEdit.apply();
    }

    public boolean getSensorSession(Activity act) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        return defaultPrefs.getBoolean("defaultSensorSession", false);
    }

    public void removeStoredKey(Activity act, String key) {
        SharedPreferences defaultPrefs = act.getSharedPreferences("defaultPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = defaultPrefs.edit();
        prefsEdit.remove(key);
        prefsEdit.apply();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.M)
    public int getCompatibleColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(color, null);
        }

        return getResources().getColor(color);
    }

    // Sends the disconnect tx message to our bt device.
    public void doDisconnectMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        DisconnectTxMessage disconnectTx = new DisconnectTxMessage();
        characteristic.setValue(disconnectTx.byteSequence);
        gatt.writeCharacteristic(characteristic);
    }

    public void doStopBTManagerOnExit() {
        if (btManager != null) {
            btManager.stopScan();

            if (btManager.mGatt != null) {
                btManager.mGatt.close();
                btManager.mGatt = null;
            }

            if (currentAct != null && isBondingReceiverRegistered) {
                currentAct.unregisterReceiver(btManager.mPairReceiver);
            }
        }
    }

}
