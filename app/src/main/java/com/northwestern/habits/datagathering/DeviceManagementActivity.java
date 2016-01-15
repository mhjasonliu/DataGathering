package com.northwestern.habits.datagathering;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceManagementActivity extends AppCompatActivity {

    private final String TAG = "Device activity";

    public static final String BT_LE_EXTRA = "le";
    public static final String CONT_STUDY_EXTRA = "continueStudy";
    public static final String STUDY_NAME_EXTRA = "studyName";

    private String studyName = "Error: name passed incorrectly";
    private boolean continueStudy = true;

    // Paired Devices
    private ArrayList<String> pairedMacAddresses = new ArrayList<>();
    List<String> leAddressList = new ArrayList<>();
    List<String> leNameList = new ArrayList<>();


    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader = new ArrayList<>();
    HashMap<String, List<String>> listDataChild = new HashMap<>();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Log.v(TAG, "Opened Device Management activity");

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            bluetoothLe = extras.getBoolean(BT_LE_EXTRA);
            continueStudy = extras.getBoolean(CONT_STUDY_EXTRA);
            studyName = extras.getString(STUDY_NAME_EXTRA);
        }

        ((TextView) findViewById(R.id.studyTextView)).setText("Managing Devices for Study:\n" + studyName);


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
                    case 0: {
                        // Microsoft band selected
                        // Start connection management using the mac address
                        Intent connectionIntent = new Intent(DeviceManagementActivity.this,
                                ManageBandConnection.class);
                        connectionIntent.putExtra(ManageBandConnection.INDEX_EXTRA, childPosition);
                        connectionIntent.putExtra(ManageBandConnection.STUDY_NAME_EXTRA, studyName);
                        startActivity(connectionIntent);
                        break;
                    }
                    case 1: {
                        // LE device selected

                        break;
                    }
                    case 2: {
                        // Other bluetooth device selected

                        break;
                    }
                }
                return false;
            }
        });
    }

    public void onRefreshClicked(View view) {
        prepareListData();
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

        leAddressList.clear();
        leNameList.clear();
        scanLeDevice(bluetoothLe);

        List<String> pairedList = new ArrayList<>();
        pairedList.addAll(pairedMacAddresses);

        listDataHeader.clear();
        listDataHeader.add("Microsoft Band 2 (" + Integer.toString(bandList.size()) + ")");
        listDataHeader.add("Low Energy Bluetooth Devices(" + Integer.toString(leAddressList.size()) + ")");
        listDataHeader.add("Other Paired Bluetooth Devices(" + Integer.toString(pairedList.size()) + ")");

        listDataChild.put(listDataHeader.get(0), bandList);
        listDataChild.put(listDataHeader.get(1), leNameList);
        listDataChild.put(listDataHeader.get(2), pairedList);

        listAdapter.notifyDataSetChanged();
    }


    /* ************************** SCANNING FOR BT LE DEVICES **************************** */
    private BluetoothLeScanner mLeScanner = BluetoothConnectionLayer.getAdapter().getBluetoothLeScanner();
    private boolean mScanning = false;
    private Handler mHandler = new Handler();

    private boolean bluetoothLe;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void scanLeDevice(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (BluetoothConnectionLayer.getAdapter().isOffloadedScanBatchingSupported()) {
                if (enable) {
                    new ScanTask().execute(enable);
                    Log.v(TAG, "executed scan task");
                } else {
                    Log.v(TAG, "LE scan was not enabled");
                }
            }
            else
                Log.v(TAG, "Batch not supported");
        }
    }


    private class ScanTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            if (!mScanning) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mLeScanner.stopScan(mLeScanCallback);
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                mLeScanner.startScan(mLeScanCallback);
            }
            return null;
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.v(TAG, "Got batch results from scan for LE devices.");
            Iterator<ScanResult> resIter = results.iterator();
            ScanResult result;
            leAddressList.clear();
            leNameList.clear();
            while (resIter.hasNext()) {
                result = resIter.next();
                leNameList.add(result.getDevice().getName());
                leAddressList.add(result.getDevice().getAddress());
            }

            listDataHeader.set(1, "Low Energy Bluetooth Devices(" + Integer.toString(leAddressList.size()) + ")");
            listDataChild.put(listDataHeader.get(1), leNameList);
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Got single result from scan for LE devices.");
                    Log.v(TAG, "name" + result.getDevice().getName());
                    leNameList.add(result.getDevice().getName());
                    leAddressList.add(result.getDevice().getAddress());
                }
            });

        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.v(TAG, "Scan for LE bluetooth devices failed with error code " + errorCode);
        }
    };

}