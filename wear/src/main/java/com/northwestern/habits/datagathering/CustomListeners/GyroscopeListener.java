package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.DataThreads.GyroWriteDataThread;
import com.northwestern.habits.datagathering.DataThreads.WriteDataThread;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
    private int SENSOR_DELAY_16HZ = 62500;
    private long prevtimestamp= 0;

    public GyroscopeListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccumulator = new DataAccumulator("Gyroscope", 1500);
    }

    private GyroWriteDataThread mWriteDataThread = null;
    public void setWDT(GyroWriteDataThread wdt)
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

    /*public void unRegisterListener1() {
        Log.v(TAG, "unregisterListenerG...");
        mManager.unregisterListener(this);
        isRegistered = false;
    }*/

    int curMin= -1;
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle new gyro value
        if (event == null) return;
        if(prevtimestamp ==event.timestamp) return;
        prevtimestamp = event.timestamp;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
        Calendar c = Calendar.getInstance();
        long timeInMillis = c.getTimeInMillis()
              + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
        //get minute value
        c.setTimeInMillis(timeInMillis);
        int minute = (c.get(Calendar.MINUTE));
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", timeInMillis);
        dataPoint.put("rotX", event.values[0]);
        dataPoint.put("rotY", event.values[1]);
        dataPoint.put("rotZ", event.values[2]);

        if(minute!= curMin)
        {
            //data available for new minute.
            if(curMin==-1)
            {
                //this is first time.
                mAccumulator.putDataPoint(dataPoint, timeInMillis);
            }
            else
            {
                //this is new minute.
                DataAccumulator old = new DataAccumulator(mAccumulator);
                mAccumulator = new DataAccumulator("Gyroscope", 1500); // 1200
                mAccumulator.putDataPoint(dataPoint, timeInMillis);
                mWriteDataThread.mQueue.add(old);
                Log.v(TAG, "Gyro mQueue.add() size " + mWriteDataThread.mQueue.size());
            }
            curMin= minute;
        }
        else
        {
            mAccumulator.putDataPoint(dataPoint, timeInMillis);
        }
//        Log.v(TAG, "G+millis "+timeInMillis);
        //event.timestamp = c.getTimeInMillis()
          //      + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);


        /*if (mAccumulator.putDataPoint(dataPoint, timeInMillis)) { // change
            // Accumulator is full

            // Start a fresh accumulator, preserving the old
            DataAccumulator old = new DataAccumulator(mAccumulator);
            mAccumulator = new DataAccumulator("Gyroscope", 1020); // 1200
            //handleFullAccumulator(old);
            //mWriteDataThread.SaveToFile(old);
            mWriteDataThread.mQueue.add(old);
            Log.v(TAG, "queue size after adding " + mWriteDataThread.mQueue.size() + " recently added ");
        }*/

        /*if (mAccumulator.putDataPoint(dataPoint, event.timestamp)) {
            // Accumulator is full
            // Start a fresh accumulator, preserving the old
            Iterator<Map<String, Object>> oldDataIter = mAccumulator.getIterator();
            mAccumulator = new DataAccumulator("Gyroscope", 192);
            DataAccumulator accumulator = new DataAccumulator("Gyroscope", mAccumulator.getCount());
            while (oldDataIter.hasNext()) {
                Map<String, Object> point = oldDataIter.next();
                accumulator.putDataPoint(point, (long) point.get("Time"));
            }
            handleFullAccumulator(accumulator);
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle change of accuracy if necessary
    }

    private void handleFullAccumulator(DataAccumulator accumulator) {
        // Check if connected to phone
        accumulator.type = "Gyroscope";
        Log.v(TAG, accumulator.type + " " + accumulator.getCount());
//        WriteDataMethods.saveAccumulator(accumulator, mContext);

        mWriteDataThread.SaveToFile(accumulator);

//        Intent intent = new Intent(mContext, WriteDataIService.class);
//        intent.putExtra("buffer", accumulator);
//        mContext.startService(intent);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        WriteDataThread.writeError(e, mContext);
    }
}
