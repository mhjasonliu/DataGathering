package com.northwestern.habits.datagathering.phonedata;


import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.SensorEvent;

/**
 * Created by William on 1/23/2016
 */
public class HumidityManager extends DataManager {
    public HumidityManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "HumidityManager", db, context);
    }

    @Override
    protected void subscribe() {

    }

    @Override
    protected void unSubscribe() {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }
}
