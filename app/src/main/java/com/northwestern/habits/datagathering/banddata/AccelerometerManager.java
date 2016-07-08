package com.northwestern.habits.datagathering.banddata;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.northwestern.habits.datagathering.DataGatheringApplication;
import com.northwestern.habits.datagathering.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class AccelerometerManager extends DataManager {
    private Map<BandInfo, SampleRate> frequencies = new HashMap<>();

    protected void setFrequency(String f, BandInfo bandinfo) {
        switch (f) {
            case "8Hz":
                frequencies.put(bandinfo,SampleRate.MS128);
                break;
            case "31Hz":
                frequencies.put(bandinfo,SampleRate.MS32);
                break;
            case "62Hz":
                frequencies.put(bandinfo,SampleRate.MS16);
                break;
            default:
                frequencies.put(bandinfo,SampleRate.MS128);
        }

        // Record frequency change
        SharedPreferences prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(Preferences.getFrequencyKey(bandinfo.getMacAddress(), Preferences.ACCEL), f);
        e.apply();
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
                            // No registered clients streaming accelerometer data
//                        Log.v(TAG, "Getting client");
                            BandClient client = connectBandClient(info, null);
                            if (client != null &&
                                    client.getConnectionState() == ConnectionState.CONNECTED) {

//                            Log.v(TAG, "Creating listener");
                                // Create the listener
                                BandAccelerometerEventListenerCustom aListener =
                                        new BandAccelerometerEventListenerCustom(info, studyName);

                                // Get the sample rate
                                SampleRate rate = frequencies.get(info);
                                if (rate == null) {
                                    rate = SampleRate.MS128;
                                }

                                // Register the listener
                                client.getSensorManager().registerGyroscopeEventListener(
                                        aListener, rate);

                                // Save the listener and client
                                listeners.put(info, aListener);
                                clients.put(info, client);

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
                                reconnectBand();
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

    private class UnsubscribeThread extends Thread {
        private Runnable r;

        @Override
        public void run() {
            r.run();
        }

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
                                client.getSensorManager().unregisterGyroscopeEventListener(
                                        (BandGyroscopeEventListener) listeners.get(info)
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
            implements BandGyroscopeEventListener, EventListener {

        private String uName;
        private String location;
        private final int BUFFER_SIZE = 100;
        private JSONArray dataBuffer = new JSONArray();

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
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
//            Log.v(TAG, "accel changed");
            if (event != null) {
                this.lastDataSample = event.getTimestamp();
                JSONObject datapoint = new JSONObject();
                try {
                    datapoint.put("Time", event.getTimestamp());
                    datapoint.put("x", event.getAccelerationX());
                    datapoint.put("y", event.getAccelerationY());
                    datapoint.put("z", event.getAccelerationZ());

                    dataBuffer.put(datapoint);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (dataBuffer.length() >= BUFFER_SIZE) {
                    try {
                        DataGatheringApplication.getInstance().getCurrentDocument().update(new Document.DocumentUpdater() {
                            @Override
                            public boolean update(UnsavedRevision newRevision) {
                                Map<String, Object> properties = newRevision.getUserProperties();
                                properties.put(info.getMacAddress() + "_" + STREAM_TYPE
                                        + "_" +  getDateTime(event), dataBuffer.toString());
                                newRevision.setUserProperties(properties);
                                return true;
                            }
                        });
                        dataBuffer = new JSONArray();
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }
                }

//                // Get hour and date string from the event timestamp
//                int hour = DataGatheringApplication.getHourFromTimestamp(event.getTimestamp());
//                String date = DataGatheringApplication.getDateFromTimestamp(event.getTimestamp());
//
//                // Form the directory path and file name
//                String dirPath = DataGatheringApplication.getDataFilePath(context, hour);
//                String fileName = DataGatheringApplication.getDataFileName(
//                        DataStorageContract.AccelerometerTable.TABLE_NAME, hour, date, T_BAND2,
//                        info.getMacAddress());
//
//                // Create the directory if it does not exist
//                File directory = new File(dirPath);
//                if (!directory.exists()) {
//                    directory.mkdirs();
//                }
//
//                // Write to csv
//                File csv = new File(dirPath, fileName);
//                try {
//                    FileWriter fw;
//                    if (!csv.exists()) {
//                        csv.createNewFile();
//                        fw = new FileWriter(csv, true);
//                        fw.append("Time,x,y,z\n");
//                    } else {
//                        fw = new FileWriter(csv, true);
//                    }
//
//                    fw.append(getDateTime(event));
//                    fw.append(',');
//                    fw.append(Float.toString(event.getAccelerationX()));
//                    fw.append(',');
//                    fw.append(Float.toString(event.getAccelerationY()));
//                    fw.append(',');
//                    fw.append(Float.toString(event.getAccelerationZ()));
//                    fw.append('\n');
//                    fw.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
            }

        }
    }
}
