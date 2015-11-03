package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    /**
     * Connection layer for this app's bluetooth activities
     */
    public BluetoothConnectionLayer myBluetooth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



    /******************************* BUTTON HANDLERS *******************************/
    private final String TAG = "Main activity"; //For logs

    public void manageDevicesClicked(View view) {
        Log.v(TAG, "Device Management button clicked");

        // Check for bluetooth connection
        BluetoothAdapter adapter = BluetoothConnectionLayer.getAdapter();
        if (adapter == null) {
            //TODO popup warning and cancel the activity
        } else {
            BluetoothConnectionLayer.refreshPairedDevices();
            BluetoothConnectionLayer.refreshNearbyDevices();
            Intent newActivityIntent = new Intent(
                    this, DeviceManagementActivity.class
            );
            startActivity(newActivityIntent);
        }

    }



    /********************* STRUCTURES FOR BLUETOOTH DEVICES ************************/



}
