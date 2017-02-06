package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class DataService extends Service {
    private static final String TAG = "DataService";

    public DataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "Starting service...");

        initSensors();

        if (!accelIsRegistered) {
            mManager.registerListener(accelListener, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
            accelIsRegistered = true;
        }

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
            if (mAccumulator.addEvent(event)){
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

        private void handleFullAccumulator(DataAccumulator accumulator){
            // Check if connected to phone
            Log.v(TAG, "Accumulator was full");
            new SendDataTask(accumulator.getAsBytes(), DataService.this).execute();
        }
    };
}
