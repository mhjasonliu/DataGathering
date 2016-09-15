package com.northwestern.habits.datagathering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.LabelManager;
import com.northwestern.habits.datagathering.userinterface.SplashActivity;

/**
 * Created by William on 7/11/2016
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";
    public static final String ACTION_LABEL = "com.northwestern.habits.datagathering.action.LABEL";
    public static final String LABEL_EXTRA = "Label";
    public static final String USER_ID_EXTRA = "UserID";
    public static final String TIMESTAMP_EXTRA = "timestamp";

    /**
     * Returns whether or not the wifi is accessable
     * @param c context from which to access the wifi service
     * @return boolean
     */
    public static boolean isWifiConnected(Context c) {
        SupplicantState supState;
        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        supState = wifiInfo.getSupplicantState();
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
            && supState == SupplicantState.COMPLETED;
    }

    /**
     * Returns whether or not the phone is charging
     * @param context from which to access the battery manager
     * @return boolean
     */
    public static boolean isCharging(Context context) {
        // Check for charging
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert i != null;
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    Log.v(TAG, "Power connected action received.");

                    // Power connected, start backup if wifi connected
                    if (isWifiConnected(context)) {
                        // start database backup
                        Intent i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_BACKUP);
                        context.startService(i);
                    }
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.v(TAG, "Power disconnected action received.");

                    // Power disconnected. stop backup if it is running
                    Intent i = new Intent(context, DataManagementService.class);
                    i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                    context.startService(i);
                break;

                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    Log.v(TAG, "Wifi action received");
                    if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                            false)) {
                        // Wifi is connected, start backup if power connected
                        if (isCharging(context)) {
                            i = new Intent(context, DataManagementService.class);
                            i.setAction(DataManagementService.ACTION_BACKUP);
                            context.startService(i);
                        }

                    } else {
                        // Wifi disconnected, stop backup if it is running
                        i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);

                    }

                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    SplashActivity.onStartup(context);
                    break;
                case ACTION_LABEL:
                    // Hand it off to the LabelManager
                    //TODO get actual values from the intent
                    int labelExtra = intent.getIntExtra(LABEL_EXTRA, 0);
                    long timestamp = intent.getLongExtra(TIMESTAMP_EXTRA, 0);
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit().putInt(Preferences.LABEL, labelExtra).apply();
                    String userID = PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .getString(Preferences.USER_ID, "");
                    Log.v(TAG, "Label change received: " + labelExtra);
                    LabelManager.addLabelChange(userID, context, Integer.toString(labelExtra), timestamp);
                    break;
                default:
                    Log.e(TAG, "Unknown type sent to receiver: " + intent.getAction());
            }
        }
    }
}
