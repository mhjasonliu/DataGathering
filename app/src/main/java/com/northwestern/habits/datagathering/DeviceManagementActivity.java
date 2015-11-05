package com.northwestern.habits.datagathering;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeviceManagementActivity extends AppCompatActivity {

    private ListView pairedDeviceList;
    private ListView nearbyDeviceList;
    private ArrayAdapter<String> pairedListUpdater;
    private final String TAG = "Device activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Log.v(TAG, "Opened Device Management activity");

        // Find the list views
        pairedDeviceList = (ListView) findViewById(R.id.pairedDeviceListView);
        // nearbyDeviceList = (ListView) findViewById(R.id.nearbyDeviceList);

        pairedListUpdater = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedDeviceList.setAdapter(pairedListUpdater);

        refreshPairedDevices(findViewById(R.id.refreshButton));
    }


    /**
     * Searches for android devices, adding each one to the list view
     * @param view to allow the refresh button to directly call this
     */
    public void refreshPairedDevices(View view) {
        // Setup
        pairedListUpdater.clear();
        BluetoothConnectionLayer.refreshPairedDevices();

        // Store devices in the list view
        Iterator<String> nameIter = BluetoothConnectionLayer.pairedDeviceNames.iterator();
        while (nameIter.hasNext()) {
            pairedListUpdater.add(nameIter.next());
        }

        pairedListUpdater.notifyDataSetChanged();
    }


    /**
     * Allows user to select a device
     */
}
