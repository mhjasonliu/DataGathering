package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;
import com.northwestern.habits.datagathering.DataThreads.WriteDataThread;

import java.util.Calendar;
import java.util.HashMap;
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
    private DataAccumulator mAccumulator;
    private int SENSOR_DELAY_16HZ = 62500;
    private long prevtimestamp= 0;
    private WriteDataThread mWriteDataThread = null;

    public AccelerometerListener(Context context, SensorManager manager) {
        mContext = context;
        mManager = manager;
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccumulator = new DataAccumulator("Accelerometer", 1500);
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

    /*public void unRegisterListener1() {
        Log.v(TAG, "unregisterListenerA...");
        mManager.unregisterListener(this);
        isRegistered = false;
    }*/

    int curMin= -1;
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle new accel value
        if (event == null) return;
        if(prevtimestamp == event.timestamp) return;
        prevtimestamp = event.timestamp;
//        Log.v(TAG, event.sensor.getName() + "+Accumulator at " + event.timestamp);
//        Calendar c = Calendar.getInstance();
//        event.timestamp = c.getTimeInMillis();
        Calendar c = Calendar.getInstance();

        long timeInMillis = c.getTimeInMillis()
                + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
        //get minute value
        c.setTimeInMillis(timeInMillis);
        int minute = (c.get(Calendar.MINUTE));
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("Time", timeInMillis);
        dataPoint.put("accX", event.values[0]);
        dataPoint.put("accY", event.values[1]);
        dataPoint.put("accZ", event.values[2]);

        if(minute != curMin) {
            //data available for new minute.
            if(curMin==-1) {
                //this is first time.
                mAccumulator.putDataPoint(dataPoint, timeInMillis);
            } else {
                //this is new minute.
                DataAccumulator old = new DataAccumulator(mAccumulator);
                mAccumulator = new DataAccumulator("Accelerometer", 1500); // 1200
                mAccumulator.putDataPoint(dataPoint, timeInMillis);
                mWriteDataThread.mQueue.add(old);
                Log.v(TAG, "Accel mQueue.add() size " + mWriteDataThread.mQueue.size());
            }
            curMin= minute;
        } else {
            mAccumulator.putDataPoint(dataPoint, timeInMillis);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle change of accuracy if necessary
    }

    private void handleFullAccumulator(DataAccumulator accumulator) {
        // Check if connected to phone
        Log.v(TAG, accumulator.type + " " + accumulator.getCount());
        //new WriteDataTask(mContext, accumulator, "Accelerometer").execute();
        accumulator.type = "Accelerometer";
//        WriteDataMethods.saveAccumulator(accumulator, mContext);

        mWriteDataThread.SaveToFile(accumulator);

//        Intent intent = new Intent(mContext.getApplicationContext(), WriteDataIService.class);
//        intent.putExtra("buffer", accumulator);
//        mContext.getApplicationContext().startService(intent);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        WriteDataThread.writeError(e, mContext);
    }
}
