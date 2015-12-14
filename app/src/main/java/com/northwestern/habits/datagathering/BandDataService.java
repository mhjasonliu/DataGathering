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
import com.microsoft.band.sensors.SampleRate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class BandDataService extends Service {

    private BandInfo band = null;
    private BandClient client = null;
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, BandClient> connectedBands = new HashMap<>();

    private DataStorageContract.BluetoothDbHelper mDbHelper;

    private String userName;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started the service.");

        Log.v(TAG, "Retrieving database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());

        // Get the band info, client, and data required
        Bundle extras = intent.getExtras();
        if (extras != null){
            int index = extras.getInt("index");
            Log.v(TAG, "Index is " + Integer.toString(index));
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
        }


        if (connectedBands.containsKey(band)) {
            // Disconnect from band
            client = connectedBands.get(band);
            new disconnectClient().execute();
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
            connectedBands.put(band,client);
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

    /**
     * Helper that gets the date and time in proper format for database
     */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
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

                SQLiteDatabase writeDb = mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = mDbHelper.getReadableDatabase();


                int userId, devId = -1, sensId = -1;
                try {
                    userId = getUserId(uName, readDb);
                    Log.v(TAG, "User found");
                } catch (Resources.NotFoundException e) {

                    Log.v(TAG, "User not found.");
                    // User not found, use lowest available
                    userId = getLowestUser(readDb);


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

                Cursor cursor;
                // See if devices has a device for the user
                String[] projection = new String[] {
                        DataStorageContract.DeviceTable._ID,
                        DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID,
                        DataStorageContract.DeviceTable.COLUMN_NAME_MAC
                };

                cursor = readDb.query(
                        DataStorageContract.DeviceTable.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        DataStorageContract.DeviceTable._ID + " ASC"
                );
                cursor.moveToFirst();

                int devMacCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.DeviceTable.COLUMN_NAME_MAC);
                int devIdCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.DeviceTable._ID);
                int userIdCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID);
                int smallestAvailableId = 0;

                // Find the device wanted
                while ( !cursor.isAfterLast() && devId == -1) {

                    if (info.getMacAddress().equals(cursor.getString(devMacCol))
                            && cursor.getInt(userIdCol) == userId) {
                        // Found device in table.
                        devId = cursor.getInt(devIdCol);
                    } else if (cursor.getInt(devIdCol) == smallestAvailableId) {
                        smallestAvailableId++;
                    }
                    cursor.moveToNext();
                }

                if ( devId == -1 ) {
                    // Write the device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable._ID, smallestAvailableId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, "Microsoft_Band_2");
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID, userId);
                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                    devId = smallestAvailableId;
                }

                // Check if the accelerometer exists
                projection = new String[] {
                        DataStorageContract.SensorTable._ID,
                        DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID,
                        DataStorageContract.SensorTable.COLUMN_NAME_TYPE
                };

                cursor = readDb.query(
                        DataStorageContract.SensorTable.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        DataStorageContract.SensorTable._ID + " ASC"
                );
                cursor.moveToFirst();

                int sensIdCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.SensorTable._ID);
                devIdCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID);
                int sensTypeCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.SensorTable.COLUMN_NAME_TYPE);
                smallestAvailableId = 0;

                // Find the sensor wanted
                while ( !cursor.isAfterLast() && sensId == -1) {

                    if (cursor.getString(sensTypeCol).equals("Accelerometer")
                            && cursor.getInt(devIdCol) == devId) {
                        // Found sensor in table.
                        sensId = cursor.getInt(sensIdCol);
                    } else if (cursor.getInt(sensIdCol) == smallestAvailableId) {
                        smallestAvailableId++;
                    }
                    cursor.moveToNext();
                }

                if ( sensId == -1 ) {
                    // Write the user into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, smallestAvailableId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, "Accelerometer");
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    writeDb.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                    sensId = smallestAvailableId;
                }


                cursor = readDb.query(
                        DataStorageContract.AccelerometerTable.TABLE_NAME,
                        new String[] { DataStorageContract.AccelerometerTable._ID },
                        null,
                        null,
                        null,
                        null,
                        DataStorageContract.AccelerometerTable._ID + " DESC",
                        "1"
                );
                cursor.moveToFirst();
                int dataIdCol = cursor.getColumnIndexOrThrow(
                        DataStorageContract.AccelerometerTable._ID);

                int dataId;
                try {
                    dataId = cursor.getInt(dataIdCol) + 1;
                } catch (Exception e) {
                    dataId = 0;
                }

                Log.v(TAG, "User name is: " + uName);
                Log.v(TAG, "User Id is: " + Integer.toString(userId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, "Data ID is: " + Integer.toString(dataId));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AccelerometerTable._ID, dataId);
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME, getDateTime());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_X, event.getAccelerationX());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y, event.getAccelerationY());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z, event.getAccelerationZ());


                writeDb.insert(DataStorageContract.AccelerometerTable.TABLE_NAME, null, values);

                cursor.close();
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
                    client.getSensorManager().registerAccelerometerEventListener(
                            new BandAccelerometerEventListenerCustom(band, userName),
                            SampleRate.MS128);

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


    private class disconnectClient extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                getConnectedBandClient();
            } catch (InterruptedException | BandException e) {
                e.printStackTrace();
            }

            try {
                client.disconnect().await();
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

        if (userName == null) {
            userName = "null";
        }

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


        return cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.UserTable._ID));
    }


    /**
     * Finds the lowest user _ID that is not being used
     * @param db database to find the lowest user in
     * @return the lowest unused _ID
     */
    private static int getLowestUser(SQLiteDatabase db) {

        int smallestAvailableId = 0;

        String[] projection = new String[] {
                DataStorageContract.UserTable._ID,
                DataStorageContract.UserTable.COLUMN_NAME_USER_NAME
        };

        // Get the table of users
        Cursor cursor = db.query(
                DataStorageContract.UserTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.UserTable._ID + " ASC"
        );
        cursor.moveToFirst();

        int userIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.UserTable._ID);

        // Find the user wanted
        while ( !cursor.isAfterLast() ) {

            if (cursor.getInt(userIdCol) == smallestAvailableId) {
                smallestAvailableId++;
            } else {
                return smallestAvailableId;
            }
            cursor.moveToNext();
        }

        return smallestAvailableId;
    }
}
