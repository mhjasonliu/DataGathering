package com.northwestern.habits.datagathering.banddata;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class BandDataService extends Service {

    /* ****************** STRINGS FOR IDENTIFICATION OF EXTRAS ********************************* */
    public static final String ACCEL_REQ_EXTRA = "accelerometer";
    public static final String ALT_REQ_EXTRA = "altimeter";
    public static final String AMBIENT_REQ_EXTRA = "ambient";
    public static final String BAROMETER_REQ_EXTRA = "barometer";
    public static final String CALORIES_REQ_EXTRA = "calories";
    public static final String CONTACT_REQ_EXTRA = "contact";
    public static final String DISTANCE_REQ_EXTRA = "distance";
    public static final String GSR_REQ_EXTRA = "gsr";
    public static final String GYRO_REQ_EXTRA = "gyro";
    public static final String HEART_RATE_REQ_EXTRA = "heartRate";
    public static final String PEDOMETER_REQ_EXTRA = "pedometer";
    public static final String SKIN_TEMP_REQ_EXTRA = "skinTemperature";
    public static final String UV_REQ_EXTRA = "ultraViolet";

    public static final String INDEX_EXTRA = "index";
    public static final String STUDY_ID_EXTRA = "study";
    public static final String LOCATION_EXTRA = "location";
    public static final String FREQUENCY_EXTRA = "frequency";

    public static final String CONTINUE_STUDY_EXTRA = "continue study";
    public static final String STOP_STREAM_EXTRA = "stop stream";



    private static final String TAG = "Band Service";

    public static String PATH;// = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BandData";

    // General stuff (maintained by main)
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, List<String>> bandStreams = new HashMap<>();
    protected static HashMap<BandInfo, String> locations = new HashMap<>();
    protected static HashMap<BandInfo, String> frequencies = new HashMap<>();

    protected String studyName;

    private SQLiteOpenHelper dbHelper;


    // Data managers
    AccelerometerManager accManager;
    AltimeterManager altManager;
    AmbientManager ambManager;
    BarometerManager barometerManager;
    CaloriesManager calManager;
    ContactManager conManager;
    DistanceManager distManager;
    GsrManager gsrManager;
    GyroscopeManager gyroManager;
    HeartRateManager heartManager;
    PedometerManager pedoManager;
    SkinTempManager skinTempManager;
    UvManager uvManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started the service.");

        Log.v(TAG, "Retrieving database");
        if (dbHelper == null)
            dbHelper = DataStorageContract.BluetoothDbHelper.getInstance(this);

        // Get the band info, client, and data required
        Bundle extras = intent.getExtras();
        if (extras != null){
            if (!extras.getBoolean(CONTINUE_STUDY_EXTRA)) {
                // End study requested
                Log.v(TAG, "Ending study");
                // Unregister all clients
                new StopAllStreams().execute();

            } else {
                // Continue the study
                int index = extras.getInt(INDEX_EXTRA);
                BandInfo band = pairedBands[index];
                modes.put(ACCEL_REQ_EXTRA, extras.getBoolean(ACCEL_REQ_EXTRA));
                modes.put(ALT_REQ_EXTRA, extras.getBoolean(ALT_REQ_EXTRA));
                modes.put(AMBIENT_REQ_EXTRA, extras.getBoolean(AMBIENT_REQ_EXTRA));
                modes.put(BAROMETER_REQ_EXTRA, extras.getBoolean(BAROMETER_REQ_EXTRA));
                modes.put(CALORIES_REQ_EXTRA, extras.getBoolean(CALORIES_REQ_EXTRA));
                modes.put(CONTACT_REQ_EXTRA, extras.getBoolean(CONTACT_REQ_EXTRA));
                modes.put(DISTANCE_REQ_EXTRA, extras.getBoolean(DISTANCE_REQ_EXTRA));
                modes.put(GSR_REQ_EXTRA, extras.getBoolean(GSR_REQ_EXTRA));
                modes.put(GYRO_REQ_EXTRA, extras.getBoolean(GYRO_REQ_EXTRA));
                modes.put(HEART_RATE_REQ_EXTRA, extras.getBoolean(HEART_RATE_REQ_EXTRA));
                modes.put(PEDOMETER_REQ_EXTRA, extras.getBoolean(PEDOMETER_REQ_EXTRA));
                modes.put(SKIN_TEMP_REQ_EXTRA, extras.getBoolean(SKIN_TEMP_REQ_EXTRA));
                modes.put(UV_REQ_EXTRA, extras.getBoolean(UV_REQ_EXTRA));

                locations.put(band, extras.getString(LOCATION_EXTRA));
                frequencies.put(band, extras.getString(FREQUENCY_EXTRA));

                // Set the study and device
                studyName = extras.getString(STUDY_ID_EXTRA);
                Log.v(TAG, "Study name is: " + studyName);

                if (extras.getBoolean(STOP_STREAM_EXTRA)){
                    Log.v(TAG, "Stop stream requested.");

                    // Unsubscribe from specified tasks
                    for (String type : modes.keySet()) {
                        if (modes.get(type)) {
                            genericUnsubscribeFactory(type, band);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                } else {
                    // Stop stream not requested: start requested streams if not already streaming

                    for ( String key : modes.keySet() ) {
                        if (modes.get(key)) {
                            //Log.v(TAG, "For mode " + key + " value is " + modes.get(key));
                            genericSubscriptionFactory(key, band);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        // Create file for the csv's
        PATH = Environment.getExternalStorageDirectory() + "/Band";

        File folder = new File(BandDataService.PATH);

        boolean var;
        if (!folder.exists()) {
            var = folder.mkdir();
            Log.v(TAG, BandDataService.PATH + " exists: " + Boolean.toString(var));
        }
        return Service.START_NOT_STICKY;
    }


    private  void genericUnsubscribeFactory(String request, BandInfo band) {
        Log.v(TAG, "Stopping stream " + request);
        // Check for existance of stream
        if (bandStreams.containsKey(band) && bandStreams.get(band).contains(request)) {
            if (bandStreams.get(band).size() == 1) {
                // Only stream open for this band, remove from bandStreams
                bandStreams.remove(band);
            } else {
                // Other streams open, remove from list
                bandStreams.get(band).remove(request);
            }

            // Unsubscribe from the appropriate stream
            switch (request) {
                case ACCEL_REQ_EXTRA:
                    if (accManager != null)
                        accManager.unSubscribe(band);
                    break;
                case ALT_REQ_EXTRA:
                    if (altManager != null)
                        altManager.unSubscribe(band);
                    break;
                case AMBIENT_REQ_EXTRA:
                    if (ambManager != null)
                        ambManager.unSubscribe(band);
                    break;
                case BAROMETER_REQ_EXTRA:
                    if (barometerManager != null)
                        barometerManager.unSubscribe(band);
                    break;
                case CALORIES_REQ_EXTRA:
                    if (calManager != null)
                        calManager.unSubscribe(band);
                    break;
                case CONTACT_REQ_EXTRA:
                    if (conManager != null)
                        conManager.unSubscribe(band);
                    break;
                case DISTANCE_REQ_EXTRA:
                    if (distManager != null)
                        distManager.unSubscribe(band);
                    break;
                case GSR_REQ_EXTRA:
                    if (gsrManager != null)
                        gsrManager.unSubscribe(band);
                    break;
                case GYRO_REQ_EXTRA:
                    if (gyroManager != null)
                        gyroManager.unSubscribe(band);
                    break;
                case HEART_RATE_REQ_EXTRA:
                    if (heartManager != null)
                        heartManager.unSubscribe(band);
                    break;
                case PEDOMETER_REQ_EXTRA:
                    if (pedoManager != null)
                        pedoManager.unSubscribe(band);
                    break;
                case SKIN_TEMP_REQ_EXTRA:
                    if (skinTempManager != null)
                        skinTempManager.unSubscribe(band);
                    break;
                case UV_REQ_EXTRA:
                    if (uvManager != null)
                        uvManager.unSubscribe(band);
                    break;
                default:
                    Log.e(TAG, "Unknown subscription requested " + request);
            }
        }

    }


    private void genericSubscriptionFactory(String request, BandInfo band) {
        Log.v(TAG, request + " requested");
        if (!bandStreams.containsKey(band)) {
            // Make a new list to put into the map with the band
            List<String> list = new LinkedList<>();
            list.add(request);

            // Add the band to the map
            bandStreams.put(band, list);

        } else if (!bandStreams.get(band).contains(request)) {
            // Add sensor to the list in the stream map
            bandStreams.get(band).add(request);

        }

        // Request the appropriate stream
        switch (request) {
            case ACCEL_REQ_EXTRA:
                if (accManager == null)
                    accManager = new AccelerometerManager(studyName, dbHelper, this);

                accManager.setFrequency(frequencies.get(band));
                accManager.subscribe(band);
                break;
            case ALT_REQ_EXTRA:
                if (altManager == null)
                    altManager = new AltimeterManager(studyName, dbHelper, this);

                altManager.subscribe(band);
                break;
            case AMBIENT_REQ_EXTRA:
                if (ambManager == null)
                    ambManager = new AmbientManager(studyName, dbHelper, this);

                ambManager.subscribe(band);
                break;
            case BAROMETER_REQ_EXTRA:
                if (barometerManager == null)
                    barometerManager = new BarometerManager(studyName, dbHelper, this);

                barometerManager.subscribe(band);
                break;
            case CALORIES_REQ_EXTRA:
                if (calManager == null)
                    calManager = new CaloriesManager(studyName, dbHelper, this);

                calManager.subscribe(band);
                break;
            case CONTACT_REQ_EXTRA:
                if (conManager == null)
                    conManager = new ContactManager(studyName, dbHelper, this);

                conManager.subscribe(band);
                break;
            case DISTANCE_REQ_EXTRA:
                if (distManager == null)
                    distManager = new DistanceManager(studyName, dbHelper, this);

                distManager.subscribe(band);
                break;
            case GSR_REQ_EXTRA:
                if (gsrManager == null)
                    gsrManager = new GsrManager(studyName, dbHelper, this);

                gsrManager.subscribe(band);
                break;
            case GYRO_REQ_EXTRA:
                if (gyroManager == null)
                    gyroManager = new GyroscopeManager(studyName, dbHelper, this);

                gyroManager.setFrequency(frequencies.get(band));
                gyroManager.subscribe(band);
                break;
            case HEART_RATE_REQ_EXTRA:
                if (heartManager == null)
                    heartManager = new HeartRateManager(studyName, dbHelper, this);

                heartManager.subscribe(band);
                break;
            case PEDOMETER_REQ_EXTRA:
                if (pedoManager == null)
                    pedoManager = new PedometerManager(studyName, dbHelper, this);

                pedoManager.subscribe(band);
                break;
            case SKIN_TEMP_REQ_EXTRA:
                if (skinTempManager == null)
                    skinTempManager = new SkinTempManager(studyName, dbHelper, this);

                skinTempManager.subscribe(band);
                break;
            case UV_REQ_EXTRA:
                if (uvManager == null)
                    uvManager = new UvManager(studyName, dbHelper, this);

                uvManager.subscribe(band);
                break;
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Process was bound when it shouldn't be.");
        return null;
    }


    public class StopAllStreams extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            for (BandInfo band : bandStreams.keySet()) {
                for (String type : bandStreams.get(band)) {
                    genericUnsubscribeFactory(type, band);
                }
            }
            return null;
        }
    }
}
