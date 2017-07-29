package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by William on 2/25/2017
 */

public class RestartReceiver extends BroadcastReceiver {
    private static final String TAG = "RestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Starting BroadcastReceiver...");

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) { // power connected 1
            context.startActivity(new Intent(context, MainActivity.class));
        }
    }
}
