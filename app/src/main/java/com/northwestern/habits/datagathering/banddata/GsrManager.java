package com.northwestern.habits.datagathering.banddata;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by William on 12/31/2015
 */
public class GsrManager extends DataManager {
    public GsrManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "GsrManager", db, context);
        STREAM_TYPE = "GSR";
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }

    /* **************************** THREADS ********************************************** */

    private TimeoutHandler timeoutThread = new TimeoutHandler();

    private class SubscriptionThread extends Thread {
        Runnable r;

        public SubscriptionThread(final BandInfo bandInfo) {
            super();

            r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!clients.containsKey(bandInfo)) {
                            // No registered clients streaming gsr data
                            BandClient client = connectBandClient(bandInfo, null);
                            if (client != null &&
                                    client.getConnectionState() == ConnectionState.CONNECTED) {
                                // Create the listener
                                CustomBandGsrEventListener aListener =
                                        new CustomBandGsrEventListener(bandInfo, studyName);

                                // Register the listener
                                client.getSensorManager().registerGsrEventListener(
                                        aListener, GsrSampleRate.MS200);

                                // Save the listener and client
                                listeners.put(bandInfo, aListener);
                                clients.put(bandInfo, client);

                                // Toast saying connection successful
                                toastStreaming(STREAM_TYPE);
                                if (timeoutThread.getState() != State.NEW) {
                                    timeoutThread.makeThreadTerminate();
                                    timeoutThread = new TimeoutHandler();
                                }
                                timeoutThread.start();
                            } else {
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                        "the band is in range.\n");

                                toastFailure();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream gsr sensor from this device ignored");
                            toastAlreadyStreaming();
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


                        // trigger crash
                        ((BandInfo) null).getName();
                    }
                }
            };
        }

        @Override
        public void run() {
            r.run();
        }
    }

    private class UnsubscribeThread extends Thread {
        private Runnable r;

        public UnsubscribeThread(final BandInfo band) {
            super();
            r = new Runnable() {
                @Override
                public void run() {
                    if (clients.containsKey(band)) {

                        BandClient client = clients.get(band);

                        // Unregister the client
                        try {
                            client.getSensorManager().unregisterGsrEventListener(
                                    (BandGsrEventListener) listeners.get(band)
                            );
                            mHandler.post(unsubscribedToastRunnable);

                            // Remove listener from list
                            listeners.remove(band);
                            // Remove client from list
                            clients.remove(band);
                        } catch (BandIOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }

        @Override
        public void run() {
            r.run();
        }
    }


    /* ***************************** LISTENER ***************************************** */

    private class CustomBandGsrEventListener extends CustomListener
            implements BandGsrEventListener {
        private BandInfo info;
        private String uName;
        private String location;

        public CustomBandGsrEventListener (BandInfo bandInfo, String name) {
            info = bandInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }


        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {

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
                    sensId = getSensorId(STREAM_TYPE, devId, database);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(database);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, STREAM_TYPE);

                    database.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Resistance table
//                Log.v(TAG, "Study name is: " + uName);
//                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
//                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
//                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
//                Log.v(TAG, String.format("Resistance = %d kOhms\n", event.getResistance()));
//                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_DATETIME, getDateTime(event));
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.GsrTable.COLUMN_NAME_RESISTANCE, event.getResistance());


                database.insert(DataStorageContract.GsrTable.TABLE_NAME, null, values);

                // Insert into csv file
                File folder = new File(BandDataService.PATH);
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(cal.getTime());
                final String filename = folder.toString() + "/" + "GSR " + formattedDate
                        + uName + ".csv";

                File file = new File(filename);

                // If file does not exists, then create it
                boolean fpExists = true;
                if (!file.exists()) {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fpExists = false;
                }

                // Post data to the csv
                FileWriter fw;
                try {
                    fw = new FileWriter(filename, true);
                    if (!fpExists) {
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(file));
                        context.sendBroadcast(intent);
                        fw.append("StudyName,StudyId,DeviceId,SensorId,Time,Resistance\n");
                    }
                    fw.append(uName);
                    fw.append(',');
                    fw.append(Integer.toString(studyId));
                    fw.append(',');
                    fw.append(Integer.toString(devId));
                    fw.append(',');
                    fw.append(Integer.toString(sensId));
                    fw.append(',');
                    fw.append(getDateTime(event));
                    fw.append(',');
                    fw.append(Float.toString(event.getResistance()));
                    fw.append('\n');
                    fw.close();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to write to csv");
                    e.printStackTrace();
                }
            }
        }
    }
}
