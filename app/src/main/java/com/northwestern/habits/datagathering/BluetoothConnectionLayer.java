package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.ArraySet;

import java.io.Serializable;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by William on 10/25/2015.
 * Contains methods and fields for use in managing bluetooth connections
 */
public class BluetoothConnectionLayer {


    /* ******************************* PUBLIC METHODS *********************************** */

    /**
     * Refreshes the set pairedDevices, updating the set of paired device names
     */
    public static void refreshPairedDevices() {
        BluetoothAdapter adapter = getAdapter();

        if (adapter != null) {
            // Get the list of devices and update pairedDevices
            pairedDevices = adapter.getBondedDevices();

            // Update names of paired devices
            pairedDeviceNames.clear();
            for (BluetoothDevice device : pairedDevices)
                pairedDeviceNames.add(device.getName());

        } else {
            // No connection, clear everything
            pairedDeviceNames = new LinkedHashSet<>();
            pairedDevices = new LinkedHashSet<>();
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
     * Set of paired device names
     */
    protected static Set<String> pairedDeviceNames = new HashSet<>();

    /**
     * Set of all paired devices
     */
    protected static Set<BluetoothDevice> pairedDevices = new HashSet<>();

    /**
     * Set of connections
     */




    /* *********************************** PRIVATE METHODS ********************************* */









}
