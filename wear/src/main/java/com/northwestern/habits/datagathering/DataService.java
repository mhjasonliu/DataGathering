package com.northwestern.habits.datagathering;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DataService extends WearableListenerService implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "DataService";

    public DataService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Starting service...");
        Thread.setDefaultUncaughtExceptionHandler(this);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Data Gathering Wear Data")
                .setContentText("Gathering wear data in foreground service").build();
        startForeground(1, notification);

        initSensors();
        registerSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));

        return START_STICKY;
    }

    private void registerSensors(Set<String> sensors) {
        for (String sensor : sensors) {
            switch (sensor) {
                case Preferences.SENSOR_ACCEL:
                    if (!accelIsRegistered) {
                        mManager.registerListener(accelListener, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
                        accelIsRegistered = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown sensor requested: " + sensor);
            }
        }
    }

    /* *************************** DATA HANDLING ***************************** */
    // Sensor related fields
    private SensorManager mManager;
    private Sensor mAccel;
    private DataAccumulator mAccelAccumulator;
    /**
     * Maintains accelerometer registration state.
     * Update every time you register/unregister outside of
     * activity lifecycle
     */
    private boolean accelIsRegistered = false;

    private void initSensors() {
        mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccel = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelAccumulator = new DataAccumulator("Accelerometer", 100);
    }

    /**
     * Custom implementation of SensorEventListener specific to what we want to do with
     * accelerometer data.
     */
    private SensorEventListener accelListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Handle new accel value
            if (event == null) return;

            event.timestamp = System.currentTimeMillis();

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("Time", event.timestamp);
            dataPoint.put("accX", event.values[0]);
            dataPoint.put("accY", event.values[1]);
            dataPoint.put("accZ", event.values[2]);

            if (mAccelAccumulator.putDataPoint(dataPoint, event.timestamp)) {
                // Accumulator is full

                // Start a fresh accumulator, preserving the old
                Iterator<Map<String, Object>> oldDataIter = mAccelAccumulator.getIterator();
                mAccelAccumulator = new DataAccumulator("Accelerometer", 100);
                DataAccumulator accumulator = new DataAccumulator("Accelerometer", mAccelAccumulator.getCount());
                while (oldDataIter.hasNext()) {
                    Map<String, Object> point = oldDataIter.next();
                    accumulator.putDataPoint(point, (long) point.get("Time"));
                }

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
            new WriteDataTask(getBaseContext(), accumulator, "Accelerometer").execute();

            new SendDataTask(getBaseContext()).execute();
        }
    };


    /* ***************** MESSAGE RECEIVING *********************** */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (mManager == null) {
            initSensors();
        }

        if (messageEvent != null) {
            if (messageEvent.getPath().equals("/DataRequest")) {
                SharedPreferences prefs = getSharedPreferences(Preferences.PREFERENCE_NAME, MODE_PRIVATE);
                Set<String> activeSensors = new HashSet<>(prefs.getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));

                String action = new String(messageEvent.getData());
                switch (action) {
                    case "Accelerometer1":
                        Log.v(TAG, "Start accel requested.");
                        if (!accelIsRegistered) {
                            mManager.registerListener(accelListener, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
                            accelIsRegistered = true;
                        }
                        activeSensors.add(Preferences.SENSOR_ACCEL);
                        break;
                    case "Accelerometer0":
                        Log.v(TAG, "Stop accel requested.");
                        if (accelIsRegistered) {
                            mManager.unregisterListener(accelListener, mAccel);
                            accelIsRegistered = true;
                        }
                        activeSensors.remove(Preferences.SENSOR_ACCEL);
                        break;
                    default:
                        Log.w(TAG, "Unknown action received" + action);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Preferences.KEY_PHONE_ID, messageEvent.getSourceNodeId());
                editor.putStringSet(Preferences.KEY_ACTIVE_SENSORS, activeSensors);
                Log.v(TAG, "Active sensors: " + activeSensors);
                editor.commit();

                Log.v(TAG, getSharedPreferences(Preferences.PREFERENCE_NAME, 0).getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()).toString());
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        WriteDataTask.writeError(e, getBaseContext());
    }
}
