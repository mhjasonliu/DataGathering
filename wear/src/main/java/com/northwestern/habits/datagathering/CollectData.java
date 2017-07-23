package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;

import com.northwestern.habits.datagathering.CustomListeners.AccelerometerListener;
import com.northwestern.habits.datagathering.CustomListeners.GyroscopeListener;
import com.northwestern.habits.datagathering.CustomListeners.HeartRateListener;

public class CollectData extends Service {
    private SensorManager mManager;
    private AccelerometerListener mAccelListener;
    private GyroscopeListener mGyroListener;
    private HeartRateListener mHeartListener;


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
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mAccelListener.unRegisterListener1();
        mGyroListener.unRegisterListener1();
        mHeartListener.unRegisterListener1();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
