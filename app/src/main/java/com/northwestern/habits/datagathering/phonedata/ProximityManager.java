package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorEvent;

/**
 * Created by William on 1/23/2016
 */
public class ProximityManager extends DataManager {
    public ProximityManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "ProximityManager", db, context);
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
