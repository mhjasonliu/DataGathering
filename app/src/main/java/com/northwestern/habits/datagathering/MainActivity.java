package com.northwestern.habits.datagathering;

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


        // Set up bluetooth
        myBluetooth = new BluetoothConnectionLayer();
        myBluetooth.establishAdapter();

    }



    /******************************* BUTTON HANDLERS *******************************/
    private final String TAG = "Main activity"; //For logs

    public void connectClicked(View view) {
        Log.v(TAG, "Connect button clicked");

        // Open a connection activity
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra("bluetoothLayer", myBluetooth);
        // Give it the list of connected devices


        startActivity(intent);
    }




    /********************* STRUCTURES FOR BLUETOOTH DEVICES ************************/



}
