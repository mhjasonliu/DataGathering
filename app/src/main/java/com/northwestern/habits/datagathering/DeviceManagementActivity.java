package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceManagementActivity extends AppCompatActivity {

    private ListView nearbyDeviceList;
    private ArrayAdapter<String> pairedListUpdater;
    private ArrayList<String> macAddresses = new ArrayList<>();
    private final String TAG = "Device activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Log.v(TAG, "Opened Device Management activity");

        // Find the list views
        ListView pairedDeviceList = (ListView) findViewById(R.id.pairedDeviceListView);
        // nearbyDeviceList = (ListView) findViewById(R.id.nearbyDeviceList);

        pairedListUpdater = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedDeviceList.setAdapter(pairedListUpdater);
        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Paired device was selected
                // get the device mac address from the pairedDeviceList
                String macAddress = macAddresses.get(position);

                // Start connection management using the mac address
                Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                        ManageConnectionActivity.class);
                connectionIntent.putExtra("MAC", macAddress);
                startActivity(connectionIntent);
            }
        });

        refreshPairedDevices(findViewById(R.id.refreshButton));
    }


    /**
     * Searches for android devices, adding each one to the list view
     * @param view to allow the refresh button to directly call this
     */
    public void refreshPairedDevices(View view) {
        // Setup
        pairedListUpdater.clear();
        macAddresses.clear();
        BluetoothConnectionLayer.refreshPairedDevices();

        // Store devices in the list view
        Iterator<BluetoothDevice> devIter =
                BluetoothConnectionLayer.pairedDevices.values().iterator();
        BluetoothDevice curDev;
        while (devIter.hasNext()) {
            curDev = devIter.next();
            pairedListUpdater.add(curDev.getName());
            macAddresses.add(curDev.getAddress());
        }

        pairedListUpdater.notifyDataSetChanged();
    }

}
