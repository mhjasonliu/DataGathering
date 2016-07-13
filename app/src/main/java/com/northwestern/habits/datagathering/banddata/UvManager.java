package com.northwestern.habits.datagathering.banddata;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.northwestern.habits.datagathering.CouchBaseData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class UvManager extends DataManager {

    public UvManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "UvManager", db, context);
        STREAM_TYPE = "UV";
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }

    /* ***************************** THREADS ****************************************** */

    private TimeoutHandler timeoutThread = new TimeoutHandler();

    private class SubscriptionThread extends Thread {
        private Runnable r;

        @Override
        public void run() {
            r.run();
        }

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
                                CustomBandUvEventListener aListener =
                                        new CustomBandUvEventListener(info, studyName);

                                // Register the listener
                                client.getSensorManager().registerUVEventListener(
                                        aListener);

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
                                client.disconnect();
                                toastFailure();
                                reconnectBand();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream UV sensor from this device ignored");
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
                        Log.e(TAG, "Unknown error occurred when getting UV data");
                    }
                }
            };
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

                    if (clients.containsKey(info)) {

                        BandClient client = clients.get(info);

                        // Unregister the client
                        try {
                            client.getSensorManager().unregisterUVEventListener(
                                    (BandUVEventListener) listeners.get(info)
                            );
                            mHandler.post(unsubscribedToastRunnable);

                            // Remove listener from list
                            listeners.remove(info);
                            // Remove client from list
                            client.disconnect();
                            clients.remove(info);
                        } catch (BandIOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
    }

    /* ******************************** LISTENER *************************************** */

    private class CustomBandUvEventListener extends CustomListener
            implements BandUVEventListener {
        private BandInfo info;
        private String uName;
        private String location;
        private final int BUFFER_SIZE = 100;
        private JSONArray dataBuffer = new JSONArray();

        public CustomBandUvEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandUVChanged(final BandUVEvent event) {
            if (event != null) {
                JSONObject datapoint = new JSONObject();
                try {
                    datapoint.put("Time", event.getTimestamp());
                    datapoint.put("Level", event.getUVIndexLevel());
                    try {
                        datapoint.put("UV_Exposure_Today", event.getUVExposureToday());
                    } catch (InvalidBandVersionException e) {
                        e.printStackTrace();
                    }

                    dataBuffer.put(datapoint);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (dataBuffer.length() >= BUFFER_SIZE) {
                    try {
                        CouchBaseData.getCurrentDocument(context).update(new Document.DocumentUpdater() {
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
//                        fw.append("Time,Level,UV_Exposure_Today\n");
//                    } else {
//                        fw = new FileWriter(csv, true);
//                    }
//
//                    fw.append(getDateTime(event));
//                    fw.append(',');
//                    fw.append(event.getUVIndexLevel().toString());
//                    fw.append(',');
//                    fw.append(Long.toString(event.getUVExposureToday()));
//                    fw.append('\n');
//                    fw.close();
//                    Log.v(TAG, "Wrote to " + csv.getPath());
//                } catch (IOException | InvalidBandVersionException e1) {
//                    e1.printStackTrace();
//                }
            }
        }
    }

}
