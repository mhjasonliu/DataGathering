package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.util.ArraySet;
import android.util.Log;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import java.io.Serializable;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
     * Refreshes the array pairedBands
     */
    public static void refreshPairedBands() {
        // Refresh Bands
        pairedBands = BandClientManager.getInstance().getPairedBands();
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
     * Array of paired bands
     */
    protected static BandInfo[] pairedBands;










    /* *********************************** PRIVATE METHODS ********************************* */









}
