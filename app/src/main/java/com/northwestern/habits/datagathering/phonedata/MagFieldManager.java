package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by William on 1/23/2016
 */
public class MagFieldManager extends DataManager {
    public MagFieldManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "MagFieldManager", db, context);
    }

    @Override
    protected void subscribe() {

    }

    @Override
    protected void unSubscribe() {

    }
}
