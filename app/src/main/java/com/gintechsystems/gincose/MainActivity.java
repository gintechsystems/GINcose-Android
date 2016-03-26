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

import com.gintechsystems.gincose.bluetooth.BluetoothManager;

/**
 * Created by joeginley on 3/13/16.
 */
public class MainActivity extends Activity {
    private GINcoseWrapper gincoseWrap;
    private BluetoothManager btManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gincoseWrap = (GINcoseWrapper)getApplicationContext();

        gincoseWrap.currentAct = this;

        gincoseWrap.defaultTransmitter = new Transmitter(this, "401Y38");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!gincoseWrap.locationPermission()) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            }
            else {
                btManager = new BluetoothManager(gincoseWrap);
            }
        }
        else {
            btManager = new BluetoothManager(gincoseWrap);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            btManager.setupBluetooth();
        }
        else {
            gincoseWrap.showPermissionToast(this, "Please allow bluetooth services in order to access bluetooth devices.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        btManager = new BluetoothManager(gincoseWrap);
                    }
                    else {
                        gincoseWrap.showPermissionToast(this, "Please allow location services in order to access bluetooth devices.");
                        break;
                    }
                }
            }
        }
    }
}
