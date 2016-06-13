package com.northwestern.habits.datagathering;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;

/**
 * Created by William on 5/11/2016.
 * Class for maintaining application state
 */
public class DataGatheringApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static DataGatheringApplication ourInstance = new DataGatheringApplication();

    public static DataGatheringApplication getInstance() { return ourInstance; }
    public static final String PREFS_NAME = "my_prefs";
    public static final String PREF_USER_ID = "userID";

    @Override
    public void onCreate() {
        super.onCreate();
        ourInstance = this;
        Thread.setDefaultUncaughtExceptionHandler(this);
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

    private void handleUncaughtException (Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        Intent intent = new Intent ();
        intent.setAction ("com.northwestern.habits.datagathering.SEND_LOG"); // see step 5.
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity (intent);

        System.exit(1); // kill off the crashed app
    }

    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        handleUncaughtException(ex);
    }
}
