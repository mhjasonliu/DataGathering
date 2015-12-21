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
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSensorEvent;
import com.microsoft.band.sensors.SampleRate;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class BandDataService extends Service {

    public static final String ACCEL_REQ_EXTRA = "accelerometer";
    public static final String ALT_REQ_EXTRA = "altimeter";
    public static final String AMBIENT_REQ_EXTRA = "ambient";
    public static final String BAROMETER_REQ_EXTRA = "barometer";
    public static final String GSR_REQ_EXTRA = "gsr";
    public static final String HEART_RATE_REQ_EXTRA = "heartRate";

    public static final String INDEX_EXTRA = "index";
    public static final String STUDY_ID_EXTRA = "study";

    public static final String CONTINUE_STUDY_EXTRA = "continue study";
    public static final String STOP_STREAM_EXTRA = "stop stream";



    private BandInfo band = null;
    private BandClient client = null;
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, BandClient> connectedBands = new HashMap<>();

    private DataStorageContract.BluetoothDbHelper mDbHelper;

    private String studyName;

    // Maps of listeners
    private HashMap<BandClient, BandAccelerometerEventListenerCustom> accListeners = new HashMap<>();


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
                Log.v(TAG, "Ending study");
                // Unregister all clients
                new StopAllStreams().execute();

            } else {
                int index = extras.getInt(INDEX_EXTRA);
                band = pairedBands[index];
                client = BandClientManager.getInstance().create(getBaseContext(), band);
                modes.put(ACCEL_REQ_EXTRA, extras.getBoolean(ACCEL_REQ_EXTRA));
                modes.put(ALT_REQ_EXTRA, extras.getBoolean(ALT_REQ_EXTRA));
                modes.put(AMBIENT_REQ_EXTRA, extras.getBoolean(AMBIENT_REQ_EXTRA));
                modes.put(BAROMETER_REQ_EXTRA, extras.getBoolean(BAROMETER_REQ_EXTRA));
                modes.put(GSR_REQ_EXTRA, extras.getBoolean(GSR_REQ_EXTRA));
                modes.put(HEART_RATE_REQ_EXTRA, extras.getBoolean(HEART_RATE_REQ_EXTRA));

                // Set the study and device
                studyName = extras.getString(STUDY_ID_EXTRA);
                Log.v(TAG, "Study name is: " + studyName);

                if (extras.getBoolean(STOP_STREAM_EXTRA)){
                    Log.v(TAG, "Stop stream requested.");
                    if (connectedBands.containsKey(band)) {
                        // Unsubscribe from specified tasks
                        if (modes.get(ACCEL_REQ_EXTRA)) {
                            // Start an accelerometer task
                            Log.v(TAG, "Unsubscribe from accelerometer");
                            new AccelerometerUnsubscribe().execute();
                        }

                        if (modes.get(ALT_REQ_EXTRA))
                            new AltimeterSubscriptionTask().execute();
                        if (modes.get(AMBIENT_REQ_EXTRA))
                            new AmbientLightSubscriptionTask().execute();
                        if (modes.get(BAROMETER_REQ_EXTRA))
                            new BarometerSubscriptionTask().execute();
                        if (modes.get(GSR_REQ_EXTRA))
                            new GsrSubscriptionTask().execute();
                        if (modes.get(HEART_RATE_REQ_EXTRA))
                            new HeartRateSubscriptionTask().execute();
                    }
                } else {
                    if (connectedBands.containsKey(band)) {
                        // Disconnect from band
                        new disconnectClient().execute(connectedBands.get((band)));
                        Log.v(TAG, "Disconnected from the band");
                        connectedBands.remove(band);
                    } else {
                        // Request data
                        if (modes.get(ACCEL_REQ_EXTRA)) {
                            // Start an accelerometer task
                            new AccelerometerSubscriptionTask().execute();
                        }

                        if (modes.get(ALT_REQ_EXTRA))
                            new AltimeterSubscriptionTask().execute();
                        if (modes.get(AMBIENT_REQ_EXTRA))
                            new AmbientLightSubscriptionTask().execute();
                        if (modes.get(BAROMETER_REQ_EXTRA))
                            new BarometerSubscriptionTask().execute();
                        if (modes.get(GSR_REQ_EXTRA))
                            new GsrSubscriptionTask().execute();
                        if (modes.get(HEART_RATE_REQ_EXTRA))
                            new HeartRateSubscriptionTask().execute();


                        // Add the band to connected list
                        connectedBands.put(band, client);
                    }
                }
            }
        }
        return Service.START_NOT_STICKY;
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

        public BandAccelerometerEventListenerCustom(BandInfo mInfo, String name) {
            super();
            info = mInfo;
            uName = name;
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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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
        }

        private String uName;
        private BandInfo info;

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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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
        BandInfo info;
        String uName;

        public CustomBandAmbientLightEventListener(BandInfo bInfo, String name) {
            super();
            info = bInfo;
            uName = name;
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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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
        BandInfo info;
        String uName;

        public CustomBandBarometerEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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

    private class CustomBandGsrEventListener implements BandGsrEventListener {
        BandInfo info;
        String uName;

        public CustomBandGsrEventListener (BandInfo bandInfo, String name) {
            info = bandInfo;
            uName = name;
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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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

        BandInfo info;
        String uName;

        public CustomBandHeartRateEventListener (BandInfo bandInfo, String name) {
            info = bandInfo;
            uName = name;
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
                    devId = getDevId(info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());

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
    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    Log.v(TAG, "Band is connected.\n");
                    BandAccelerometerEventListenerCustom aListener =
                            new BandAccelerometerEventListenerCustom(band, studyName);
                    client.getSensorManager().registerAccelerometerEventListener(
                            aListener,  SampleRate.MS128);

                    accListeners.put(client, aListener);

                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, "Unknown error occurred when getting accelerometer data");
            }
            return null;
        }
    }


    private class AccelerometerUnsubscribe extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {


                if (getConnectedBandClient()) {
                    Log.v(TAG, "Unregistering accelerometer listener");
                    client.getSensorManager().unregisterAccelerometerEventListener(
                            accListeners.get(client)
                    );
                    accListeners.remove(client);
                    Log.v(TAG, "Removed client");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class AltimeterSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.v(TAG, "Band is connected.\n");
                        client.getSensorManager().registerAltimeterEventListener(new CustomBandAltimeterEventListener(band, studyName));
                    } else {
                        Log.e(TAG, "The Altimeter sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    private class AmbientLightSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.v(TAG, "Band is connected.\n");
                        client.getSensorManager().registerAmbientLightEventListener(
                                new CustomBandAmbientLightEventListener(band, studyName)
                        );
                    } else {
                        Log.e(TAG, "The Ambient Light sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occurred: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    private class BarometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.v(TAG, "Band is connected.\n");
                        client.getSensorManager().registerBarometerEventListener(
                                new CustomBandBarometerEventListener(band, studyName)
                        );
                    } else {
                        Log.e(TAG, "The Barometer sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.v(TAG, "Band is connected.\n");
                        client.getSensorManager().registerGsrEventListener(
                                new CustomBandGsrEventListener(band, studyName)
                        );
                    } else {
                        Log.e(TAG, "The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(
                                new CustomBandHeartRateEventListener(band, studyName)
                        );
                    } else {
                        Log.e(TAG, "You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    // General
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            client = BandClientManager.getInstance().create(getBaseContext(), band);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        Log.v(TAG, "Loading band connection client...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }


    private class disconnectClient extends AsyncTask<BandClient, Void, Void> {
        @Override
        protected Void doInBackground(BandClient... params) {
            try {
                getConnectedBandClient();
            } catch (InterruptedException | BandException e) {
                e.printStackTrace();
            }

            try {
                params[0].disconnect().await();
            } catch (InterruptedException | BandException e) {
                e.printStackTrace();
            }
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
    private static int getDevId(String mac, int study, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[] {
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC,
                DataStorageContract.DeviceTable._ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID
        };


        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC + "=?" +
                        " AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID + "=?",
                new String[] { mac, Integer.toString(study)},
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
            Iterator<BandInfo> bandIter = connectedBands.keySet().iterator();
            BandInfo mBand;
            while (bandIter.hasNext()) {
                mBand = bandIter.next();
                Log.v(TAG, "Disconnecting a band: " + mBand.getMacAddress());
                try {
                    try {
                        getConnectedBandClient();
                    } catch (InterruptedException | BandException e) {
                        e.printStackTrace();
                    }
                    try {
                        connectedBands.get(mBand).disconnect().await();
                    } catch (InterruptedException | BandException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    // The band was not connected, do nothing
                    e.printStackTrace();
                }
                Log.v(TAG, "Removing the band");
                Log.v(TAG, "Band removed.");
            }

            connectedBands.clear();
            accListeners.clear();

            return null;
        }
    }
}
