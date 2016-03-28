package com.northwestern.habits.datagathering.banddata;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EventListener;

/**
 * Created by William on 12/31/2015
 */
public class AccelerometerManager extends DataManager {
    private static final long TIMEOUT_INTERVAL = 1000;
    private SampleRate frequency;
    private final String T_ACCEL = "Accelerometer";
    private TimeoutTask mTimeoutTask = null;
    private int restartCount = 1;

    protected void setFrequency(String f) {
        switch (f) {
            case "8Hz":
                frequency = SampleRate.MS128;
                break;
            case "31Hz":
                frequency = SampleRate.MS32;
                break;
            case "62Hz":
                frequency = SampleRate.MS16;
                break;
            default:
                frequency = SampleRate.MS128;
        }
    }


    @Override
    protected void subscribe(BandInfo info) {
        Log.v(TAG, "Subscribing to " + T_ACCEL);
        new AccelerometerSubscriptionTask().executeOnExecutor(AccelerometerSubscriptionTask.THREAD_POOL_EXECUTOR, info);
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        Log.v(TAG, "Unsubscribing from " + T_ACCEL);
        new AccelerometerUnsubscribe().executeOnExecutor(AccelerometerUnsubscribe.THREAD_POOL_EXECUTOR, info);
    }


    public AccelerometerManager(String sName, SQLiteOpenHelper dbHelper, Context context) {
        super(sName, "AccelerometerManager", dbHelper, context);
    }


    private class AccelerometerSubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {
            if (params.length > 0) {
                BandInfo band = params[0];
//                Log.v(TAG, "Got the band");
                try {
                    if (!clients.containsKey(band)) {
                        // No registered clients streaming accelerometer data
//                        Log.v(TAG, "Getting client");
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {

//                            Log.v(TAG, "Creating listener");
                            // Create the listener
                            BandAccelerometerEventListenerCustom aListener =
                                    new BandAccelerometerEventListenerCustom(band, studyName);

//                            Log.v(TAG, "Registering listener");
                            // Register the listener
                            client.getSensorManager().registerAccelerometerEventListener(
                                    aListener, frequency);

                            // Save the listener and client
//                            Log.v(TAG, "putting listener");
                            listeners.put(band, aListener);
//                            Log.v(TAG, "Putting client");
                            clients.put(band, client);

                            // Toast saying connection successful
                            toastStreaming(T_ACCEL);
//                            Log.e(TAG, "Timeout task is ");// + mTimeoutTask);
                            if (mTimeoutTask == null) {
                                mTimeoutTask = new TimeoutTask();
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(TAG, "Running mTimeoutTask");
                                        mTimeoutTask.execute();
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");

                            toastFailure();
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream accelerometer from this device ignored");
                        Log.v(TAG, listeners.toString());
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
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Unknown error connecting to "
                                    + T_ACCEL, Toast.LENGTH_LONG).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
            return null;
        }
//
//        @Override
//        protected void onPostExecute(Void result) {
//            // Start timeout task if one isn't already running
//            if (mTimeoutTask == null) {
//                mTimeoutTask = new TimeoutTask();
//                mTimeoutTask.executeOnExecutor(THREAD_POOL_EXECUTOR);
//            }
//        }

    }

    private class AccelerometerUnsubscribe extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            Log.v(TAG, "1");
            if (params.length > 0) {
                BandInfo band = params[0];

                Log.v(TAG, "2");
                if (clients.containsKey(band)) {

                    Log.v(TAG, "3");
                    BandClient client = clients.get(band);

                    // Unregister the client
                    try {
                        Log.e(TAG, "Unsubscribing...");
                        client.getSensorManager().unregisterAccelerometerEventListener(
                                (BandAccelerometerEventListener) listeners.get(band)
                        );

                        Log.v(TAG, "Removing from lists");
                        // Remove listener from list
                        listeners.remove(band);
                        // Remove client from list
                        clients.remove(band);
//                        toastFailure();
                    } catch (BandIOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Tried to unsubscribe from band, but clients doesn't contain the info");
                }
            }
            return null;
        }
    }

    private class TimeoutTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                // Iterate through stored event handlers
                long timeout;
                long interval;
                for (EventListener listener :
                        listeners.values()) {
                    // Check timeout field
                    timeout = ((BandAccelerometerEventListenerCustom) listener).lastReceived;
                    interval = System.currentTimeMillis() - timeout;
                    if (timeout != 0
                            && interval > TIMEOUT_INTERVAL) {
                        // Timeout occurred, unsubscribe the current listener
                        new AccelerometerUnsubscribe().doInBackground(((BandAccelerometerEventListenerCustom) listener).info);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Accelerometer timeout detected...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        // Subscribe again
                        new AccelerometerSubscriptionTask().doInBackground(((BandAccelerometerEventListenerCustom) listener).info);
                        final int innerCount = restartCount++;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Successfully restarted Accelerometer for the " +
                                        Integer.toString(innerCount) +
                                        "th time", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                try {
                    Thread.sleep(TIMEOUT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (listeners.size() == 0) {
                    // All listeners have been unsubscribed
                    mTimeoutTask = null;
                    break;
                }
            }
            return null;
        }
    }

    private class BandAccelerometerEventListenerCustom implements BandAccelerometerEventListener, EventListener {

        private BandInfo info;
        private String uName;
        private String location;
        protected long lastReceived = 0;

        @Override
        public String toString() {
            StringBuilder builter = new StringBuilder();
            builter.append("Band info: ");
            builter.append(info.getName());
            builter.append("\nUser name: ");
            builter.append(uName);
            builter.append("\nLocation: ");
            builter.append(location);
            return builter.toString();
        }

        public BandAccelerometerEventListenerCustom(BandInfo mInfo, String name) {
            super();
            info = mInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                int studyId, devId, sensId;
                try {
                    studyId = getStudyId(uName, database);
                } catch (Resources.NotFoundException e) {

                    // study not found, use lowest available
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
                    sensId = getSensorId(T_ACCEL, devId, database);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(database);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_ACCEL);

                    database.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the Accelerometer table
                Log.v(TAG, "Event Received");
//                Log.v(TAG, "Study name is: " + uName);
//                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
//                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
//                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
//                Log.v(TAG, "X: " + Double.toString(event.getAccelerationX()) +
//                        "Y: " + Double.toString(event.getAccelerationY()) +
//                        "Z: " + Double.toString(event.getAccelerationZ()));
//                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME, getDateTime(event));
                lastReceived = System.currentTimeMillis();
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_X, event.getAccelerationX());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y, event.getAccelerationY());
                values.put(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z, event.getAccelerationZ());


                database.insert(DataStorageContract.AccelerometerTable.TABLE_NAME, null, values);

                // Insert into csv file
                File folder = new File(BandDataService.PATH);
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(cal.getTime());
                final String filename = folder.toString() + "/" + "Accel" + formattedDate.toString()
                        + uName + ".csv";

                File file = new File(filename);

                // If file does not exists, then create it
                boolean fpExists = true;
                if (!file.exists()) {
                    try {
                        boolean fb = file.createNewFile();
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
                        fw.append("StudyName,StudyId,DeviceId,SensorId,Time,Accx,Accy,Accz\n");
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
                    fw.append(Float.toString(event.getAccelerationX()));
                    fw.append(',');
                    fw.append(Float.toString(event.getAccelerationY()));
                    fw.append(',');
                    fw.append(Float.toString(event.getAccelerationZ()));
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
