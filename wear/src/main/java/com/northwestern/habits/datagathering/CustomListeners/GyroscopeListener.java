package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.content.Intent;


import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.WriteData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by William on 3/1/2017.
 */

public class GyroscopeListener implements SensorEventListener, Thread.UncaughtExceptionHandler {
    private static final String TAG = "GyroscopeListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccumulator;
    private int SENSOR_DELAY_16HZ = 62000;
    private int SENSOR_DELAY_20HZ = 50000;
    private int SENSOR_DELAY_100HZ = 10000;
    private int BUFFER_SIZE = 200;

    private String TYPE = "Gyroscope";

    public GyroscopeListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccumulator = new DataAccumulator(TYPE, BUFFER_SIZE);
    }

    public void registerListener() {
        if (!isRegistered) {
            isRegistered =  mManager.registerListener( this, mSensor, SENSOR_DELAY_20HZ );
        }
    }

    public void unRegisterListener() {
        if (isRegistered) {
            mManager.unregisterListener(this);
            isRegistered = false;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle new gyro value
        if (event == null) return;

        Calendar c = Calendar.getInstance();
        long time_ms = c.getTimeInMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;

        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", time_ms);
        dataPoint.put("rotX", event.values[0]);
        dataPoint.put("rotY", event.values[1]);
        dataPoint.put("rotZ", event.values[2]);

        if (mAccumulator.putDataPoint(dataPoint, time_ms)) {
            // Accumulator is full
            // Start a fresh accumulator, preserving the old

            DataAccumulator old = new DataAccumulator(mAccumulator);
            mAccumulator = new DataAccumulator(TYPE, BUFFER_SIZE);
            handleFullAccumulator(old);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void handleFullAccumulator(DataAccumulator accumulator) {
        WriteData.requestWrite(mContext, accumulator);

    }

    public void uncaughtException(Thread t, Throwable e) {
        WriteData.logError(mContext, e);
    }
}