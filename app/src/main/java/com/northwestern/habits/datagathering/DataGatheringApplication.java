package com.northwestern.habits.datagathering;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by William on 5/11/2016.
 * Class for maintaining application state
 */
public class DataGatheringApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static DataGatheringApplication ourInstance = new DataGatheringApplication();
    private Manager manager;
    private Database db;
    private static String TAG = "Application";

    public static DataGatheringApplication getInstance() {
        return ourInstance;
    }

    public Database getDatabase() {
        return db;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ourInstance = this;
        Thread.setDefaultUncaughtExceptionHandler(this);

        createDatabase(getApplicationContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private void handleUncaughtException(Throwable e) {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        Intent intent = new Intent();
        intent.setAction("com.northwestern.habits.datagathering.SEND_LOG"); // see step 5.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity(intent);

        System.exit(1); // kill off the crashed app
    }


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        handleUncaughtException(ex);
    }

    /* ********************************* MANAGE THE DATABASE ********************************* */
    public static String getFilePath(Context context, int hour) {
        return context.getFilesDir().getPath()
                + "/hour_" + Integer.toString(hour) + "_data";
    }

    public static String getDataFileName(String dataType, int hour, String date, String devType, String mac) {
        return dataType + "_" + Integer.toString(hour) + "_" + date +
                "_" + devType + "_" + mac.replace(":", ".") + ".csv";
    }

    public static int getHourFromTimestamp(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static String getDateFromTimestamp(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(c.getTime());
    }

    private void createDatabase(Context context) {
        try {
            this.manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            this.db = manager.getDatabase("data_gathering_db");
            db.setMaxRevTreeDepth(1);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create database", e);
        } catch (CouchbaseLiteException e1) {
            e1.printStackTrace();
        }
    }


    /* ********************************* MANAGE THE SERVICE ********************************** */
    public void registerActivity(Activity activity) {

    }

    void unregisterActivity(Activity activity) {

    }
}
