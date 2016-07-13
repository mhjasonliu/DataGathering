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
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.northwestern.habits.datagathering.CouchBaseData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class AltimeterManager extends DataManager {

    private final String TAG = "AltimeterManager";
    private TimeoutHandler timeoutThread = new TimeoutHandler();

    public AltimeterManager(String sName, SQLiteOpenHelper dbHelper, Context context) {
        super(sName, "AltimeterManager", dbHelper, context);
        STREAM_TYPE = "Altimeter";
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }


    private class SubscriptionThread extends Thread {
        private Runnable runnable;

        public SubscriptionThread(final BandInfo band) {
            super();
            runnable = new Runnable() {
                @Override
                public void run() {
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


                                // Toast saying connection successful
                                toastStreaming(STREAM_TYPE);
                                // Dismiss notification if necessary
                                notifySuccess(band);
                                // Restart the timeout checker
                                if (timeoutThread.getState() != State.NEW
                                        && timeoutThread.getState() != State.RUNNABLE) {
                                    timeoutThread.makeThreadTerminate();
                                    timeoutThread = new TimeoutHandler();
                                    timeoutThread.start();
                                } else if (timeoutThread.getState() == State.NEW) {
                                    timeoutThread.start();
                                }
                            } else {
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                        "the band is in range.\n");

                                if (client != null) {
                                    client.disconnect(); }
//                                toastFailure();
                                notifyFailure(band);

                                reconnectBand();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream altimeter from this device ignored");
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
                        Log.e(TAG, "Unknown error occurred when getting altimeter data");
                        e.printStackTrace();
                    }

                }
            };
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    private class UnsubscribeThread extends Thread {
        private Runnable runnable;

        public UnsubscribeThread(final BandInfo info) {
            super();
            runnable = new Runnable() {
                @Override
                public void run() {

                    if (clients.containsKey(info)) {

                        BandClient client = clients.get(info);
                        Log.v(TAG, "Client found ");

                        // Unregister the client
                        try {
                            client.getSensorManager().unregisterAltimeterEventListener(
                                    (BandAltimeterEventListener) listeners.get(info)
                            );
                            mHandler.post(unsubscribedToastRunnable);

                            Log.v(TAG, "Unregistered listener");
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
        @Override
        public void run(){
            runnable.run();
        }
    }



    private class CustomBandAltimeterEventListener extends CustomListener
            implements BandAltimeterEventListener {

        public CustomBandAltimeterEventListener(BandInfo bandInfo, String name) {
            super();
            uName = name;
            info = bandInfo;
            location = BandDataService.locations.get(info);
        }

        private String uName;
        private String location;
        private final int BUFFER_SIZE = 100;
        private JSONArray dataBuffer = new JSONArray();

        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {

//                    fw.append(getDateTime(event));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getTotalGain()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getTotalLoss()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getSteppingGain()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getSteppingLoss()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getStepsAscended()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getStepsDescended()));
//                    fw.append(',');
//                    fw.append(Float.toString(event.getRate()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getFlightsAscended()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getFlightsDescended()));
//                    fw.append('\n');
                JSONObject datapoint = new JSONObject();
                try {
                    datapoint.put("Time", event.getTimestamp());
                    datapoint.put("Total_Gain", event.getTotalGain());
                    datapoint.put("Total_Loss", event.getTotalLoss());
                    datapoint.put("Stepping_Gain", event.getSteppingGain());
                    datapoint.put("Stepping_Loss", event.getSteppingLoss());
                    datapoint.put("Steps_Ascended", event.getStepsAscended());
                    datapoint.put("Steps_Descended", event.getStepsDescended());
                    datapoint.put("Rate", event.getRate());
                    datapoint.put("Flights_Ascended", event.getFlightsAscended());
                    datapoint.put("Flights_Descended", event.getFlightsDescended());

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
                                properties.put(info.getMacAddress() + "_" + STREAM_TYPE + "_"
                                        + getDateTime(event), dataBuffer.toString());
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
//                        fw.append("Time,TotalGain,TotalLoss," +
//                                "SteppingGain,SteppingLoss,StepsAscended,StepsDescended," +
//                                        "Rate,FlightsAscended,FlightsDescended");
//                    } else {
//                        fw = new FileWriter(csv, true);
//                    }
//
//                    fw.append(getDateTime(event));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getTotalGain()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getTotalLoss()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getSteppingGain()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getSteppingLoss()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getStepsAscended()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getStepsDescended()));
//                    fw.append(',');
//                    fw.append(Float.toString(event.getRate()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getFlightsAscended()));
//                    fw.append(',');
//                    fw.append(Long.toString(event.getFlightsDescended()));
//                    fw.append('\n');
//                    fw.close();
//                    Log.v(TAG, "Wrote to " + csv.getPath());
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
            }
        }

    }
}
