package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceManagementActivity extends AppCompatActivity {

    private final String TAG = "Device activity";

    // Paired Devices
    private ArrayList<String> pairedMacAddresses = new ArrayList<>();

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader = new ArrayList<>();
    HashMap<String, List<String>> listDataChild = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Log.v(TAG, "Opened Device Management activity");

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.deviceListView);


        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);
        // preparing list data
        prepareListData();

        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                switch (groupPosition) {
                    case 0:{
                        // Microsoft band selected
                        // Start connection management using the mac address
                        Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                                ManageBandConnection.class);
                        connectionIntent.putExtra(ManageBandConnection.INDEX_EXTRA, childPosition);
                        startActivity(connectionIntent);
                        break;
                    }case 1:{
                        // LE device selected

                        break;
                    }case 2:{
                        // Other bluetooth device selected

                        break;
                    }
                }
                return false;
            }
        });
    }


    /**
     * Searches for bluetooth devices, adding each one to the list view
     * @param view to allow the refresh button to directly call this
     */
    public void refreshPairedDevices(View view) {

    }

    /*
      * Preparing the list data
      */
    private void prepareListData() {
        // Adding child data

        // Adding child data
        List<String> bandList = new ArrayList<>();
        BluetoothConnectionLayer.refreshPairedBands();
        bandList.addAll(BluetoothConnectionLayer.bandMap.keySet());


        BluetoothConnectionLayer.refreshPairedDevices();
        pairedMacAddresses.clear();
        // Store devices in the list view
        Iterator<BluetoothDevice> devIter =
                BluetoothConnectionLayer.pairedDevices.values().iterator();
        BluetoothDevice curDev;
        while (devIter.hasNext()) {
            curDev = devIter.next();
            if (!BluetoothConnectionLayer.bandMap.containsKey(curDev.getAddress())) {
                // Add to list of generic devices
                pairedMacAddresses.add(curDev.getName());
            }
        }

        List<String> leList = new ArrayList<>();
        //TODO leList = getLeConnections();

        List<String> pairedList = new ArrayList<>();
        pairedList.addAll(pairedMacAddresses);


        listDataHeader.add("Microsoft Band 2 (" + Integer.toString(bandList.size()) + ")");
        listDataHeader.add("Low Energy Bluetooth Devices(" + Integer.toString(leList.size()) + ")");
        listDataHeader.add("Other Paired Bluetooth Devices(" + Integer.toString(pairedList.size()) + ")");

        listDataChild.put(listDataHeader.get(0), bandList);
        listDataChild.put(listDataHeader.get(1), leList);
        listDataChild.put(listDataHeader.get(2), pairedList);

        listAdapter.notifyDataSetChanged();
    }

//
//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//
////                            leDeviceList.addDevice(device);
////                            leDeviceList.notifyDataSetChanged();
//                        }
//                    });
//                }
//            };
}
