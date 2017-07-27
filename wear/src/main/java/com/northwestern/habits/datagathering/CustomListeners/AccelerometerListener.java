package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.WriteData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by William on 3/1/2017.
 */

public class AccelerometerListener implements SensorEventListener, Thread.UncaughtExceptionHandler {
    private static final String TAG = "AccelerometerListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccelAccumulator;
    //    private int SENSOR_DELAY_16HZ = 62000;
    private int SENSOR_DELAY_20HZ = 50000;
    private int SENSOR_DELAY_100HZ = 10000;
    private int BUFFER_SIZE = 200;

    public AccelerometerListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelAccumulator = new DataAccumulator("Accelerometer", BUFFER_SIZE);
    }

    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            Log.v(TAG, "Accel+registerListener...");
            boolean bret= mManager.registerListener( this, mSensor, SENSOR_DELAY_100HZ);
            isRegistered = true;
        }
    }

    public void unRegisterListener() {
        if (isRegistered) {
            Log.v(TAG, "Accel+unregisterListener...");
            mManager.unregisterListener(this);
            isRegistered = false;
        }
    }

    public void unRegisterListener1() {
        Log.v(TAG, "unregisterListenerA...");
        mManager.unregisterListener(this);
        isRegistered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle new accel value
        if (event == null) return;

        Calendar c = Calendar.getInstance();
        event.timestamp = c.getTimeInMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("accX", event.values[0]);
        dataPoint.put("accY", event.values[1]);
        dataPoint.put("accZ", event.values[2]);

        if (mAccelAccumulator.putDataPoint(dataPoint, event.timestamp)) { // change
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            DataAccumulator old = new DataAccumulator(mAccelAccumulator);
            mAccelAccumulator = new DataAccumulator("Accelerometer", BUFFER_SIZE); // 1200
            handleFullAccumulator(old);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle change of accuracy if necessary
    }

    private void handleFullAccumulator(DataAccumulator accumulator) {
        WriteData.requestWrite(mContext, accumulator);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
    }
}