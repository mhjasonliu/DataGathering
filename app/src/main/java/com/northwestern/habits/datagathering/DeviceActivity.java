package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

public class DeviceActivity extends AppCompatActivity {

    private List<String> deviceList;
    private BluetoothConnectionLayer myBluetooth;
    private ArrayAdapter<String> listUpdater;
    private final String TAG = "Device activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        Log.v(TAG, "Opened new activity");
        // Receive the passed bluetooth layer
        Intent intent = getIntent();
        myBluetooth = (BluetoothConnectionLayer)
                intent.getSerializableExtra("bluetoothLayer");


        // Needed in order to update the listview
        ListView theList = (ListView) findViewById(R.id.deviceListView);

        deviceList = new ArrayList<>();

        listUpdater = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1,
                deviceList);
        theList.setAdapter(listUpdater);

        refreshDevices(findViewById(R.id.refreshButton));
    }


    /**
     * Searches for android devices, adding each one to the list view
     * @param view to allow the refresh button to directly call this
     */
    public void refreshDevices(View view) {
        try {
            deviceList = myBluetooth.pairedDeviceNames();
        } catch (ConnectException e) {
            // There is no adapter. Set the list to say so
            deviceList.clear();
            deviceList.add("Bluetooth not enabled");
        }
        listUpdater.notifyDataSetChanged();
    }

    /**
     * Allows user to select a device
     */
}
