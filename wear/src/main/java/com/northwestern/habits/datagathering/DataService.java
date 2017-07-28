package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.northwestern.habits.datagathering.CustomListeners.AccelerometerListener;
import com.northwestern.habits.datagathering.CustomListeners.GyroscopeListener;
import com.northwestern.habits.datagathering.CustomListeners.HeartRateListener;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class DataService extends WearableListenerService implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "DataService";
    private int ONGOING_NOTIFICATION_ID = 003;

    //default constructor
    public DataService() {
    }

    private static final int SERVICE_ID = 5345;

    /**
     * Returns whether or not the wear is charging
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting WearableListenerService...");

        if (intent != null) {
            startIntent = PendingIntent.getActivity(getBaseContext(), 0,
                    new Intent(intent), flags);
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Data Gathering Wear Data")
                .setContentText("Gathering wear data in foreground service").build();
        startForeground(SERVICE_ID, notification);

        initSensorsAndRegister();
//        if (isCharging(this)) {
//            Log.v(TAG, "isCharging true" );
//            new SendDataTask(getBaseContext()).execute();
//        }
        return START_STICKY;
    }

    /* *************************** DATA HANDLING ***************************** */
    // Sensor related fields
    private SensorManager mManager;
    /**
     * Maintains accelerometer registration state.
     * Update every time you register/unregister outside of
     * activity lifecycle
     */
    static boolean isFirstTime = true;
    private void initSensorsAndRegister() {
        Log.d(TAG, "*************************INIT SENSORS CALLED*************************");

//        if (isCharging(this)) {
//            isFirstTime = true;
//            if (mManager == null) {
//                mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//                mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
//                mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
//                mHeartListener = new HeartRateListener(getBaseContext(), mManager);
//            }
//            unRegisterSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
//                    .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));
//        } else {
            if (isFirstTime) {
                isFirstTime = false;
                if (mManager == null) {
                    mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                    mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
                    mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
                    mHeartListener = new HeartRateListener(getBaseContext(), mManager);
                }
                registerSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                        .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));
            }
//        }
    }

    private void registerSensors(Set<String> sensors) {
        Log.d(TAG, "registerSensors count..." + sensors.size());
        mAccelListener.registerListener();
        mGyroListener.registerListener();
        mHeartListener.registerListener();

//        for (String sensor : sensors) {
//            Log.d(TAG, "registerSensors service..." + sensor);
//            switch (sensor) {
//                case Preferences.SENSOR_ACCEL:
//                    mAccelListener.registerListener();
//                    break;
//                case Preferences.SENSOR_GYRO:
//                    mGyroListener.registerListener();
//                    break;
//                case Preferences.SENSOR_HEART:
//                    mHeartListener.registerListener();
//                    break;
//                default:
//                    Log.e(TAG, "Unknown sensor requested: " + sensor);
//            }
//        }
    }

    private void unRegisterSensors(Set<String> sensors) {
        Log.d(TAG, "unRegisterSensors count..." + sensors.size());
        mAccelListener.unRegisterListener1();
        mGyroListener.unRegisterListener1();
        mHeartListener.unRegisterListener1();

//        for (String sensor : sensors) {
//            Log.d(TAG, "unRegisterSensors service..." + sensor);
//            switch (sensor) {
//                case Preferences.SENSOR_ACCEL:
//                    mAccelListener.unRegisterListener1();
//                    break;
//                case Preferences.SENSOR_GYRO:
//                    mGyroListener.unRegisterListener1();
//                    break;
//                case Preferences.SENSOR_HEART:
//                    mHeartListener.unRegisterListener1();
//                    break;
//                default:
//                    Log.e(TAG, "Unknown sensor requested: " + sensor);
//            }
//        }
    }

    /**
     * Custom implementation of SensorEventListener specific to what we want to do with
     * accelerometer data.
     */
    private AccelerometerListener mAccelListener;
    private GyroscopeListener mGyroListener;
    private HeartRateListener mHeartListener;

    /* ***************** MESSAGE RECEIVING *********************** */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived " + messageEvent.getSourceNodeId());
        if (mManager == null) {
            initSensorsAndRegister();
        }

        if (messageEvent != null) {
            if (messageEvent.getPath().equals("/DataRequest")) {
                SharedPreferences prefs = getSharedPreferences(Preferences.PREFERENCE_NAME, MODE_PRIVATE);
                Set<String> activeSensors = new HashSet<>(prefs.getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));

                String action = new String(messageEvent.getData());
                switch (action) {
                    case "Accelerometer1":
                        Log.d(TAG, "Start accel requested.");
                        mAccelListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_ACCEL);
                        break;
                    case "Accelerometer0":
                        Log.e(TAG, "Stop accel requested.");
                        mAccelListener.unRegisterListener();
                        activeSensors.remove(Preferences.SENSOR_ACCEL);
                        break;
                    case "Gyroscope1":
                        Log.d(TAG, "Start gyro requested.");
                        mGyroListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_GYRO);
                        break;
                    case "Gyroscope0":
                        Log.e(TAG, "Stop gyro requested.");
                        mGyroListener.unRegisterListener();
                        activeSensors.remove(Preferences.SENSOR_GYRO);
                        break;
                    case "HeartRate1":
                        Log.d(TAG, "Start HeartRate1 requested.");
                        mHeartListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_HEART);
                        break;
                    case "HeartRate0":
                        Log.e(TAG, "Stop HeartRate1 requested.");
                        mHeartListener.unRegisterListener();
                        activeSensors.remove(Preferences.SENSOR_HEART);
                        break;
                    default:
                        Log.w(TAG, "Unknown action received" + action);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Preferences.KEY_PHONE_ID, messageEvent.getSourceNodeId());
                editor.putStringSet(Preferences.KEY_ACTIVE_SENSORS, activeSensors);
                Log.v(TAG, "Active sensors: " + activeSensors);
                editor.apply();
                Log.v(TAG, getSharedPreferences(Preferences.PREFERENCE_NAME, 0).getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()).toString());
            }
        }
    }

    @Override
    public void onCreate() {
        Intent notificationIntent = new Intent(this, DataService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Collecting data")
                .setContentText("HABits")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }


    @Override
    public void onDestroy() {
        unRegisterSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));
    }

    private PendingIntent startIntent;
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.v(TAG, "uncaughtException " + e.getMessage());

        if (startIntent != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, startIntent);
        }
        System.exit(2);
    }
}
