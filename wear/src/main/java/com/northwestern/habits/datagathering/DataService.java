package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.northwestern.habits.datagathering.CustomListeners.AccelerometerListener;
import com.northwestern.habits.datagathering.CustomListeners.GyroscopeListener;
import com.northwestern.habits.datagathering.CustomListeners.HeartRateListener;
import com.northwestern.habits.datagathering.CustomListeners.WriteDataThread;
import com.northwestern.habits.datagathering.filewriteservice.SingletonFileWriter;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class DataService extends WearableListenerService implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "DataService";

    public DataService() {
    }

    private static final int SERVICE_ID = 5345;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service...");

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

        /*final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
//                new SendDataTask(getBaseContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                new SendDataTask(getBaseContext()).execute();
            }
        };
        handler.postDelayed(r, 10000);*/
        new SendDataTask(getBaseContext()).execute();
        return START_STICKY;
    }

    private void registerSensors(Set<String> sensors) {
        Log.d(TAG, "registerSensors count..." + sensors.size());
        for (String sensor : sensors) {
            Log.d(TAG, "registerSensors service..." + sensor);
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
    static boolean FirstTime= true;
    private void initSensors() {
        if(FirstTime) {
            FirstTime= false;
            Log.d(TAG, "************************* INIT SENSORS CALLED... *************************");
            mManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            WriteDataThread wdt = new WriteDataThread(getBaseContext());
            mAccelListener = new AccelerometerListener(getBaseContext(), mManager);
            mGyroListener = new GyroscopeListener(getBaseContext(), mManager);
            mHeartListener = new HeartRateListener(getBaseContext(), mManager);
            mAccelListener.setWDT(wdt);
            mGyroListener.setWDT(wdt);
            mHeartListener.setWDT(wdt);
            wdt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            SendDataTask sendDataTask = new SendDataTask(getBaseContext());
//            sendDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
            initSensors();
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

    private PendingIntent startIntent;
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        SingletonFileWriter.getInstance(getBaseContext()).writeError(e, getBaseContext());

        if (startIntent != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, startIntent);
        }
        System.exit(2);
    }
}
