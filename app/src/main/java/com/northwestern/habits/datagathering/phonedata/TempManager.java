package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by William on 1/23/2016
 */
public class TempManager extends DataManager {
    public TempManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "TempManager", db, context);
    }

    @Override
    protected void subscribe() {

    }

    @Override
    protected void unSubscribe() {

    }
}
