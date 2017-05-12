package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.SendDataTask;
import com.northwestern.habits.datagathering.WriteDataTask;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by William on 3/1/2017.
 */

public class AccelerometerListener implements SensorEventListener {
    private static final String TAG = "AccelerometerListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccelAccumulator;
    private int SENSOR_DELAY_16HZ = 63;

    public AccelerometerListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelAccumulator = new DataAccumulator("Accelerometer", 100);
    }

    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            mManager.registerListener(this, mSensor, SENSOR_DELAY_16HZ);
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
        // Handle new accel value
        if (event == null) return;

        Calendar c = Calendar.getInstance();
        event.timestamp = c.getTimeInMillis()
                + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;

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
        new WriteDataTask(mContext, accumulator, "Accelerometer").execute();

        new SendDataTask(mContext).execute();
    }
}
