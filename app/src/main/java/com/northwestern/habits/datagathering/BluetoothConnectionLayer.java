package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by William on 10/25/2015.
 * Contains methods and fields for use in managing bluetooth connections
 */
public class BluetoothConnectionLayer implements Serializable {


    /* ******************************* PUBLIC METHODS *********************************** */
    /** Constructor */
    public BluetoothConnectionLayer() {
        establishAdapter();
    }

    /**
     * Lists the paired devices or throws exception if no adapter available
     * @return a list of paired device names
     * @throws ConnectException
     */
    public List<String> pairedDeviceNames() throws ConnectException {
        List<String> s = new ArrayList<>();

        if (adapter != null) {
            Set<BluetoothDevice> deviceSet = adapter.getBondedDevices();
            for (BluetoothDevice bt : deviceSet)
                s.add(bt.getName());
        } else {
            throw new ConnectException();
        }

        return s;
    }


    /**
     * Connects to the adaptor
     * sets error flag NO_ADAPTER if adapter is null
     */
    public void establishAdapter() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        noAdapter = (adapter == null);
    }

    /* ********************************* FIELDS *********************************** */
    /**
     * The phone's bluetooth adapter
     * used for communicating with bluetooth devices
     */
    private BluetoothAdapter adapter;

    /**
     * Error flag for when the addapter is not accessable
     * true if not accessable
     */
    private boolean noAdapter;


    /**
     * accessor for noAdapter
     */
    public boolean hasNoAdapter() {
        return noAdapter;
    }



    /* *********************************** PRIVATE METHODS ********************************* */









}
