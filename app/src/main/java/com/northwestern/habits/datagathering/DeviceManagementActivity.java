package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;

import java.util.ArrayList;
import java.util.Iterator;

public class DeviceManagementActivity extends AppCompatActivity {

    private ListView nearbyDeviceList;
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

        // Paired Devices
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

        refreshPairedDevices(findViewById(R.id.refreshButton));


        // Paired bands
        ListView pairedBandList = (ListView) findViewById(R.id.bandList);

        pairedBandUpdater = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        pairedBandList.setAdapter(pairedBandUpdater);
        pairedBandList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Paired band was selected
                // get the band from the list of bands
                BandClient band =
                BandClientManager.getInstance().create(
                        DeviceManagementActivity.this, BluetoothConnectionLayer.pairedBands[position]
                );

                // Start connection management using the mac address
                Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                        ManageConnectionActivity.class);
                connectionIntent.putExtra("MAC", macAddress);
                startActivity(connectionIntent);
            }
        });
    }

    /**
     * Searches for android devices, adding each one to the list view
     * @param view to allow the refresh button to directly call this
     */
    public void refreshPairedDevices(View view) {
        // Setup
        pairedListUpdater.clear();
        pairedMacAddresses.clear();
        BluetoothConnectionLayer.refreshPairedDevices();

        // Store devices in the list view
        Iterator<BluetoothDevice> devIter =
                BluetoothConnectionLayer.pairedDevices.values().iterator();
        BluetoothDevice curDev;
        while (devIter.hasNext()) {
            curDev = devIter.next();
            pairedListUpdater.add(curDev.getName());
            pairedMacAddresses.add(curDev.getAddress());
        }

        pairedListUpdater.notifyDataSetChanged();
    }

}
