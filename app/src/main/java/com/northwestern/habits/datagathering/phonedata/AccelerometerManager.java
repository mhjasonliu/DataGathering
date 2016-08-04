package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;


/**
 * Created by William on 1/23/2016
 */
public class AccelerometerManager extends DataManager {

    public AccelerometerManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "AccelerometerManager", db, context);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }


    @Override
    protected void subscribe() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void unSubscribe() {
        mSensorManager.unregisterListener(this, mSensor);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Add to the accelerometer table

        String T_ACCEL = "Accelerometer";
        throw new UnsupportedOperationException("Not implemented");

    }
}
