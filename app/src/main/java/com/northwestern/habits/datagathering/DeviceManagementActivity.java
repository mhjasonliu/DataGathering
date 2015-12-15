package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;

public class DeviceManagementActivity extends AppCompatActivity {

    private final String TAG = "Device activity";

    // Paired Devices
    private ArrayAdapter<String> pairedListUpdater;
    private ArrayList<String> pairedMacAddresses = new ArrayList<>();

    // Paired Bands
    private ArrayAdapter<String> pairedBandUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Log.v(TAG, "Opened Device Management activity");

        // List of Paired Devices
        ListView pairedDeviceList = (ListView) findViewById(R.id.pairedDeviceListView);

        pairedListUpdater = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedDeviceList.setAdapter(pairedListUpdater);
        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Paired device was selected
                // get the device mac address from the pairedDeviceList
                String macAddress = pairedMacAddresses.get(position);

                // Start connection management using the mac address
                Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                        ManageConnectionActivity.class);
                connectionIntent.putExtra("MAC", macAddress);
                startActivity(connectionIntent);
            }
        });



        // Paired bands
        ListView pairedBandList = (ListView) findViewById(R.id.bandList);

        pairedBandUpdater = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedBandList.setAdapter(pairedBandUpdater);
        pairedBandList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Paired band was selected
                // Start connection management using the mac address
                Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                        ManageBandConnection.class);
                connectionIntent.putExtra("Index", position);
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
        pairedMacAddresses.clear();
        pairedBandUpdater.clear();

        BluetoothConnectionLayer.refreshPairedDevices();

        // Store devices in the list view
        Iterator<BluetoothDevice> devIter =
                BluetoothConnectionLayer.pairedDevices.values().iterator();
        BluetoothDevice curDev;
        while (devIter.hasNext()) {
            curDev = devIter.next();
            if (BluetoothConnectionLayer.bandMap.containsKey(curDev.getAddress())) {
                // Add to list of Bands
                pairedBandUpdater.add(curDev.getName());
                pairedBandUpdater.add(curDev.getAddress());
            } else {
                // Add to list of generic devices
                pairedListUpdater.add(curDev.getName());
                pairedMacAddresses.add(curDev.getAddress());
            }
        }

        pairedBandUpdater.notifyDataSetChanged();
        pairedListUpdater.notifyDataSetChanged();
    }


}
