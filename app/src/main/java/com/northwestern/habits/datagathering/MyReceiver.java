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

import com.northwestern.habits.datagathering.database.CsvWriter;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.LabelManager;
import com.northwestern.habits.datagathering.userinterface.SplashActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
            String userID = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString(Preferences.USER_ID, "");
            long ts = Calendar.getInstance().getTimeInMillis();
            File logCsv = CsvWriter.getCsv(CsvWriter.getFolder(ts, userID, "LOG"), ts);
            List<String> properties = new ArrayList<>();
            properties.add("Timestamp");
            properties.add("Event");
            FileWriter csvwriter = null;
            try {
                csvwriter = CsvWriter.writeProperties(properties, logCsv, context);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String event = intent.getAction();

            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    Log.v(TAG, "Power connected action received.");
                    event = "Power connected";

                    // Power connected, start backup if wifi connected
                    if (isWifiConnected(context)) {
                        // start database backup
                        Intent i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_BACKUP);
                        context.startService(i);
                    }
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    event = "Power disconnected";
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
                        event = "Wifi connected";
                        // Wifi is connected, start backup if power connected
                        if (isCharging(context)) {
                            i = new Intent(context, DataManagementService.class);
                            i.setAction(DataManagementService.ACTION_BACKUP);
                            context.startService(i);
                        }

                    } else {
                        // Wifi disconnected, stop backup if it is running
                        event = "Wifi disconnected";
                        i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);

                    }

                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    event = "Phone turned on";
                    SplashActivity.onStartup(context);
                    break;
                case ACTION_LABEL:
                    // Hand it off to the LabelManager
                    int labelExtra = intent.getIntExtra(LABEL_EXTRA, 0);
                    long timestamp = intent.getLongExtra(TIMESTAMP_EXTRA, 0);
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit().putInt(Preferences.LABEL, labelExtra)
                            .putBoolean(Preferences.IS_EATING,
                                    labelExtra == DataManagementService.L_EATING)
                            .commit();
                    LabelManager.addLabelChange(userID, context, Integer.toString(labelExtra), timestamp);
                    break;
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    event = "Airplane mode changed";
                    break;
                case Intent.ACTION_SHUTDOWN:
                    event = "Shutdown Started";
                    break;
                case Intent.ACTION_DEVICE_STORAGE_LOW:
                    event = "Device storage low";
                    break;
                case Intent.ACTION_DEVICE_STORAGE_OK:
                    event = "Device storage OK";
                    break;
                case Intent.ACTION_BATTERY_LOW:
                    event = "Battery low.";
                    break;
                case Intent.ACTION_BATTERY_OKAY:
                    event = "Battery OK";
                    break;
                default:
                    Log.e(TAG, "Unknown type sent to receiver: " + intent.getAction());
            }

            if (csvwriter != null) {
                List<Map<String,Object>> data = new LinkedList<>();
                Map<String,Object> map = new HashMap<>();
                map.put("Timestamp", Calendar.getInstance().getTime().toString());
                map.put("Event", event);
                data.add(map);
                CsvWriter.writeDataSeries(csvwriter, data, properties);
                try {
                    csvwriter.flush();
                    csvwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
