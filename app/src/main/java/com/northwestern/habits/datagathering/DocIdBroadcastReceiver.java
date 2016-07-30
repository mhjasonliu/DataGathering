package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by William on 7/29/2016.
 */
public interface DocIdBroadcastReceiver {
    String TAG = "DocIDReceiver";
    BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Delete the current reference to docID
            SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
            if (intent.hasExtra(NEW_ID_EXTRA)) {
                prefs.putString(Preferences.CURRENT_DOCUMENT, intent.getStringExtra(NEW_ID_EXTRA));
            } else {
                prefs.remove(Preferences.CURRENT_DOCUMENT);
            }
            prefs.apply();
        }
    };

    String ACTION_BROADCAST_CHANGE_ID = "change id";

    String NEW_ID_EXTRA = "new id";

    IntentFilter _filter = new IntentFilter(ACTION_BROADCAST_CHANGE_ID);

    void registerReceiver();
    void unregisterReceiver();

}
