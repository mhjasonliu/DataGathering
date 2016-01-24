package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorEvent;

/**
 * Created by William on 1/23/2016
 */
public class LinearAccelManager extends DataManager {
    public LinearAccelManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "LinearAccelManager", db, context);
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
