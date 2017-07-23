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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by William on 3/1/2017.
 **/

public class HeartRateListener implements SensorEventListener, Thread.UncaughtExceptionHandler {
    private static final String TAG = "HeartRateListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccumulator;
    private int SENSOR_DELAY_5HZ  = 200000;
    private int SENSOR_DELAY_10HZ = 100000;
    private int BUFFER_SIZE = 20;
    private long prevtimestamp= 0;

    public HeartRateListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mAccumulator = new DataAccumulator("HeartRate", BUFFER_SIZE);
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void registerListener() {
        if (!isRegistered) {
            mManager.registerListener( this, mSensor, SENSOR_DELAY_10HZ );
            isRegistered = true;
        }
    }

    public void unRegisterListener() {
        if (isRegistered) {
            mManager.unregisterListener(this);
            isRegistered = false;
        }
    }

    public void unRegisterListener1() {
        Log.v(TAG, "unregisterListenerH...");
        mManager.unregisterListener(this);
        isRegistered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle new HEART value
        if (event == null) return;
        if(prevtimestamp ==event.timestamp) return;
        prevtimestamp = event.timestamp;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Calendar c = Calendar.getInstance();
        event.timestamp = c.getTimeInMillis();
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("Accuracy", event.accuracy);
        dataPoint.put("Rate", event.values[0]);

        if (mAccumulator.putDataPoint(dataPoint, event.timestamp)) {
            // Accumulator is full
            // Start a fresh accumulator, preserving the old
            DataAccumulator old = new DataAccumulator(mAccumulator);
            mAccumulator = new DataAccumulator("HeartRate", BUFFER_SIZE); // 1200
            handleFullAccumulator(old);
        }
    }
    private WriteDataThread mWriteDataThread = null;
    public void setWDT(WriteDataThread wdt)
    {
        mWriteDataThread = wdt;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle change of accuracy if necessary
    }

    private void handleFullAccumulator(DataAccumulator accumulator) {
        // Check if connected to phone
        accumulator.type = "HeartRate";
//        Log.v(TAG, " count " + accumulator.getCount());
        mWriteDataThread.SaveToFile(accumulator);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        WriteDataThread.writeError(e, mContext);
    }
}
