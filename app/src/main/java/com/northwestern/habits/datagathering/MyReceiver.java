package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;

import com.northwestern.habits.datagathering.banddata.BandDataService;

/**
 * Created by William on 7/11/2016.
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    boolean shouldReplicateDatabase() {
        return isCharging && isWifiConnected;
    }

    boolean isCharging = false;
    boolean isWifiConnected = false;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            ConnectivityManager connManager;
            NetworkInfo network;
            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    Log.v(TAG, "Power connected action received.");
                    isCharging = true;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.v(TAG, "Power connected action received.");
                    isCharging = false;
                    break;

                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    Log.v(TAG, "Wifi action received");
                    isWifiConnected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                            false);
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    // Check for charging
                    Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (i != null) {
                        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                        isCharging = (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
                    }
                    context.startService(new Intent(context, BandDataService.class));
                    break;
                default:
                    Log.e(TAG, "Unknown type sent to receiver: " + intent.getAction());
            }
        }
    }
}
