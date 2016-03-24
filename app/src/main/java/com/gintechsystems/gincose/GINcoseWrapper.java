package com.gintechsystems.gincose;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gintechsystems.gincose.messages.AuthRequestTxMessage;
import com.gintechsystems.gincose.messages.AuthStatusRxMessage;

/**
 * Created by joeginley on 3/16/16.
 */
public class GINcoseWrapper extends Application {
    public Activity currentAct = null;

    public Transmitter defaultTransmitter = null;

    public AuthStatusRxMessage authStatus = null;
    public AuthRequestTxMessage authRequest = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("InflateParams")
    /*public void showPermissionToast(Activity act, String message) {
        View toastLayout = act.getLayoutInflater().inflate(R.layout.toast_layout, null);

        TextView toastText = (TextView)toastLayout.findViewById(R.id.toast_text);

        toastText.setTypeface(gothicFont);
        toastText.setText(message);

        Toast toast = new Toast(act);
        toast.setView(toastLayout);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }*/

    public Boolean locationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
