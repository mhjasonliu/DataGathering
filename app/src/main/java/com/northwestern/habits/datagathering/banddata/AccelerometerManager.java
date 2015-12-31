package com.northwestern.habits.datagathering.banddata;

import android.content.ContentValues;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.util.EventListener;

/**
 * Created by William on 12/31/2015
 */
public class AccelerometerManager extends  DataManager{
    @Override
    protected void subscribe(BandInfo info) {
        new AccelerometerSubscriptionTask().execute(info);
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new AccelerometerUnsubscribe().execute(info);
    }


    public AccelerometerManager(String sName) {
        super(sName);
    }


    private final String TAG = "AccelerometerManager";


    private class AccelerometerSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                Log.v(TAG, "Got the band");
                try {
                    if (!clients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
                        BandClient client = BandDataService.connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            BandAccelerometerEventListenerCustom aListener =
                                    new BandAccelerometerEventListenerCustom(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerAccelerometerEventListener(
                                    aListener, SampleRate.MS128);

                            // Save the listener and client
                            listeners.put(band, aListener);
                            clients.put(band, client);
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

                if (clients.containsKey(band)) {

                    BandClient client = clients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterAccelerometerEventListener(
                                (BandAccelerometerEventListener) listeners.get(band)
                        );

                        // Remove listener from list
                        listeners.remove(band);
                        // Remove client from list
                        clients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private class BandAccelerometerEventListenerCustom implements BandAccelerometerEventListener, EventListener {

        private BandInfo info;
        private String uName;
        private String location;

        public BandAccelerometerEventListenerCustom(BandInfo mInfo, String name) {
            super();
            info = mInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                String T_ACCEL = "Accelerometer";

                SQLiteDatabase writeDb = BandDataService.mDbHelper.getWritableDatabase();
                SQLiteDatabase readDb = BandDataService.mDbHelper.getReadableDatabase();


                int studyId, devId, sensId;
                try {
                    studyId = BandDataService.getStudyId(uName, readDb);
                } catch (Resources.NotFoundException e) {

                    // study not found, use lowest available
                    studyId = BandDataService.getNewStudy(readDb);


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
                    devId = BandDataService.getDevId(location, info.getMacAddress(), studyId, readDb);
                } catch (Resources.NotFoundException e) {
                    devId = BandDataService.getNewDev(readDb);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, BandDataService.T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    writeDb.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = BandDataService.getSensorId(T_ACCEL, devId, readDb);
                } catch (Resources.NotFoundException e) {
                    sensId = BandDataService.getNewSensor(readDb);

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

                // Add new entry to the Accelerometertable
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, "X: " + Double.toString(event.getAccelerationX()) +
                        "Y: " + Double.toString(event.getAccelerationY()) +
                        "Z: " + Double.toString(event.getAccelerationZ()));
                Log.v(TAG, BandDataService.getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME, BandDataService.getDateTime(event));
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_X, event.getAccelerationX());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y, event.getAccelerationY());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z, event.getAccelerationZ());


                writeDb.insert(DataStorageContract.AccelerometerTable.TABLE_NAME, null, values);
            }

        }
    }
}
