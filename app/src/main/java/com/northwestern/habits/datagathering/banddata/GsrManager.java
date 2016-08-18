package com.northwestern.habits.datagathering.banddata;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.northwestern.habits.datagathering.database.CouchBaseData;
import com.northwestern.habits.datagathering.database.DataManagementService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class GsrManager extends DataManager {
    public GsrManager(Context context) {
        super("GsrManager", context, 100);
        STREAM_TYPE = "GSR";
        dataBuffer = new DataSeries(DataManagementService.T_GSR, BUFFER_SIZE);
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
                                        new CustomBandGsrEventListener(bandInfo, userID);

                                // Register the listener
                                client.getSensorManager().registerGsrEventListener(
                                        aListener, GsrSampleRate.MS200);

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
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                        "the band is in range.\n");

//                                toastFailure();
                                notifyFailure(bandInfo);
                                if (client != null) { client.disconnect(); }
                                reconnectBand();
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
                            client.disconnect();
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
                this.lastDataSample = System.currentTimeMillis();
                Map<String, Object> datapoint = new HashMap<>();
                datapoint.put("Time", Long.toString(event.getTimestamp()));
                datapoint.put("Label", label);
                datapoint.put("Resistance", event.getResistance());

                dataBuffer.putDataPoint(datapoint, event.getTimestamp());


                if (dataBuffer.isFull()) {
                    final DataSeries myBuffer = dataBuffer;
                    dataBuffer = new DataSeries(DataManagementService.T_Gyroscope, BUFFER_SIZE);

                    try {
                        CouchBaseData.getNewDocument(context).update(new Document.DocumentUpdater() {
                            @Override
                            public boolean update(UnsavedRevision newRevision) {
                                Map<String, Object> properties = newRevision.getUserProperties();
                                properties.putAll(myBuffer.pack());
                                properties.put(DataManagementService.DEVICE_MAC, info.getMacAddress());
                                properties.put(DataManagementService.T_DEVICE, T_BAND2);
                                properties.put(DataManagementService.USER_ID, userID);

                                newRevision.setUserProperties(properties);
                                return true;
                            }
                        });

                        // Write to csv
                        myBuffer.exportCSV(context, userID, T_BAND2);
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
