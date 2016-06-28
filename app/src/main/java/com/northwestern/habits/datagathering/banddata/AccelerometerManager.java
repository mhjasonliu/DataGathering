package com.northwestern.habits.datagathering.banddata;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
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
import com.northwestern.habits.datagathering.DataGatheringApplication;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EventListener;

/**
 * Created by William on 12/31/2015
 */
public class AccelerometerManager extends DataManager {
    private SampleRate frequency;

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
    protected void subscribe(final BandInfo info) {
        Log.v(TAG, "Subscribing to " + STREAM_TYPE);
        new SubscribeThread(info).start();
    }

    @Override
    protected void unSubscribe(final BandInfo info) {
        Log.v(TAG, "Unsubscribing from " + STREAM_TYPE);
        new UnsubscribeThread(info).start();
    }


    public AccelerometerManager(String sName, SQLiteOpenHelper dbHelper, Context context) {
        super(sName, "AccelerometerManager", dbHelper, context);
        STREAM_TYPE = "Accelerometer";
    }

    /* ******************************** THREADS ********************************************* */

    private class SubscribeThread extends Thread {
        private Runnable r;

        public SubscribeThread(final BandInfo info) {
            super();
            r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!clients.containsKey(info)) {
                            Log.d(TAG, "1");
                            // No registered clients streaming accelerometer data
//                        Log.v(TAG, "Getting client");
                            BandClient client = connectBandClient(info, null);
                            Log.d(TAG, "1.5");
                            if (client != null &&
                                    client.getConnectionState() == ConnectionState.CONNECTED) {
                                Log.d(TAG, "2");

//                            Log.v(TAG, "Creating listener");
                                // Create the listener
                                BandAccelerometerEventListenerCustom aListener =
                                        new BandAccelerometerEventListenerCustom(info, studyName);

                                Log.d(TAG, "3");
                                // Register the listener
                                client.getSensorManager().registerAccelerometerEventListener(
                                        aListener, frequency);

                                Log.d(TAG, "4");
                                // Save the listener and client
                                listeners.put(info, aListener);
                                clients.put(info, client);

                                Log.d(TAG, "5");
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
                            Log.w(TAG, "Multiple attempts to stream accelerometer from this device ignored");
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
                                        "have the correct permissions. aka SERVICE ERROR.\n";
                                break;
                            default:
                                exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                                break;
                        }
                        Log.e(TAG, exceptionMessage);
                        e.printStackTrace();
                        ((Integer) null).toString();
                    } catch (Exception e) {
                        Log.e(TAG, "Unknown error occurred when getting accelerometer data");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Unknown error connecting to "
                                        + STREAM_TYPE, Toast.LENGTH_LONG).show();
                            }
                        });
                        e.printStackTrace();
                    }
                }
            };
        }

        @Override
        public void run() {
            r.run();
        }
    }

    private class UnsubscribeThread extends Thread{
        private Runnable r;

        @Override
        public void run () {r.run();}

        public UnsubscribeThread(final BandInfo info) {
            super();

            r = new Runnable() {
                @Override
                public void run() {
                    if (info != null) {
                        if (clients.containsKey(info)) {
                            BandClient client = clients.get(info);

                            // Unregister the client
                            try {
                                Log.v(TAG, "Unsubscribing...");
                                client.getSensorManager().unregisterAccelerometerEventListener(
                                        (BandAccelerometerEventListener) listeners.get(info)
                                );
                                mHandler.post(unsubscribedToastRunnable);
                                Log.v(TAG, "Removing from lists");
                                // Remove listener from list
                                listeners.remove(info);
                                // Remove client from list
                                clients.remove(info);
//                        toastFailure();
                            } catch (BandIOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Tried to unsubscribe from band, but clients doesn't contain the info");
                        }
                    }
                }
            };
        }
    }

    private TimeoutHandler timeoutThread = new TimeoutHandler();

    private class BandAccelerometerEventListenerCustom extends CustomListener
            implements BandAccelerometerEventListener, EventListener {

        private String uName;
        private String location;
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Band info: ");
            builder.append(info.getName());
            builder.append("\nUser name: ");
            builder.append(uName);
            builder.append("\nLocation: ");
            builder.append(location);
            return builder.toString();
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
                // Get hour and date string from the event timestamp
                int hour = DataGatheringApplication.getHourFromTimestamp(event.getTimestamp());
                String date = DataGatheringApplication.getDateFromTimestamp(event.getTimestamp());

                // Form the directory path and file name
                String dirPath = DataGatheringApplication.getDataFilePath(context, hour);
                String fileName = DataGatheringApplication.getDataFileName(
                        DataStorageContract.AccelerometerTable.TABLE_NAME, hour, date, T_BAND2,
                        info.getMacAddress());

                // Create the directory if it does not exist
                File directory = new File(dirPath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Write to csv
                File csv = new File(dirPath, fileName);
                try {
                    FileWriter fw;
                    if (!csv.exists()) {
                        csv.createNewFile();
                        fw = new FileWriter(csv, true);
                        fw.append("Time,x,y,z\n");
                    } else {
                        fw = new FileWriter(csv, true);
                    }

                    fw.append(getDateTime(event));
                    fw.append(',');
                    fw.append(Float.toString(event.getAccelerationX()));
                    fw.append(',');
                    fw.append(Float.toString(event.getAccelerationY()));
                    fw.append(',');
                    fw.append(Float.toString(event.getAccelerationZ()));
                    fw.append('\n');
                    fw.close();
                    Log.v(TAG, "Wrote to " + csv.getPath());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }
}
