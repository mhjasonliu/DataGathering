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
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.northwestern.habits.datagathering.CouchBaseData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class AmbientManager extends DataManager {

    public AmbientManager(String sName, SQLiteOpenHelper dbHelper, Context context) {
        super(sName, "AmbientManager", dbHelper, context);
        STREAM_TYPE = "AMB";
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();
    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }


    /* ******************************** THREADS ********************************************* */

    private TimeoutHandler timeoutThread = new TimeoutHandler();

    private class SubscriptionThread extends Thread {
        Runnable runnable;

        public SubscriptionThread(final BandInfo bandInfo) {
            super();
            runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!clients.containsKey(bandInfo)) {
                            // No registered clients streaming ambient light data
                            BandClient client = connectBandClient(bandInfo, null);
                            if (client != null &&
                                    client.getConnectionState() == ConnectionState.CONNECTED) {
                                // Create the listener
                                CustomBandAmbientLightEventListener aListener =
                                        new CustomBandAmbientLightEventListener(bandInfo, studyName);

                                // Register the listener
                                client.getSensorManager().registerAmbientLightEventListener(
                                        aListener);

                                // Save the listener and client
                                listeners.put(bandInfo, aListener);
                                clients.put(bandInfo, client);


                                // Toast saying connection successful
                                toastStreaming(STREAM_TYPE);
                                // Dismiss notification if necessary
                                notifySuccess(bandInfo);
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
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on " +
                                        "and the band is in range.\n");

//                                toastFailure();
                                notifyFailure(bandInfo);
                                if (client != null) { client.disconnect(); }
                                reconnectBand();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream ambient sensor from this device ignored");
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
                        Log.e(TAG, "Unknown error occurred when getting ambient light data");
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
        Runnable runnable;

        public UnsubscribeThread(final BandInfo bandInfo) {
            super();
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (clients.containsKey(bandInfo)) {

                        BandClient client = clients.get(bandInfo);

                        // Unregister the client
                        try {
                            client.getSensorManager().unregisterAmbientLightEventListener(
                                    (BandAmbientLightEventListener) listeners.get(bandInfo)
                            );
                            mHandler.post(unsubscribedToastRunnable);

                            // Remove listener from list
                            listeners.remove(bandInfo);
                            // Remove client from list
                            clients.get(bandInfo).disconnect();
                            clients.remove(bandInfo);
                        } catch (BandIOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }

        @Override
        public void run() {
            runnable.run();
        }
    }


    /* **************************** EVENT LISTENER ******************************************** */

    private class CustomBandAmbientLightEventListener extends CustomListener
            implements BandAmbientLightEventListener {
        private BandInfo info;
        private String uName;
        private String location;
        private final int BUFFER_SIZE = 100;
        private JSONArray dataBuffer = new JSONArray();

        public CustomBandAmbientLightEventListener(BandInfo bInfo, String name) {
            super();
            info = bInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
            if (event != null) {
                JSONObject datapoint = new JSONObject();
                try {
                    datapoint.put("Time", event.getTimestamp());
                    datapoint.put("Ambient_Brightness", event.getBrightness());
                    datapoint.put("label", label);
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
