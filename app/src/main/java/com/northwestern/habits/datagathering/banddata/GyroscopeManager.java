package com.northwestern.habits.datagathering.banddata;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
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
public class GyroscopeManager extends DataManager {
    private SampleRate frequency;
    private final String T_Gyro = "Gyroscope";
    private TimeoutThread mTimeoutThread = new TimeoutThread();
    private final long TIMEOUT_INTERVAL = 1000;
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

    public GyroscopeManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "GyroscopeManager", db, context);
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }

    /* *********************************** THREADS *************************************** */

    private class SubscriptionThread extends Thread {
        private Runnable r;

        public SubscriptionThread(final BandInfo info) {
            super();

            r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!clients.containsKey(info)) {
                            // No registered clients streaming calories data
                            BandClient client = connectBandClient(info, null);
                            if (client != null &&
                                    client.getConnectionState() == ConnectionState.CONNECTED) {
                                // Create the listener
                                CustomBandGyroEventListener aListener =
                                        new CustomBandGyroEventListener(info, studyName);
                                // Register the listener
                                client.getSensorManager().registerGyroscopeEventListener(
                                        aListener, frequency);

                                // Save the listener and client
                                listeners.put(info, aListener);
                                clients.put(info, client);

                                // Toast saying connection successful
                                toastStreaming(T_Gyro);
                                if (mTimeoutThread.getState() == State.NEW ||
                                        mTimeoutThread.getState() == State.TERMINATED) {
                                    mTimeoutThread.start();
                                }
                            } else {
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                        "the band is in range.\n");

                                toastFailure();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream Gyro sensor from this device ignored");
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
                        Log.e(TAG, "Unknown error occurred when getting Gyro data");
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Unknown error connecting to "
                                        + T_Gyro, Toast.LENGTH_LONG).show();
                            }
                        });
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

        public UnsubscribeThread(final BandInfo info) {
            super();

            r = new Runnable() {
                @Override
                public void run() {
                    if (clients.containsKey(info)) {

                        BandClient client = clients.get(info);

                        // Unregister the client
                        try {
                            client.getSensorManager().unregisterGyroscopeEventListener(
                                    (BandGyroscopeEventListener) listeners.get(info)
                            );

                            // Remove listener from list
                            listeners.remove(info);
                            // Remove client from list
                            clients.remove(info);
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

    private class TimeoutThread extends Thread {
        @Override
        public void run() {
            while (true) {
                // Iterate through stored event handlers
                long timeout;
                long interval;
                for (EventListener listener :
                        listeners.values()) {
                    // Check timeout field
                    timeout = ((CustomBandGyroEventListener) listener).lastReceived;
                    interval = System.currentTimeMillis() - timeout;
                    if (timeout != 0
                            && interval > TIMEOUT_INTERVAL) {
                        // Timeout occurred, unsubscribe the current listener
                        new UnsubscribeThread(((CustomBandGyroEventListener) listener).info).run();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Gryo timeout detected...", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Subscribe again
                        subscribe(((CustomBandGyroEventListener) listener).info);
                        final int innerCount = restartCount++;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Successfully restarted Gyro for the " +
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
                    break;
                }
            }
        }
    }


    /* **************************** LISTENER *********************************** */

    private class CustomBandGyroEventListener implements BandGyroscopeEventListener, EventListener {

        private BandInfo info;
        private String uName;
        private String location;
        private long lastReceived;

        public CustomBandGyroEventListener(BandInfo mInfo, String name) {
            super();
            info = mInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
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
                    sensId = getSensorId(T_Gyro, devId, database);
                } catch (Resources.NotFoundException e) {
                    sensId = getNewSensor(database);

                    // Write new sensor into database, save id
                    ContentValues values = new ContentValues();
                    values.put(DataStorageContract.SensorTable._ID, sensId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                    values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_Gyro);

                    database.insert(
                            DataStorageContract.SensorTable.TABLE_NAME,
                            null,
                            values
                    );
                }

                // Add new entry to the gyro table
                Log.v(TAG, "G");
//                Log.v(TAG, "Study name is: " + uName);
//                Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
//                Log.v(TAG, "Device ID is: " + Integer.toString(devId));
//                Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
//                Log.v(TAG, "X: " + Double.toString(event.getAccelerationX()) +
//                        "Y: " + Double.toString(event.getAccelerationY()) +
//                        "Z: " + Double.toString(event.getAccelerationZ()));
//                Log.v(TAG, getDateTime(event));

                ContentValues values = new ContentValues();
                values.put(DataStorageContract.GyroTable.COLUMN_NAME_DATETIME, getDateTime(event));
                lastReceived = System.currentTimeMillis();
                values.put(DataStorageContract.GyroTable.COLUMN_NAME_SENSOR_ID, sensId);
                values.put(DataStorageContract.GyroTable.COLUMN_NAME_X, event.getAccelerationX());
                values.put(DataStorageContract.GyroTable.COLUMN_NAME_Y, event.getAccelerationY());
                values.put(DataStorageContract.GyroTable.COLUMN_NAME_Z, event.getAccelerationZ());


                database.insert(DataStorageContract.GyroTable.TABLE_NAME, null, values);


                // Insert into csv file
                File folder = new File(BandDataService.PATH);
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(cal.getTime());
                final String filename = folder.toString() + "/" + "Gyroscope " + formattedDate
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
                        fw.append("StudyName,StudyId,DeviceId,SensorId,Time,Gx,Gy,Gz\n");
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
