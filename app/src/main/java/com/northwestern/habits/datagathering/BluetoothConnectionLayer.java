package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by William on 10/25/2015.
 * Contains methods and fields for use in managing bluetooth connections
 */
public class BluetoothConnectionLayer {


    private final String TAG = "Bluetooth Connection";

    /* ******************************* PUBLIC METHODS *********************************** */

    /**
     * Refreshes the set pairedDevices, updating the set of paired device names
     */
    public static void refreshPairedDevices() {
        BluetoothAdapter adapter = getAdapter();

        if (adapter != null) {
            // Get the list of devices and update pairedDevices
            Iterator<BluetoothDevice> devIter = adapter.getBondedDevices().iterator();
            BluetoothDevice curDevice;
            pairedDevices.clear();
            while (devIter.hasNext()) {
                curDevice = devIter.next();
                pairedDevices.put(curDevice.getAddress(), curDevice);
            }
        } else {
            // No connection, clear everything
            pairedDevices.clear();
        }

    }

    /**
     * Refreshes the array pairedBands and the hashmap bandMap
     */
    public static void refreshPairedBands() {
        // Refresh Bands
        BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();

        for (BandInfo curBand :  pairedBands) {
            bandMap.put(curBand.getMacAddress(), curBand);
        }
    }

    public static void refreshNearbyDevices() {
        // TODO implement this method
    }



    /* ********************************* FIELDS *********************************** */
    /**
     * The phone's bluetooth adapter
     * used for communicating with bluetooth devices
     */
    protected static BluetoothAdapter getAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }


    /**
     * Set of all paired devices
     * Keys are the mac addresses of devices
     */
    protected static HashMap<String, BluetoothDevice> pairedDevices = new HashMap<>();

    /**
     * Map of MAC address to band info
     */
    protected static HashMap<String, BandInfo> bandMap = new HashMap<>();

    /**
     * Array of paired bands
     */
    //protected static BandInfo[] pairedBands;

}
