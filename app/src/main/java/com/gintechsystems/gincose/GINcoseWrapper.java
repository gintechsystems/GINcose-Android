package com.gintechsystems.gincose;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;
import com.gintechsystems.gincose.messages.TransmitterStatus;

/**
 * Created by joeginley on 3/16/16.
 */
public class GINcoseWrapper extends Application {
    private static GINcoseWrapper singleton;

    public Activity currentAct;

    public Transmitter defaultTransmitter;

    public AuthStatusRxMessage authStatus;
    public AuthRequestTxMessage authRequest;

    public BluetoothGattCharacteristic authCharacteristic;
    public BluetoothGattCharacteristic controlCharacteristic;
    public BluetoothGattCharacteristic communicationCharacteristic;

    // Manually switch this on to unpair a transmitter.
    public boolean requestUnbond = false;

    // Boolean for bonding receiver.
    public boolean isBondingReceiverRegistered = false;

    // Transmitter start time, required to get the correct timestamps.
    public long startTimeInterval = -1;

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
}
