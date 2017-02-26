package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by William on 2/25/2017
 */

public class Receiver extends BroadcastReceiver {
    private static final String TAG = "habitsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, DataService.class);
            context.startService(i);
        }
    }
}
