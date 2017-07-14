package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;

import java.util.Calendar;
import java.util.Date;
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
    private int SENSOR_DELAY_16HZ = 62000;
    private int SENSOR_DELAY_20HZ = 20000;
    private long prevtimestamp= 0;
    private WriteDataThread mWriteDataThread = null;

    public AccelerometerListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelAccumulator = new DataAccumulator("Accelerometer", 192);
    }

    public void setWDT(WriteDataThread wdt)
    {
        mWriteDataThread = wdt;
    }
    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            Log.v(TAG, "Accel+registerListener...");
            boolean bret= mManager.registerListener( this, mSensor, SENSOR_DELAY_16HZ);
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
        if(prevtimestamp == event.timestamp) return;
        prevtimestamp = event.timestamp;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);

        Calendar c = Calendar.getInstance();
        event.timestamp = c.getTimeInMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos());

//        Log.w(TAG, event.sensor.getName() + "+timestamp before calculation" + event.timestamp);
//
//
//        Log.w(TAG, event.sensor.getName() + "+system nanotime " + System.nanoTime());
//        Log.w(TAG, event.sensor.getName() + "+system elapsed nanotime " + SystemClock.elapsedRealtimeNanos());
//
//        Log.w(TAG, event.sensor.getName() + "+absolute time " + (new Date()).getTime());
        Log.w(TAG, event.sensor.getName() + "+timestamp after calculation " + event.timestamp);


        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("accX", event.values[0]);
        dataPoint.put("accY", event.values[1]);
        dataPoint.put("accZ", event.values[2]);

        if (mAccelAccumulator.putDataPoint(dataPoint, event.timestamp)) { // change
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            Iterator<Map<String, Object>> oldDataIter = mAccelAccumulator.getIterator();
            // change check is full
            mAccelAccumulator = new DataAccumulator("Accelerometer", 192); // 1200
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
        Log.v(TAG, "Accel+Accumulator was full " + accumulator.getCount());
        //new WriteDataTask(mContext, accumulator, "Accelerometer").execute();
        accumulator.type="Accelerometer";
        mWriteDataThread.SaveToFile(accumulator);
    }
}
