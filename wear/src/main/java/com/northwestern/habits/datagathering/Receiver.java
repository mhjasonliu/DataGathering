package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by William on 2/25/2017
 */

public class Receiver extends BroadcastReceiver {
    private static final String TAG = "Wear+Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Starting BroadcastReceiver...");

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) { // power connected 1
//            WriteDataThread.writeLogs( "Wearable connected to power" + "_" + System.currentTimeMillis(), context );
            Log.d(TAG, "1ACTION_POWER_CONNECTED...BFR");
            context.startActivity(new Intent(context, MainActivity.class));
        }
    }
}
