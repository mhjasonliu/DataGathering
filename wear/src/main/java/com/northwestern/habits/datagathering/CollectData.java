package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.RequiresPermission;

import com.northwestern.habits.datagathering.CustomListeners.AccelerometerListener;
import com.northwestern.habits.datagathering.CustomListeners.GyroscopeListener;
import com.northwestern.habits.datagathering.CustomListeners.HeartRateListener;
import com.northwestern.habits.datagathering.CustomListeners.WriteDataThread;

public class CollectData extends Service {
    private SensorManager mManager;
    private AccelerometerListener mAccelListener;
    private GyroscopeListener mGyroListener;
    private HeartRateListener mHeartListener;
    private WriteDataThread wdt;

    public CollectData() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {

        if (mManager == null) {
            mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
            mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
            mHeartListener = new HeartRateListener(getBaseContext(), mManager);

            mAccelListener.registerListener();
            mGyroListener.registerListener();
            mHeartListener.registerListener();

            wdt = new WriteDataThread(getBaseContext());
            wdt.start();
            mAccelListener.setWDT(wdt);
            mGyroListener.setWDT(wdt);
            mHeartListener.setWDT(wdt);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        wdt.setStop();
        mAccelListener.unRegisterListener1();
        mGyroListener.unRegisterListener1();
        mHeartListener.unRegisterListener1();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}