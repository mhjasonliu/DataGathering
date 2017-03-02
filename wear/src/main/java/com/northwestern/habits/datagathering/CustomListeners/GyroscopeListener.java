package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.SendDataTask;
import com.northwestern.habits.datagathering.WriteDataTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by William on 3/1/2017.
 */

public class GyroscopeListener implements SensorEventListener {
    private static final String TAG = "GyroscopeListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccumulator;

    public GyroscopeListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccumulator = new DataAccumulator("Gyroscope", 100);
    }

    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            mManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isRegistered = true;
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

        event.timestamp = (new Date()).getTime()
                + (event.timestamp - System.nanoTime()) / 1000000L;

        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("rotX", event.values[0]);
        dataPoint.put("rotY", event.values[1]);
        dataPoint.put("rotZ", event.values[2]);

        if (mAccumulator.putDataPoint(dataPoint, event.timestamp)) {
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            Iterator<Map<String, Object>> oldDataIter = mAccumulator.getIterator();
            mAccumulator = new DataAccumulator("Gyroscope", 100);
            DataAccumulator accumulator = new DataAccumulator("Gyroscope", mAccumulator.getCount());
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
        new WriteDataTask(mContext, accumulator, "Gyroscope").execute();

        new SendDataTask(mContext).execute();
    }
}
