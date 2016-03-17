package com.northwestern.habits.datagathering.phonedata;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.northwestern.habits.datagathering.DataStorageContract;

import java.util.HashMap;

public class PhoneDataService extends Service {

    public static final String ACCELEROMETER_EXTRA = "accel";
    public static final String TEMP_EXTRA = "temp";
    public static final String GRAVITY_EXTRA = "grav";
    public static final String GYRO_EXTRA = "GYRO";
    public static final String LIGHT_EXTRA = "LIGHT";
    public static final String LINEAR_ACCEL_EXTRA = "LINACC";
    public static final String MAG_FIELD_EXTRA = "MAG";
    public static final String PRESSURE_EXTRA = "PRESSURE";
    public static final String PROXIMITY_EXTRA = "PROX";
    public static final String HUMIDIDTY_EXTRA = "HUMID";
    public static final String ROTATION_EXTRA = "ROTATION";

    public static final String STUDY_ID_EXTRA = "study";
    public static final String CONTINUE_STUDY_EXTRA = "continue study";
    public static final String STOP_STREAM_EXTRA = "stop stream";

    private final String TAG = "PhoneDataService";
    private String studyName;

    private SQLiteOpenHelper dbHelper;
    private HashMap<String, Boolean> modes = new HashMap<>();


    private AccelerometerManager accManager;
    private TempManager tempManager;
    private GravityManager gravManager;
    private GyroManager gyroManager;
    private LightManager lightManager;
    private LinearAccelManager linAccelManager;
    private MagFieldManager magManager;
    private PressureManager pressureManager;
    private ProximityManager proximityManager;
    private HumidityManager humidityManager;
    private RotationManager rotationManager;




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started the service.");

