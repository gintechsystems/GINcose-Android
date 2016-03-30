package com.gintechsystems.gincose;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.gintechsystems.gincose.bluetooth.BluetoothManager;

/**
 * Created by joeginley on 3/13/16.
 */
public class MainActivity extends Activity {
    private BluetoothManager btManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GINcoseWrapper.getSharedInstance().currentAct = this;

        RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.activity_main);
        final EditText transmitterIdBox = (EditText)findViewById(R.id.transmitter_id_box);

        transmitterIdBox.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        transmitterIdBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // Write transmitter id to preferences and begin scanning for the bt devices.
                    GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId = transmitterIdBox.getText().toString();
                    GINcoseWrapper.getSharedInstance().saveTransmitterId(MainActivity.this, transmitterIdBox.getText().toString());

                    Log.i("Transmitter", String.format("The TransmitterId has been updated to %s", GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId));

                    startBTManager();
                }
                return false;
            }
        });

        // Check if a transmitterId already exists.
        String storedTransmitterId = GINcoseWrapper.getSharedInstance().getStoredTransmitterId(MainActivity.this);
        if (storedTransmitterId != null) {
            transmitterIdBox.setText(storedTransmitterId);
            GINcoseWrapper.getSharedInstance().defaultTransmitter = new Transmitter(storedTransmitterId);
        }
        else {
            GINcoseWrapper.getSharedInstance().defaultTransmitter = new Transmitter();
        }

        startBTManager();

        mainLayout.setFocusableInTouchMode(true);
        mainLayout.requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // BT Request
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                btManager.setupBluetooth();
            }
            else {
                GINcoseWrapper.getSharedInstance().showPermissionToast(this, "Please allow bluetooth services in order to access bluetooth devices.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                       startBTManager();
                    }
                    else {
                        GINcoseWrapper.getSharedInstance().showPermissionToast(this, "Please allow location services in order to access bluetooth devices.");
                        break;
                    }
                }
            }
        }
    }

    private void startBTManager() {
        if (GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId != null) {
            if (btManager == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!GINcoseWrapper.getSharedInstance().locationPermission()) {
                        requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
                    }
                    else {
                        btManager = new BluetoothManager();
                    }
                }
                else {
                    btManager = new BluetoothManager();
                }
            }
        }
    }
}
