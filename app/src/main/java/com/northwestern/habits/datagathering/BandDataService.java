package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSensorEvent;
import com.microsoft.band.sensors.SampleRate;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class BandDataService extends Service {

    public static final String ACCEL_REQ_EXTRA = "accelerometer";
    public static final String ALT_REQ_EXTRA = "altimeter";
    public static final String AMBIENT_REQ_EXTRA = "ambient";
    public static final String BAROMETER_REQ_EXTRA = "barometer";
    public static final String CALORIES_REQ_EXTRA = "calories";
    public static final String CONTACT_REQ_EXTRA = "contact";
    public static final String DISTANCE_REQ_EXTRA = "distance";
    public static final String GSR_REQ_EXTRA = "gsr";
    public static final String HEART_RATE_REQ_EXTRA = "heartRate";
    public static final String PEDOMETER_REQ_EXTRA = "pedometer";
    public static final String SKIN_TEMP_REQ_EXTRA = "skinTemperature";
    public static final String UV_REQ_EXTRA = "ultraViolet";



    public static final String INDEX_EXTRA = "index";
    public static final String STUDY_ID_EXTRA = "study";
    public static final String LOCATION_EXTRA = "location";

    public static final String CONTINUE_STUDY_EXTRA = "continue study";
    public static final String STOP_STREAM_EXTRA = "stop stream";


    // General stuff (maintained by main)
    //private BandInfo band = null;
    //private BandClient client = null;
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, List<String>> bandStreams = new HashMap<>();
    private HashMap<BandInfo, String> locations = new HashMap<>();

    private DataStorageContract.BluetoothDbHelper mDbHelper;

    private String studyName;

    // Maps of listeners (maintained by tasks)
    HashMap<BandInfo, BandAccelerometerEventListenerCustom> accListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> accClients = new HashMap<>();


    HashMap<BandInfo, CustomBandAltimeterEventListener> altListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> altClients = new HashMap<>();

    HashMap<BandInfo, CustomBandAmbientLightEventListener> ambientListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> ambientClients = new HashMap<>();

    HashMap<BandInfo, CustomBandBarometerEventListener> barometerListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> barometerClients = new HashMap<>();

    HashMap<BandInfo, CustomBandCaloriesEventListener> caloriesListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> caloriesClients = new HashMap<>();

    HashMap<BandInfo, CustomBandGsrEventListener> gsrListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> gsrClients = new HashMap<>();

    HashMap<BandInfo, CustomBandHeartRateEventListener> heartRateListeners = new HashMap<>();
    HashMap<BandInfo, BandClient> heartRateClients = new HashMap<>();



    // Types
    private String T_BAND2 = "Microsoft_Band_2";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started the service.");

        Log.v(TAG, "Retrieving database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());

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
                modes.put(GSR_REQ_EXTRA, extras.getBoolean(GSR_REQ_EXTRA));
                modes.put(HEART_RATE_REQ_EXTRA, extras.getBoolean(HEART_RATE_REQ_EXTRA));

                locations.put(band, extras.getString(LOCATION_EXTRA));

                // Set the study and device
                studyName = extras.getString(STUDY_ID_EXTRA);
                Log.v(TAG, "Study name is: " + studyName);

                if (extras.getBoolean(STOP_STREAM_EXTRA)){
                    Log.v(TAG, "Stop stream requested.");
                    // Unsubscribe from specified tasks
                    if (modes.get(ACCEL_REQ_EXTRA)  && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(ACCEL_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(ACCEL_REQ_EXTRA);
                        }
                        // Start an accelerometer unsubscribe task
                        Log.v(TAG, "Unsubscribe from accelerometer");
                        new AccelerometerUnsubscribe().execute(band);
                    }

                    if (modes.get(ALT_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(ALT_REQ_EXTRA)) {
                        Log.v(TAG, "******************");
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(ALT_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from altimeter");
                        new AltimeterUnsubscribeTask().execute(band);
                    }

                    if (modes.get(AMBIENT_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(AMBIENT_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(AMBIENT_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from ambient light");
                        new AmbientUnsubscribeTask().execute(band);
                    }

                    if (modes.get(BAROMETER_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(BAROMETER_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(BAROMETER_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from barometer");
                        new BarometerUnsubscribeTask().execute(band);
                    }
                    if (modes.get(CALORIES_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(CALORIES_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(CALORIES_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from barometer");
                        new CaloriesUnsubscribeTask().execute(band);
                    }
                    if (modes.get(GSR_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(GSR_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(GSR_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from gsr");
                        new GsrUnsubscribeTask().execute(band);
                    }

                    if (modes.get(HEART_RATE_REQ_EXTRA) && bandStreams.containsKey(band) &&
                            bandStreams.get(band).contains(HEART_RATE_REQ_EXTRA)) {
                        if (bandStreams.get(band).size() == 1) {
                            // Only stream open for this band, remove from bandStreams
                            bandStreams.remove(band);
                        } else {
                            // Other streams open, remove from list
                            bandStreams.get(band).remove(HEART_RATE_REQ_EXTRA);
                        }
                        // Start an altimeter unsubscribe task
                        Log.v(TAG, "Unsubscribe from Heart Rate");
                        new HeartRateUnsubscribeTask().execute(band);
                    }




                } else {
                    // Stop stream not requested: start requested streams if not already streaming

                    for ( String key : modes.keySet() ) {
                        if (modes.get(key)) {
                            genericSubscriptionFactory(key, band);
                        }
                    }
                }
            }
        }
        return Service.START_NOT_STICKY;
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
            // Add accelerometer to the list in the stream map
            bandStreams.get(band).add(request);

        }

        // Request the appropriate stream
        switch (request) {
            case ACCEL_REQ_EXTRA:
                new AccelerometerSubscriptionTask().execute(band);
                break;
            case ALT_REQ_EXTRA:
                new AltimeterSubscriptionTask().execute(band);
                break;
            case AMBIENT_REQ_EXTRA:
                new AmbientLightSubscriptionTask().execute(band);
                break;
            case BAROMETER_REQ_EXTRA:
                new BarometerSubscriptionTask().execute(band);
                break;
            case CALORIES_REQ_EXTRA:
                new CaloriesSubscriptionTask().execute(band);
                break;
//            case contact
//            case distance
            case GSR_REQ_EXTRA:
                new GsrSubscriptionTask().execute(band);
                break;
//            case gyro
            case HEART_RATE_REQ_EXTRA:
                new HeartRateSubscriptionTask().execute(band);
                break;
//            case pedo
//            case skin temp
//            case uv
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Process was bound when it shouldn't be.");
        return null;
    }

    private final String TAG = "Band Service";

        /* *********** Event Listeners ************ */

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    /**
     * Helper that gets the date and time in proper format for database
     */
    private String getDateTime(BandSensorEvent event) {
        return dateFormat.format(event.getTimestamp());
    }

    private class BandAccelerometerEventListenerCustom implements BandAccelerometerEventListener {

        private BandInfo info;
        private String uName;
        private String location;

        public BandAccelerometerEventListenerCustom(BandInfo mInfo, String name) {
            super();
            info = mInfo;
            uName = name;
            location = locations.get(info);
        }

        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                String T_ACCEL = "Accelerometer";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_ACCEL, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_ACCEL);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Accelerometer table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, "X: " + Double.toString(event.getAccelerationX()) +
                        "Y: " + Double.toString(event.getAccelerationY()) +
                        "Z: " + Double.toString(event.getAccelerationZ()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_X, event.getAccelerationX());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y, event.getAccelerationY());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z, event.getAccelerationZ());


                writeDb.insert(DataStorageContract.AccelerometerTable.TABLE_NAME, null, values);
            }

        }
    }


    private class CustomBandAltimeterEventListener implements BandAltimeterEventListener {

        public CustomBandAltimeterEventListener(BandInfo bandInfo, String name) {
            super();
            uName = name;
            info = bandInfo;
            location = locations.get(info);
        }

        private String uName;
        private BandInfo info;
        private String location;

        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {

                String T_ALT = "Altimeter";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_ALT, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_ALT);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Altimeter table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("Total Gain = %d cm\n", event.getTotalGain()) +
                        String.format("Total Loss = %d cm\n", event.getTotalLoss()) +
                        String.format("Stepping Gain = %d cm\n", event.getSteppingGain()) +
                        String.format("Stepping Loss = %d cm\n", event.getSteppingLoss()) +
                        String.format("Steps Ascended = %d\n", event.getStepsAscended()) +
                        String.format("Steps Descended = %d\n", event.getStepsDescended()) +
                        String.format("Rate = %f cm/s\n", event.getRate()) +
                        String.format("Flights of Stairs Ascended = %d\n", event.getFlightsAscended()) +
                        String.format("Flights of Stairs Descended = %d\n", event.getFlightsDescended()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_TOTAL_GAIN, event.getTotalGain());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_TOTAL_LOSS, event.getTotalLoss());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STEPPING_GAIN, event.getSteppingGain());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STEPPING_LOSS, event.getSteppingLoss());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STEPS_ASCENDED, event.getStepsAscended());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STEPS_DESCENDED, event.getStepsDescended());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_RATE, event.getRate());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STAIRS_ASCENDED, event.getFlightsAscended());
                values.put(DataStorageContract.AltimeterTable.COLUMN_NAME_STAIRS_DESCENDED, event.getFlightsDescended());


                writeDb.insert(DataStorageContract.AltimeterTable.TABLE_NAME, null, values);
            }
        }

    }


    private class CustomBandAmbientLightEventListener implements BandAmbientLightEventListener{
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandAmbientLightEventListener(BandInfo bInfo, String name) {
            super();
            info = bInfo;
            uName = name;
            location = locations.get(info);
        }

        @Override
        public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
            if (event != null) {
                String T_AMBIENT = "Ambient";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_AMBIENT, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_AMBIENT);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Brightness table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("Brightness = %d lux\n", event.getBrightness()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AmbientTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.AmbientTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AmbientTable.COLUMN_NAME_BRIGHTNESS, event.getBrightness());


                writeDb.insert(DataStorageContract.AmbientTable.TABLE_NAME, null, values);
            }
        }
    }

    private class CustomBandBarometerEventListener implements BandBarometerEventListener {
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandBarometerEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
            location = locations.get(info);
        }

        @Override
        public void onBandBarometerChanged(final BandBarometerEvent event) {
            if (event != null) {
                String T_BAROMETER = "Barometer";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_BAROMETER, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_BAROMETER);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Barometer table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("Air Pressure = %.3f hPa\n"
                                + "Temperature = %.2f degrees Celsius", event.getAirPressure(),
                        event.getTemperature()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.BarometerTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.BarometerTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.BarometerTable.COLUMN_NAME_PRESSURE, event.getAirPressure());
                values.put(DataStorageContract.BarometerTable.COLUMN_NAME_TEMP, event.getTemperature());


                writeDb.insert(DataStorageContract.BarometerTable.TABLE_NAME, null, values);

            }
        }
    }


    private class CustomBandCaloriesEventListener implements BandCaloriesEventListener {
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandCaloriesEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
            location = locations.get(info);
        }

        @Override
        public void onBandCaloriesChanged(final BandCaloriesEvent event) {
            if (event != null) {
                String T_CALORIES = "Calories";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_CALORIES, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_CALORIES);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Barometer table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("KiloCalories Burned = %.3d Calories", event.getCalories() ));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.CaloriesTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.CaloriesTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.CaloriesTable.COLUMN_NAME_CALORIES, event.getCalories());


                writeDb.insert(DataStorageContract.CaloriesTable.TABLE_NAME, null, values);

            }
        }
    }







    private class CustomBandGsrEventListener implements BandGsrEventListener {
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandGsrEventListener (BandInfo bandInfo, String name) {
            info = bandInfo;
            uName = name;
            location = locations.get(info);
        }


        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                String T_Gsr = "GSR";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_Gsr, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_Gsr);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Barometer table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("Resistance = %d kOhms\n", event.getResistance()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_RESISTANCE, event.getResistance());


                writeDb.insert(DataStorageContract.GsrTable.TABLE_NAME, null, values);
            }
        }
    }

    private class CustomBandHeartRateEventListener implements BandHeartRateEventListener {

        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandHeartRateEventListener (BandInfo bandInfo, String name) {
            info = bandInfo;
            uName = name;
            location = locations.get(info);
        }

        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                String T_HR = "heartRate";

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(readDb);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    writeDb.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_HR, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(readDb);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_HR);

                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Barometer table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, String.format("Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()));
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.HeartRateTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.HeartRateTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.HeartRateTable.COLUMN_NAME_HEART_RATE, event.getHeartRate());
                values.put(DataStorageContract.HeartRateTable.COLUMN_NAME_QUALITY, event.getQuality().toString());


                writeDb.insert(DataStorageContract.HeartRateTable.TABLE_NAME, null, values);
            }
        }
    }


    /* ********* Tasks ******** */

    // Accelerometer
    private class AccelerometerSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                Log.v(TAG, "Got the band");
                try {
                    if (!accClients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            BandAccelerometerEventListenerCustom aListener =
                                    new BandAccelerometerEventListenerCustom(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerAccelerometerEventListener(
                                    aListener, SampleRate.MS128);

                            // Save the listener and client
                            accListeners.put(band, aListener);
                            accClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream accelerometer from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting accelerometer data");
                }
            }
            return null;
        }
    }

    private class AccelerometerUnsubscribe extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (accClients.containsKey(band)) {

                    BandClient client = accClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterAccelerometerEventListener(
                                accListeners.get(band)
                        );

                        // Remove listener from list
                        accListeners.remove(band);
                        // Remove client from list
                        accClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }


    private class AltimeterSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                Log.v(TAG, "Got the band");
                try {
                    if (!altClients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandAltimeterEventListener aListener =
                                    new CustomBandAltimeterEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerAltimeterEventListener(
                                    aListener);

                            // Save the listener and client
                            altListeners.put(band, aListener);
                            altClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream altimeter from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting altimeter data");
                }
            }
            return null;
        }
    }

    private class AltimeterUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (altClients.containsKey(band)) {

                    BandClient client = altClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterAltimeterEventListener(
                                altListeners.get(band)
                        );

                        // Remove listener from list
                        altListeners.remove(band);
                        // Remove client from list
                        altClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }


    private class AmbientLightSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!ambientClients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandAmbientLightEventListener aListener =
                                    new CustomBandAmbientLightEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerAmbientLightEventListener(
                                    aListener);

                            // Save the listener and client
                            ambientListeners.put(band, aListener);
                            ambientClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on " +
                                    "and the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream ambient sensor from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting ambient light data");
                }
            }
            return null;
        }
    }

    private class AmbientUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (ambientClients.containsKey(band)) {

                    BandClient client = ambientClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterAmbientLightEventListener(
                                ambientListeners.get(band)
                        );

                        // Remove listener from list
                        ambientListeners.remove(band);
                        // Remove client from list
                        ambientClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }


    private class BarometerSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!barometerClients.containsKey(band)) {
                        // No registered clients streaming barometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandBarometerEventListener aListener =
                                    new CustomBandBarometerEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerBarometerEventListener(
                                    aListener);

                            // Save the listener and client
                            barometerListeners.put(band, aListener);
                            barometerClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream barometer sensor from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting barometer data");
                }
            }
            return null;
        }
    }

    private class BarometerUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (barometerClients.containsKey(band)) {

                    BandClient client = barometerClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterBarometerEventListener(
                                barometerListeners.get(band)
                        );

                        // Remove listener from list
                        barometerListeners.remove(band);
                        // Remove client from list
                        barometerClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private class CaloriesSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!caloriesClients.containsKey(band)) {
                        // No registered clients streaming calories data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandCaloriesEventListener aListener =
                                    new CustomBandCaloriesEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerCaloriesEventListener(
                                    aListener);

                            // Save the listener and client
                            caloriesListeners.put(band, aListener);
                            caloriesClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream barometer sensor from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting barometer data");
                }
            }
            return null;
        }
    }

    private class CaloriesUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (caloriesClients.containsKey(band)) {

                    BandClient client = caloriesClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterCaloriesEventListener(
                                caloriesListeners.get(band)
                        );

                        // Remove listener from list
                        caloriesListeners.remove(band);
                        // Remove client from list
                        caloriesClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }








    private class GsrSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!gsrClients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandGsrEventListener aListener =
                                    new CustomBandGsrEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerGsrEventListener(
                                    aListener);

                            // Save the listener and client
                            gsrListeners.put(band, aListener);
                            gsrClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream gsr sensor from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting gsr data");
                }
            }
            return null;
        }
    }

    private class GsrUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (gsrClients.containsKey(band)) {

                    BandClient client = gsrClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterGsrEventListener(
                                gsrListeners.get(band)
                        );

                        // Remove listener from list
                        gsrListeners.remove(band);
                        // Remove client from list
                        gsrClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private class HeartRateSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!heartRateClients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandHeartRateEventListener aListener =
                                    new CustomBandHeartRateEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerHeartRateEventListener(
                                    aListener);

                            // Save the listener and client
                            heartRateListeners.put(band, aListener);
                            heartRateClients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream heart rate sensor from this device ignored");
                    }
                } catch (BandException e) {
                    String exceptionMessage;
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your " +
                                    "SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. " +
                                    "Please make sure Microsoft Health is installed and that you " +
                                    "have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    Log.e(TAG, exceptionMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Unknown error occurred when getting heart rate data");
                }
            }
            return null;
        }
    }

    private class HeartRateUnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (heartRateClients.containsKey(band)) {

                    BandClient client = heartRateClients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterHeartRateEventListener(
                                heartRateListeners.get(band)
                        );

                        // Remove listener from list
                        heartRateListeners.remove(band);
                        // Remove client from list
                        heartRateClients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    // General
    private BandClient connectBandClient(BandInfo band, BandClient client) throws InterruptedException, BandException {
        if (client == null) {
            client = BandClientManager.getInstance().create(getBaseContext(), band);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return client;
        }

        Log.v(TAG, "Loading band connection client...\n");
        if (ConnectionState.CONNECTED == client.connect().await())
            return client;
        else
            return null;
    }


    private class disconnectClient extends AsyncTask<BandClient, Void, Void> {
        @Override
        protected Void doInBackground(BandClient... params) {
//            if (params.length > 0) {
//                BandClient client;
//                try {
//                    client = connectBandClient(params[0]);
//                    try {
//                        client.disconnect().await();
//                    } catch (InterruptedException | BandException e) {
//                        e.printStackTrace();
//                    }
//                } catch (InterruptedException | BandException e) {
//                    e.printStackTrace();
//                }
//
//            }
            return null;
        }
    }

    /**
     * Gets the _ID value for the study in the database
     * @param studyId study name to search for
     * @param db database to search for the study name
     * @throws android.content.res.Resources.NotFoundException when study name cannot be found
     * @return the integer _ID or
     */
    private static int getStudyId(String studyId, SQLiteDatabase db) throws Resources.NotFoundException {

        // Querry databse for the study name
        String[] projection = new String[] {
                DataStorageContract.StudyTable._ID,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID
        };

        // Query for the specified studyId
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID + "=?",
                new String[] { studyId },
                null,
                null,
                null
        );
        cursor.moveToFirst();

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();


        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.StudyTable._ID));
        cursor.close();
        return tmp;
    }


    /**
     * Uses the next unused ID
     * @param db database to find the lowest study in
     * @return the lowest unused _ID
     */
    private static int getNewStudy(SQLiteDatabase db) {
        String[] projection = new String[] {
                DataStorageContract.StudyTable._ID,
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.StudyTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at study entry with largest ID
        int studyIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.StudyTable._ID);

        int tmp = cursor.getInt(studyIdCol) + 1;
        cursor.close();
        return tmp;
    }

    /**
     * Gets the device id for the device specified
     * @param mac address (physical) of the device
     * @param study id of the study
     * @param db database to query
     * @throws android.content.res.Resources.NotFoundException
     * @return id of the device
     */
    private static int getDevId(String location, String mac, int study, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[] {
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC,
                DataStorageContract.DeviceTable._ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION
        };


        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC + "=?" + " AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID + "=?" +" AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION + "=?",
                new String[] { mac, Integer.toString(study), location},
                null,
                null,
                null
        );

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();

        cursor.moveToFirst();

        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.DeviceTable._ID));
        cursor.close();
        return tmp;
    }

    /**
     * Gets the next largest ID for the device
     * @param db to search
     * @return int available ID in the device list
     */
    private static int getNewDev(SQLiteDatabase db) {
        String[] projection = new String[] {
                DataStorageContract.DeviceTable._ID
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.DeviceTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at Study entry with largest ID
        int devIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.DeviceTable._ID);

        int tmp = cursor.getInt(devIdCol) + 1;
        cursor.close();
        return tmp;
    }

    /**
     * Gets the ID for the sensor associated with the device and sensor type
     * @param type of sensor
     * @param device ID in the SQLite db associated with the sensor
     * @param db to query
     * @throws android.content.res.Resources.NotFoundException
     * @return ID of the sensor or not
     */
    private static int getSensorId(String type, int device, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[] {
                DataStorageContract.SensorTable.COLUMN_NAME_TYPE,
                DataStorageContract.SensorTable._ID,
                DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID
        };


        Cursor cursor = db.query(
                DataStorageContract.SensorTable.TABLE_NAME,
                projection,
                DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID + "=?" +
                        " AND " +
                        DataStorageContract.SensorTable.COLUMN_NAME_TYPE + "=?",
                new String[] { Integer.toString(device), type},
                null,
                null,
                null
        );

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();

        cursor.moveToFirst();

        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.SensorTable._ID));
        cursor.close();
        return tmp;
    }

    private static int getNewSensor(SQLiteDatabase db) {
        String[] projection = new String[] {
                DataStorageContract.SensorTable._ID
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.SensorTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.SensorTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at Study entry with largest ID
        int sensIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.SensorTable._ID);

        int tmp = cursor.getInt(sensIdCol) + 1;
        cursor.close();
        return tmp;
    }


    public class StopAllStreams extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
//            Iterator<BandInfo> bandIter = bandStreams.keySet().iterator();
//            BandInfo mBand;
//            BandClient client;
//            while (bandIter.hasNext()) {
//                mBand = bandIter.next();
//                client = null;
//                Log.v(TAG, "Disconnecting a band: " + mBand.getMacAddress());
//                try {
//                    try {
//                        client = connectBandClient(mBand, client);
//                    } catch (InterruptedException | BandException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        bandClients.get(mBand).disconnect().await();
//                    } catch (InterruptedException | BandException e) {
//                        e.printStackTrace();
//                    }
//                } catch (Exception e) {
//                    // The band was not connected, do nothing
//                    e.printStackTrace();
//                }
//                Log.v(TAG, "Removing the band");
//                Log.v(TAG, "Band removed.");
//            }
//
//            bandClients.clear();

            return null;
        }
    }
}
