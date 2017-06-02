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
 */

public class GyroscopeListener implements SensorEventListener {
    private static final String TAG = "GyroscopeListener";

    private Context mContext;
    private Sensor mSensor;
    private SensorManager mManager;
    private boolean isRegistered = false;
    private DataAccumulator mAccumulator;
    private int SENSOR_DELAY_16HZ = 62000;
    private int SENSOR_DELAY_20HZ = 20000;
    private long prevtimestamp= 0;

    public GyroscopeListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccumulator = new DataAccumulator("Gyroscope", 160);
    }
    private WriteDataThread mWriteDataThread = null;
    public void setWDT(WriteDataThread wdt)
    {
        mWriteDataThread = wdt;
    }
    public boolean isRegistered() { return isRegistered; }

    public void registerListener() {
        if (!isRegistered) {
            mManager.registerListener( this, mSensor, SENSOR_DELAY_16HZ );
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
        if(prevtimestamp ==event.timestamp) return;
        prevtimestamp = event.timestamp;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Calendar c = Calendar.getInstance();
        event.timestamp = c.getTimeInMillis()
                + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", event.timestamp);
        dataPoint.put("rotX", event.values[0]);
        dataPoint.put("rotY", event.values[1]);
        dataPoint.put("rotZ", event.values[2]);

        if (mAccumulator.putDataPoint(dataPoint, event.timestamp)) {
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            Iterator<Map<String, Object>> oldDataIter = mAccumulator.getIterator();
            mAccumulator = new DataAccumulator("Gyroscope", 160);
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
        Log.v(TAG, "Gyro+Accumulator was full " + accumulator.getCount());
        //new WriteDataTask(mContext, accumulator, "Gyroscope").execute();
        accumulator.type="Gyroscope";
        mWriteDataThread.SaveToFile(accumulator);

//        new SendDataTask(mContext).execute();
    }
}
