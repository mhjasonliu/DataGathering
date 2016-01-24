package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;


/**
 * Created by William on 1/23/2016
 */
public class AccelerometerManager extends DataManager {

    public AccelerometerManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "AccelerometerManager", db, context);
    }


    @Override
    protected void subscribe() {

    }

    @Override
    protected void unSubscribe() {

    }


}
