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
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.util.EventListener;

/**
 * Created by William on 12/31/2015
 */
public class AltimeterManager extends DataManager {

    private final String TAG = "AltimeterManager";

    public AltimeterManager(String sName, SQLiteDatabase db, Context context) {
        super(sName, "AltimeterManager", db, context);
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionTask().execute(info);
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeTask().execute(info);
    }


    private class SubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
                Log.v(TAG, "Got the band");
                try {
                    if (!clients.containsKey(band)) {
                        // No registered clients streaming altimeter data
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
                            listeners.put(band, aListener);
                            clients.put(band, client);
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
                    e.printStackTrace();
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
                    Log.v(TAG, "Client found ");

                    // Unregister the client
                    try {
                        client.getSensorManager().unregisterAltimeterEventListener(
                                (BandAltimeterEventListener) listeners.get(band)
                        );

                        Log.v(TAG, "Unregistered listener");
                        // Remove listener from list
                        listeners.remove(band);
                        // Remove client from list
                        clients.remove(band);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e(TAG, "No arguments passed to UnsubscribeTask");
            }
            return null;
        }
    }


    private class CustomBandAltimeterEventListener implements BandAltimeterEventListener, EventListener {

        public CustomBandAltimeterEventListener(BandInfo bandInfo, String name) {
            super();
            uName = name;
            info = bandInfo;
            location = BandDataService.locations.get(info);
        }

        private String uName;
        private BandInfo info;
        private String location;

        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {

                String T_ALT = "Altimeter";


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
                    sensId = getSensorId(T_ALT, devId, database);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(database);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_ALT);

                    database.insert(
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


                database.insert(DataStorageContract.AltimeterTable.TABLE_NAME, null, values);
            }
        }

    }
}