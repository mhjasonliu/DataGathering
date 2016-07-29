package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by William on 7/29/2016.
 */
public interface DocIdBroadcastReceiver {
    BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("DocIDReceiver", "Received");
        }
    };

    String ACTION_BROADCAST_CHANGE_ID = "change id";

    IntentFilter _filter = new IntentFilter(ACTION_BROADCAST_CHANGE_ID);

    void registerReceiver();
    void unregisterReceiver();

}
