package com.northwestern.habits.datagathering.necklacedata;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;

public class NecklaceDataService extends Service {

    public static final String START_STREAM_EXTRA = "start stream";
    public static final String DEVICE_EXTRA = "device";
    public static final String PIEZO_EXTRA = "piezo";
    public static final String AUDIO_EXTRA = "audio";

    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";
    public final static UUID UUID_SERVICE = sixteenBitUuid(0x2220);
    public final static UUID UUID_RECEIVE = sixteenBitUuid(0x2221);
    public final static UUID UUID_SEND = sixteenBitUuid(0x2222);
    public final static UUID UUID_DISCONNECT = sixteenBitUuid(0x2223);
    public final static UUID UUID_CLIENT_CONFIGURATION = sixteenBitUuid(0x2902);

    private final static String ACTION_DATA_AVAILABLE =
            "com.rfduino.ACTION_DATA_AVAILABLE";

    private boolean streamPiezo;
    private boolean streamAudio;
    private boolean startStream;
    private BluetoothDevice device;
    private HashMap<String, BluetoothGatt> gatts = new HashMap<>();

    private static final String TAG = "NecklaceDataService";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "Started");

        Bundle extras = intent.getExtras();

        if (extras != null) {
            device = (BluetoothDevice) extras.get(DEVICE_EXTRA);
            streamAudio = extras.getBoolean(AUDIO_EXTRA);
            streamPiezo = extras.getBoolean(PIEZO_EXTRA);
            startStream = extras.getBoolean(START_STREAM_EXTRA);
        }

        if (device != null) {
            Log.v(TAG, "Device is " + device);
            if (!gatts.containsKey(device.getAddress()) && startStream) {
                Log.v(TAG, "Starting stream...");
                gatts.put(device.getAddress(), device.connectGatt(this, true, gattCallback));
                registerReceiver(rfduinoReceiver, getFilter());
            } else if (gatts.containsKey(device.getAddress()) && !startStream) {
                Log.v(TAG, "Stopping stream...");
                gatts.get(device.getAddress()).disconnect();
                Log.v(TAG, "Disconnected");
                gatts.remove(device.getAddress());

            }
        }

        return START_NOT_STICKY;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.v("BluetoothLE ", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    // TODO provide user some warning
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Log.e(TAG, "RFduino GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Log.e(TAG, "RFduino receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "RFduino receive characteristic not found!");
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.v(TAG, "Characteristic read!!!");

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.v(TAG, "Characteristic changed!!!");
            NecklaceEvent event = new NecklaceEvent(characteristic.getValue());
            try {
                String myString = new String(characteristic.getValue(), "UTF-8");
                Log.v(TAG, myString);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };


    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Received a broadcast!!!!");
        }
    };


    IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DATA_AVAILABLE);
        return filter;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    private class UnsubscribeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            return null;
        }
    }

}
