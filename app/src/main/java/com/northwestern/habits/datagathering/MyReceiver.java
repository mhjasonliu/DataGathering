package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

import com.northwestern.habits.datagathering.banddata.BandDataService;

/**
 * Created by William on 7/11/2016.
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    private boolean isWifiConnected(Context c) {
        SupplicantState supState;
        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        supState = wifiInfo.getSupplicantState();
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
            && supState == SupplicantState.COMPLETED;
    }

    boolean isCharging(Context context) {
        // Check for charging
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            ConnectivityManager connManager;
            NetworkInfo network;
            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    Log.v(TAG, "Power connected action received.");

                    // Power connected, start backup if wifi connected
                    if (isWifiConnected(context)) {
                        Toast.makeText(context, "Starting databse backup", Toast.LENGTH_SHORT).show();
                        // start database backup
                        Intent i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_BACKUP);
                        context.startService(i);
                    }
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.v(TAG, "Power disconnected action received.");

                    // Power disconnected. stop backup if it is running

                    Toast.makeText(context, "Stopping databse backup", Toast.LENGTH_SHORT).show();
                    // stop database backup
                {
                    Intent i = new Intent(context, DataManagementService.class);
                    i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                    context.startService(i);
                }
                break;

                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    Log.v(TAG, "Wifi action received");
                    if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                            false)) {
                        // Wifi is connected, start backup if power connected
                        if (isCharging(context)) {
                            Toast.makeText(context, "Starting databse backup", Toast.LENGTH_SHORT).show();
                            // start database backup
                            Intent i = new Intent(context, DataManagementService.class);
                            i.setAction(DataManagementService.ACTION_BACKUP);
                            context.startService(i);
                        }

                    } else {
                        // Wifi disconnected, stop backup if it is running
                        Toast.makeText(context, "Stopping databse backup", Toast.LENGTH_SHORT).show();
                        // stop database backup
                        Intent i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);

                    }

                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    // Start streaming sensors that were cut off when powered down
                    context.startService(new Intent(context, BandDataService.class));
                    break;
                default:
                    Log.e(TAG, "Unknown type sent to receiver: " + intent.getAction());
            }
        }
    }
}
