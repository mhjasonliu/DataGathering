package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class AccelGyroListener implements SensorEventListener {
    private static final String TAG = "AccelGyroListener";

    private Context mContext;
    private Sensor mSensor1;
    private Sensor mSensor2;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccelGyroAccumulator;
    private int SENSOR_DELAY_16HZ = 62000;
    private int SENSOR_DELAY_20HZ = 20000;
    private long prevtimestamp= 0;
    private WriteDataThread mWriteDataThread = null;

    public AccelGyroListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor1 = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor2 = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelGyroAccumulator = new DataAccumulator("AccelGyro", 192);
    }

    public void setWDT(WriteDataThread wdt)
    {
        mWriteDataThread = wdt;
    }
    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            Log.v(TAG, "registerListener...");
            mManager.registerListener(this, mSensor1, SENSOR_DELAY_16HZ);
            mManager.registerListener(this, mSensor2, SENSOR_DELAY_16HZ);

            isRegistered = true;
        }
    }

    public void unRegisterListener() {
        if (isRegistered) {
            Log.v(TAG, "unregisterListener...");
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
        if(prevtimestamp == event.timestamp) return;
        prevtimestamp = event.timestamp;

        event.timestamp = System.currentTimeMillis();
        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);

        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("X", event.values[0]);
        dataPoint.put("Y", event.values[1]);
        dataPoint.put("Z", event.values[2]);

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            dataPoint.put("T", 0);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            dataPoint.put("T", 1);
        }
        else {
            return;
        }

        if (mAccelGyroAccumulator.putDataPoint(dataPoint, event.timestamp)) { // change
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            Iterator<Map<String, Object>> oldDataIter = mAccelGyroAccumulator.getIterator();
            // change check is full
            mAccelGyroAccumulator = new DataAccumulator("AccelGyro", 192); // 1200
            DataAccumulator accumulator = new DataAccumulator("AccelGyro", mAccelGyroAccumulator.getCount());
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
        Log.v(TAG, "Accel+Accumulator was full " + accumulator.getCount());
        //new WriteDataTask(mContext, accumulator, "AccelGyro").execute();
        accumulator.type="AccelGyro";
        mWriteDataThread.SaveToFile(accumulator);
    }
}