        Log.v(TAG, "Retrieving database");
        if (dbHelper == null)
            dbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());

        // Get the band info, client, and data required
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (!extras.getBoolean(CONTINUE_STUDY_EXTRA)) {
                // End study requested
                Log.v(TAG, "Ending study");
                // Unregister all clients
                new StopAllStreams().execute();

            } else {
                // Continue the study
                modes.put(ACCELEROMETER_EXTRA, extras.getBoolean(ACCELEROMETER_EXTRA));
                modes.put(TEMP_EXTRA, extras.getBoolean(TEMP_EXTRA));
                modes.put(GRAVITY_EXTRA, extras.getBoolean(GRAVITY_EXTRA));
                modes.put(GYRO_EXTRA, extras.getBoolean(GYRO_EXTRA));
                modes.put(LIGHT_EXTRA, extras.getBoolean(LIGHT_EXTRA));
                modes.put(LINEAR_ACCEL_EXTRA, extras.getBoolean(LINEAR_ACCEL_EXTRA));
                modes.put(MAG_FIELD_EXTRA, extras.getBoolean(MAG_FIELD_EXTRA));
                modes.put(PRESSURE_EXTRA, extras.getBoolean(PRESSURE_EXTRA));
                modes.put(PROXIMITY_EXTRA, extras.getBoolean(PROXIMITY_EXTRA));
                modes.put(HUMIDIDTY_EXTRA, extras.getBoolean(HUMIDIDTY_EXTRA));
                modes.put(ROTATION_EXTRA, extras.getBoolean(ROTATION_EXTRA));

                // Set the study and device
                studyName = extras.getString(STUDY_ID_EXTRA);
                Log.v(TAG, "Study name is: " + studyName);

                if (extras.getBoolean(STOP_STREAM_EXTRA)) {
                    Log.v(TAG, "Stop stream requested.");

                    // Unsubscribe from specified tasks
                    for (String type : modes.keySet()) {
                        if (modes.get(type)) {
                            genericUnsubscribeFactory(type);
                        }
                    }


                } else {
                    // Stop stream not requested: start requested streams if not already streaming

                    for (String key : modes.keySet()) {
                        if (modes.get(key)) {
                            Log.v(TAG, "For mode " + key + " value is " + modes.get(key));
                            genericSubscriptionFactory(key);
                        }
                    }
                }
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void genericSubscriptionFactory(String request) {
        Log.v(TAG, request + " requested");

        // Request the appropriate stream
        switch (request) {
            case ACCELEROMETER_EXTRA:
                if (accManager == null)
                    accManager = new AccelerometerManager(studyName, dbHelper, this);

                accManager.subscribe();
                break;
            case TEMP_EXTRA:
                if (tempManager == null)
                    tempManager = new TempManager(studyName, dbHelper, this);

                tempManager.subscribe();
                break;
            case GRAVITY_EXTRA:
                if (gravManager == null)
                    gravManager = new GravityManager(studyName, dbHelper, this);

                gravManager.subscribe();
                break;
            case GYRO_EXTRA:
                if (gyroManager == null)
                    gyroManager = new GyroManager(studyName, dbHelper, this);

                gyroManager.subscribe();
                break;
            case LIGHT_EXTRA:
                if (lightManager == null)
                    lightManager = new LightManager(studyName, dbHelper, this);

                lightManager.subscribe();
                break;
            case LINEAR_ACCEL_EXTRA:
                if (linAccelManager == null)
                    linAccelManager = new LinearAccelManager(studyName, dbHelper, this);

                linAccelManager.subscribe();
                break;
            case MAG_FIELD_EXTRA:
                if (magManager == null)
                    magManager = new MagFieldManager(studyName, dbHelper, this);

                magManager.subscribe();
                break;
            case PRESSURE_EXTRA:
                if (pressureManager == null)
                    pressureManager = new PressureManager(studyName, dbHelper, this);

                pressureManager.subscribe();
                break;
            case PROXIMITY_EXTRA:
                if (proximityManager == null)
                    proximityManager = new ProximityManager(studyName, dbHelper, this);

                proximityManager.subscribe();
                break;
            case HUMIDIDTY_EXTRA:
                if (humidityManager == null)
                    humidityManager = new HumidityManager(studyName, dbHelper, this);

                humidityManager.subscribe();
                break;
            case ROTATION_EXTRA:
                if (rotationManager == null)
                    rotationManager = new RotationManager(studyName, dbHelper, this);

                rotationManager.subscribe();
                break;
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
        }
    }


    private void genericUnsubscribeFactory(String request) {
        Log.v(TAG, "Stopping stream " + request);

        // Unsubscribe from the appropriate stream
        switch (request) {
            case ACCELEROMETER_EXTRA:
                if (accManager != null)
                    accManager.unSubscribe();
                break;
            case TEMP_EXTRA:
                if (tempManager != null)
                    tempManager.unSubscribe();
                break;
            case GRAVITY_EXTRA:
                if (gravManager != null)
                    gravManager.unSubscribe();
                break;
            case GYRO_EXTRA:
                if (gyroManager != null)
                    gyroManager.unSubscribe();
                break;
            case LIGHT_EXTRA:
                if (lightManager != null)
                    lightManager.unSubscribe();
                break;
            case LINEAR_ACCEL_EXTRA:
                if (linAccelManager != null)
                    linAccelManager.unSubscribe();
                break;
            case MAG_FIELD_EXTRA:
                if (magManager != null)
                    magManager.unSubscribe();
                break;
            case PRESSURE_EXTRA:
                if (pressureManager != null)
                    pressureManager.unSubscribe();
                break;
            case PROXIMITY_EXTRA:
                if (proximityManager != null)
                    proximityManager.unSubscribe();
                break;
            case HUMIDIDTY_EXTRA:
                if (humidityManager != null)
                    humidityManager.unSubscribe();
                break;
            case ROTATION_EXTRA:
                if (rotationManager != null)
                    rotationManager.unSubscribe();
                break;
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
        }
    }


    public class StopAllStreams extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // TODO implement this
            return null;
        }
    }
}
