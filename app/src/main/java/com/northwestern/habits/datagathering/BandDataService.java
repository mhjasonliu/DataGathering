package com.northwestern.habits.datagathering;

import android.app.Notification;
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

    private BandInfo band = null;
    private BandClient client = null;
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, BandClient> connectedBands = new HashMap<>();

    private DataStorageContract.BluetoothDbHelper mDbHelper;

    private String userName;

    // Maps of listeners
    private HashMap<BandClient, BandAccelerometerEventListenerCustom> accListeners = new HashMap<>();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started the service.");
        startForeground(1, new Notification());

        Log.v(TAG, "Retrieving database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());

        // Get the band info, client, and data required
        Bundle extras = intent.getExtras();
        if (extras != null){
            if (!extras.getBoolean("continueStudy")) {
                Log.v(TAG, "Ending study");
                // Unregister all clients
                new StopAllStreams().execute();

            } else {
                int index = extras.getInt("index");
                band = pairedBands[index];
                client = BandClientManager.getInstance().create(getBaseContext(), band);
                modes.put("accelerometer", extras.getBoolean("accelerometer"));
                modes.put("altimeter", extras.getBoolean("altimeter"));
                modes.put("ambient light", extras.getBoolean("ambient light"));
                modes.put("barometer", extras.getBoolean("barometer"));
                modes.put("gsr", extras.getBoolean("gsr"));
                modes.put("heart rate", extras.getBoolean("heart rate"));

                // Set the user and device
                userName = extras.getString("userId");

                if (extras.getBoolean("stopStream")){
                    Log.v(TAG, "Stop stream requested.");
                    if (connectedBands.containsKey(band)) {
                        // Unsubscribe from specified tasks
                        if (modes.get("accelerometer")) {
                            // Start an accelerometer task
                            Log.v(TAG, "Unsubscribe from accelerometer");
                            new AccelerometerUnsubscribe().execute();
                        }

                        if (modes.get("altimeter"))
                            new AltimeterSubscriptionTask().execute();
                        if (modes.get("ambient light"))
                            new AmbientLightSubscriptionTask().execute();
                        if (modes.get("barometer"))
                            new BarometerSubscriptionTask().execute();
                        if (modes.get("gsr"))
                            new GsrSubscriptionTask().execute();
                        if (modes.get("heart rate"))
                            new HeartRateSubscriptionTask().execute();
                    }
                } else {
                    if (connectedBands.containsKey(band)) {
                        // Disconnect from band
                        new disconnectClient().execute(connectedBands.get((band)));
                        connectedBands.remove(band);
                    } else {
                        // Request data
                        if (modes.get("accelerometer")) {
                            // Start an accelerometer task
                            new AccelerometerSubscriptionTask().execute();
                        }

                        if (modes.get("altimeter"))
                            new AltimeterSubscriptionTask().execute();
                        if (modes.get("ambient light"))
                            new AmbientLightSubscriptionTask().execute();
                        if (modes.get("barometer"))
                            new BarometerSubscriptionTask().execute();
                        if (modes.get("gsr"))
                            new GsrSubscriptionTask().execute();
                        if (modes.get("heart rate"))
                            new HeartRateSubscriptionTask().execute();


                        // Add the band to connected list
                        connectedBands.put(band, client);
                    }
                }
            }
        }
        return Service.START_STICKY;
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


                int userId, devId, sensId;
                try {
                    userId = getUserId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // User not found, use lowest available
                    userId = getNewUser(readDb);


                    // Write the user into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.UserTable.COLUMN_NAME_USER_NAME, uName);
                    values.put(DataStorageContract.UserTable._ID, userId);
                    writeDb.insert(
                            DataStorageContract.UserTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(info.getMacAddress(), userId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID, userId);
                    String t_BAND2 = "Microsoft_Band_2";
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, t_BAND2);
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
                Log.v(TAG, "User name is: " + uName);
                Log.v(TAG, "User Id is: " + Integer.toString(userId));
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


    private BandAltimeterEventListener mAltimeterEventListener = new BandAltimeterEventListener() {
        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {
                Log.v(TAG, String.format("Total Gain = %d cm\n", event.getTotalGain()) +
                        String.format("Total Loss = %d cm\n", event.getTotalLoss()) +
                        String.format("Stepping Gain = %d cm\n", event.getSteppingGain()) +
                        String.format("Stepping Loss = %d cm\n", event.getSteppingLoss()) +
                        String.format("Steps Ascended = %d\n", event.getStepsAscended()) +
                        String.format("Steps Descended = %d\n", event.getStepsDescended()) +
                        String.format("Rate = %f cm/s\n", event.getRate()) +
                        String.format("Flights of Stairs Ascended = %d\n", event.getFlightsAscended()) +
                        String.format("Flights of Stairs Descended = %d\n", event.getFlightsDescended()));
            }
        }
    };


    private BandAmbientLightEventListener mAmbientLightEventListener = new BandAmbientLightEventListener() {
        @Override
        public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
            if (event != null) {
                Log.v(TAG, String.format("Brightness = %d lux\n", event.getBrightness()));
            }
        }
    };

    private BandBarometerEventListener mBarometerEventListener = new BandBarometerEventListener() {
        @Override
        public void onBandBarometerChanged(final BandBarometerEvent event) {
            if (event != null) {
                Log.v(TAG, String.format("Air Pressure = %.3f hPa\n"
                        + "Temperature = %.2f degrees Celsius", event.getAirPressure(), event.getTemperature()));
            }
        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                Log.v(TAG, String.format("Resistance = %d kOhms\n", event.getResistance()));
            }
        }
    };

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                Log.v(TAG, String.format("Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()));
            }
        }
    };


    /* ********* Tasks ******** */

    // Accelerometer
    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    Log.v(TAG, "Band is connected.\n");
                    BandAccelerometerEventListenerCustom aListener =
                            new BandAccelerometerEventListenerCustom(band, userName);
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
                        client.getSensorManager().registerAltimeterEventListener(mAltimeterEventListener);
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
                        client.getSensorManager().registerAmbientLightEventListener(mAmbientLightEventListener);
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
                        client.getSensorManager().registerBarometerEventListener(mBarometerEventListener);
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
                        client.getSensorManager().registerGsrEventListener(mGsrEventListener);
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
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        Log.e(TAG, "You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                        //TODO get permissions to record heartrate data
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

        Log.v(TAG, "Band is connecting...\n");
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
     * Gets the _ID value for the user name in the database
     * @param userName user name to search for
     * @param db database to search for the user name
     * @throws android.content.res.Resources.NotFoundException when user name cannot be found
     * @return the integer _ID or
     */
    private static int getUserId(String userName, SQLiteDatabase db) throws Resources.NotFoundException {

        // Querry databse for the user name
        String[] projection = new String[] {
                DataStorageContract.UserTable._ID,
                DataStorageContract.UserTable.COLUMN_NAME_USER_NAME
        };

        // Query for the specified userName
        Cursor cursor = db.query(
                DataStorageContract.UserTable.TABLE_NAME,
                projection,
                DataStorageContract.UserTable.COLUMN_NAME_USER_NAME + "=?",
                new String[] { userName },
                null,
                null,
                null
        );
        cursor.moveToFirst();

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();


        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.UserTable._ID));
        cursor.close();
        return tmp;
    }


    /**
     * Uses the next unused ID
     * @param db database to find the lowest user in
     * @return the lowest unused _ID
     */
    private static int getNewUser(SQLiteDatabase db) {
        String[] projection = new String[] {
                DataStorageContract.UserTable._ID,
        };

        // Get the table of users
        Cursor cursor = db.query(
                DataStorageContract.UserTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.UserTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at User entry with largest ID
        int userIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.UserTable._ID);

        int tmp = cursor.getInt(userIdCol) + 1;
        cursor.close();
        return tmp;
    }

    /**
     * Gets the device id for the device specified
     * @param mac address (physical) of the device
     * @param user id of the user
     * @param db database to query
     * @throws android.content.res.Resources.NotFoundException
     * @return id of the device
     */
    private static int getDevId(String mac, int user, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[] {
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC,
                DataStorageContract.DeviceTable._ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID
        };


        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC + "=?" +
                        " AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID + "=?",
                new String[] { mac, Integer.toString(user)},
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

        // Get the table of users
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

        // Cursor currently points at User entry with largest ID
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

        // Get the table of users
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

        // Cursor currently points at User entry with largest ID
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
