package com.northwestern.habits.datagathering.userinterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.northwestern.habits.datagathering.MyReceiver;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.banddata.sensors.BandDataService;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.weardata.WearDataService;

import java.util.Calendar;

public class SplashActivity extends Activity {

    MyReceiver myReceiver = new MyReceiver();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onStartup(this.getApplicationContext());

        // If there is a password set, open to UserActivity, else AdvancedSettings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Intent i;
        if (prefs.getString(Preferences.PASSWORD, "").equals("")) {
            i = new Intent(this, AdvancedSettingsActivity.class);
        } else {
            final IntentFilter mIFNetwork = new IntentFilter();
            mIFNetwork.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION); //"android.net.conn.CONNECTIVITY_CHANGE"
            registerReceiver(myReceiver, mIFNetwork);
            i = new Intent (this, UserActivity.class);
        }

        startActivity(i);
    }

    /**
     * Performs everything needed upon application or phone startup
     */
    public static void onStartup(Context context) {

        // Start any saved band streams
        Intent i = new Intent(context, BandDataService.class);
        context.startService(i);

        // Start replication if plugged in and charging
        if (MyReceiver.isCharging(context) && MyReceiver.isWifiConnected(context)) {
            i = new Intent(context, DataManagementService.class);
            i.setAction(DataManagementService.ACTION_BACKUP);
            context.startService(i);
        }

        // Connect to the wear device
        i = new Intent(context, WearDataService.class);
        context.startService(i);
    }

    @Override
    protected void onDestroy() {
        try{
            if(myReceiver!=null)
                unregisterReceiver(myReceiver);
        }catch(Exception e)
        {

        }
        super.onDestroy();
    }
}
