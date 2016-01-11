package com.northwestern.habits.datagathering.banddata;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.util.EventListener;

/**
 * Created by William on 12/31/2015
 */
public class DistanceManager extends DataManager {
    public DistanceManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "AmbientManager", db, context);
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionTask().execute(info);
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeTask().execute(info);
    }

    /* ********************************* TASKS ************************************ */

    private class SubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!clients.containsKey(band)) {
                        // No registered clients streaming calories data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            CustomBandDistanceEventListener aListener =
                                    new CustomBandDistanceEventListener(band, studyName);

                            // Register the listener
                            client.getSensorManager().registerDistanceEventListener(
                                    aListener);

                            // Save the listener and client
                            listeners.put(band, aListener);
                            clients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream Distance sensor from this device ignored");
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
                    Log.e(TAG, "Unknown error occurred when getting Distance data");
                }
            }
            return null;
        }
    }

    private class UnsubscribeTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            if (params.length > 0) {
                BandInfo band = params[0];

                if (clients.containsKey(band)) {

                    BandClient client = clients.get(band);

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterDistanceEventListener(
                                (BandDistanceEventListener) listeners.get(band)
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


    /* ************************************* LISTENER *********************************** */


    private class CustomBandDistanceEventListener implements BandDistanceEventListener, EventListener {
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandDistanceEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandDistanceChanged(final BandDistanceEvent event) {
            if (event != null) {
                String T_DISTANCE = "Distance";


                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, database);
                } catch (Resources.NotFoundException e) {

                    // Study not found, use lowest available
                    studyId = getNewStudy(database);


                    // Write the study into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, uName);
                    values.put(DataStorageContract.StudyTable._ID, studyId);
                    database.insert(
                            DataStorageContract.StudyTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    devId = getDevId(location, info.getMacAddress(), studyId, database);
                } catch (Resources.NotFoundException e) {
                    devId = getNewDev(database);

                    // Write new Device into database, save the id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.DeviceTable._ID, devId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_BAND2);
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, info.getMacAddress());
                    values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                    database.insert(
                            DataStorageContract.DeviceTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                try {
                    sensId = getSensorId(T_DISTANCE, devId, database);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(database);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_DISTANCE);

                    database.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }
                String motionType = "";
                switch (event.getMotionType()){
                    case UNKNOWN:
                        motionType = "Unknown";
                        break;
                    case IDLE:
                        motionType = "Idle";
                        break;
                    case WALKING:
                        motionType = "Walking";
                        break;
                    case JOGGING:
                        motionType = "Jogging";
                        break;
                    case RUNNING:
                        motionType = "Running";
                        break;
                    default:
                        break;
                }


                // Add new entry to the Distance table
                Log.v(TAG, "Study name is: " + uName);
                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
                Log.v(TAG, "MotionType: " + motionType + "; Pace: "+ event.getPace() +
                        ", Speed: "+ event.getSpeed() + ", TotalDistance: "+ event.getTotalDistance() );
                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_CURRENT_MOTION, motionType);
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_PACE, event.getPace());
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_SPEED, event.getSpeed());
                values.put(DataStorageContract.DistanceTable.COLUMN_NAME_TOTAL_DISTANCE, event.getSpeed());


                database.insert(DataStorageContract.DistanceTable.TABLE_NAME, null, values);

            }
        }
    }


}
