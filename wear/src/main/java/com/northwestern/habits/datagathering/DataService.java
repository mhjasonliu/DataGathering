package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.northwestern.habits.datagathering.CustomListeners.AccelerometerListener;
import com.northwestern.habits.datagathering.CustomListeners.GyroscopeListener;
import com.northwestern.habits.datagathering.CustomListeners.HeartRateListener;

import java.util.HashSet;
import java.util.Set;

public class DataService extends WearableListenerService implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "DataService";

    public DataService() {
    }

    private static final int SERVICE_ID = 5345;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Starting service...");

        if (intent != null) {
            startIntent = PendingIntent.getActivity(getBaseContext(), 0,
                    new Intent(intent), flags);
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Data Gathering Wear Data")
                .setContentText("Gathering wear data in foreground service").build();
        startForeground(SERVICE_ID, notification);

        initSensors();
        registerSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));

        return START_STICKY;
    }

    private void registerSensors(Set<String> sensors) {
        for (String sensor : sensors) {
            switch (sensor) {
                case Preferences.SENSOR_ACCEL:
                        mAccelListener.registerListener();
                    break;
                case Preferences.SENSOR_GYRO:
                        mGyroListener.registerListener();
                    break;
                case Preferences.SENSOR_HEART:
                        mHeartListener.registerListener();
                    break;
                default:
                    Log.e(TAG, "Unknown sensor requested: " + sensor);
            }
        }
    }

    /* *************************** DATA HANDLING ***************************** */
    // Sensor related fields
    private SensorManager mManager;
    /**
     * Maintains accelerometer registration state.
     * Update every time you register/unregister outside of
     * activity lifecycle
     */

    private void initSensors() {
        mManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
        mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
        mHeartListener = new HeartRateListener(getBaseContext(), mManager);
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
        if (mManager == null) {
            initSensors();
        }

        if (messageEvent != null) {
            if (messageEvent.getPath().equals("/DataRequest")) {
                SharedPreferences prefs = getSharedPreferences(Preferences.PREFERENCE_NAME, MODE_PRIVATE);
                Set<String> activeSensors = new HashSet<>(prefs.getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));

                String action = new String(messageEvent.getData());
                switch (action) {
                    case "Accelerometer1":
                        Log.v(TAG, "Start accel requested.");
                        mAccelListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_ACCEL);
                        break;
                    case "Accelerometer0":
                        Log.v(TAG, "Stop accel requested.");
                        mAccelListener.unRegisterListener();
                        activeSensors.remove(Preferences.SENSOR_ACCEL);
                        break;
                    case "Gyroscope1":
                        Log.v(TAG, "Start gyro requested.");
                        mGyroListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_GYRO);
                        break;
                    case "Gyroscope0":
                        Log.v(TAG, "Stop accel requested.");
                        mGyroListener.unRegisterListener();
                        activeSensors.remove(Preferences.SENSOR_GYRO);
                        break;
                    case "HeartRate1":
                        Log.v(TAG, "Start gyro requested.");
                        mHeartListener.registerListener();
                        activeSensors.add(Preferences.SENSOR_HEART);
                        break;
                    case "HeartRate0":
                        Log.v(TAG, "Stop accel requested.");
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
                editor.commit();

                Log.v(TAG, getSharedPreferences(Preferences.PREFERENCE_NAME, 0).getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()).toString());
            }
        }
    }

    private PendingIntent startIntent;
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        WriteDataTask.writeError(e, getBaseContext());

        if (startIntent != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, startIntent);
        }
        System.exit(2);
    }
}
