package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
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

public class DataService extends Service implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "DataService";
    private int ONGOING_NOTIFICATION_ID = 003;
    private PowerManager.WakeLock wakeLock;

    private AccelerometerListener mAccelListener;
    private GyroscopeListener mGyroListener;
    private HeartRateListener mHeartListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }

        unRegisterSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting WearableListenerService...");
        Toast.makeText(this, "Start collecting", Toast.LENGTH_LONG).show();

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Data Gathering Wear Data")
                .setContentText("Gathering wear data in foreground service").build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        initSensorsAndRegister();

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
    private void initSensorsAndRegister() {
        Log.d(TAG, "*************************INIT SENSORS CALLED*************************");

        if (mManager == null) {
            mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
            mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
            mHeartListener = new HeartRateListener(getBaseContext(), mManager);
        }
        registerSensors(getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getStringSet(Preferences.KEY_ACTIVE_SENSORS, new HashSet<String>()));
    }

    private void registerSensors(Set<String> sensors) {
        Log.d(TAG, "registerSensors count..." + sensors.size());
        mAccelListener.registerListener();
        mGyroListener.registerListener();
        mHeartListener.registerListener();
    }

    private void unRegisterSensors(Set<String> sensors) {
        Log.d(TAG, "unRegisterSensors count..." + sensors.size());
        mAccelListener.unRegisterListener();
        mGyroListener.unRegisterListener();
        mHeartListener.unRegisterListener();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.v(TAG, "uncaughtException " + e.getMessage());
        WriteData.logError(this, e);
    }
}
