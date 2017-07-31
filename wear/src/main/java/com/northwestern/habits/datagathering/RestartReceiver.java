package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by William on 2/25/2017
 */

public class RestartReceiver extends BroadcastReceiver {
    private static final String TAG = "RestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Starting BroadcastReceiver...");

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startActivity(new Intent(context, MainActivity.class));
        }
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            if (context != null) {
                context.stopService(new Intent(context, DataService.class));
            }
        }
        if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            if (context != null) {
                context.startService(new Intent(context, DataService.class));
            }
        }
    }
}
