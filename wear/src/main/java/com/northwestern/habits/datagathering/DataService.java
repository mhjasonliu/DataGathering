package com.northwestern.habits.datagathering;

import android.app.Notification;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class DataService extends WearableListenerService {
    private static final String TAG = "DataService";

    public DataService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Starting service...");
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Data Gathering Wear Data")
                .setContentText("Gathering wear data in foreground service").build();
        startForeground(1, notification);

        initSensors();
        return START_STICKY;
    }


    /* *************************** DATA HANDLING ***************************** */
    // Sensor related fields
    private Sensor mAccel;
    private SensorManager mManager;
    private DataAccumulator mAccumulator;
    /**
     * Maintains accelerometer registration state.
     * Update every time you register/unregister outside of
     * activity lifecycle
     */
    private boolean accelIsRegistered = false;

    private void initSensors() {
        mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccel = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccumulator = new DataAccumulator();
    }

    /**
     * Custom implementation of SensorEventListener specific to what we want to do with
     * accelerometer data.
     */
    private SensorEventListener accelListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Handle new accel value
            Log.v(TAG, "Event REceived");
            if (mAccumulator.addEvent(event)) {
                // Passed the time interval

                // Start a fresh accumulator, preserving the old
                DataAccumulator accumulator = mAccumulator;
                mAccumulator = new DataAccumulator();

                handleFullAccumulator(accumulator);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle change of accuracy if necessary
        }

        private void handleFullAccumulator(DataAccumulator accumulator) {
            // Check if connected to phone
            Log.v(TAG, "Accumulator was full");
            new SendDataTask(accumulator.getAsBytes(), DataService.this).execute();
        }
    };


    /* ***************** MESSAGE RECEIVING *********************** */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent != null) {
            if (messageEvent.getPath().equals("/DataRequest")) {
                String action = new String(messageEvent.getData());
                switch (action) {
                    case "Accelerometer1":
                        Log.v(TAG, "Start accel requested.");
                        if (!accelIsRegistered) {
                            mManager.registerListener(accelListener, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
                            accelIsRegistered = true;
                        }
                        break;
                    case "Accelerometer0":
                        Log.v(TAG, "Stop accel requested.");
                        if (accelIsRegistered) {
                            mManager.unregisterListener(accelListener, mAccel);
                            accelIsRegistered = true;
                        }
                        break;
                    default:
                        Log.w(TAG, "Unknown action received" + action);
                }
            }
        }
    }
}
