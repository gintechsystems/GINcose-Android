package com.gintechsystems.gincose;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.gintechsystems.gincose.bluetooth.BluetoothManager;

/**
 * Created by joeginley on 3/13/16.
 */
public class MainActivity extends DrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        GINcoseWrapper.getSharedInstance().currentAct = this;

        final EditText transmitterIdBox = (EditText)findViewById(R.id.transmitter_id_box);

        // Removes annoying may produce NullPointerException.
        assert transmitterIdBox != null;

        // Enable all caps and a max length of a transmitter id, 6.
        transmitterIdBox.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)});
        transmitterIdBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (transmitterIdBox.getText().length() == 0) {
                        return true;
                    }

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

            GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterAddress = GINcoseWrapper.getSharedInstance().getStoredTransmitterAddress(this);
            GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterName = GINcoseWrapper.getSharedInstance().getStoredTransmitterName(this);
        }
        else {
            GINcoseWrapper.getSharedInstance().defaultTransmitter = new Transmitter();

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        startBTManager();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display the unbond message when our user returns to our app if forgetting the device.
        if (GINcoseWrapper.getSharedInstance().showUnbondedMessage) {
            GINcoseWrapper.getSharedInstance().showUnbondedMessage = false;

            String transmitterName = GINcoseWrapper.getSharedInstance().getStoredTransmitterName(this);

            if (transmitterName != null) {
                GINcoseWrapper.getSharedInstance().showPermissionToast(GINcoseWrapper.getSharedInstance().currentAct, transmitterName + " has been unpaired.");

                GINcoseWrapper.getSharedInstance().removeStoredKey(this, "defaultTransmitterAddress");
                GINcoseWrapper.getSharedInstance().removeStoredKey(this, "defaultTransmitterName");
            }
            else {
                GINcoseWrapper.getSharedInstance().showPermissionToast(GINcoseWrapper.getSharedInstance().currentAct, "A transmitter has been unpaired.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        GINcoseWrapper.getSharedInstance().doStopBTManagerOnExit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // BT Request
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                GINcoseWrapper.getSharedInstance().btManager.setupBluetooth();
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
            if (GINcoseWrapper.getSharedInstance().btManager == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!GINcoseWrapper.getSharedInstance().locationPermission()) {
                        requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
                    }
                    else {
                        GINcoseWrapper.getSharedInstance().btManager = new BluetoothManager();
                    }
                }
                else {
                    GINcoseWrapper.getSharedInstance().btManager = new BluetoothManager();
                }
            }
        }
    }

}