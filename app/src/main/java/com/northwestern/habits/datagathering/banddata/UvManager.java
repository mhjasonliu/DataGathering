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
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.northwestern.habits.datagathering.database.CouchBaseData;
import com.northwestern.habits.datagathering.database.DataManagementService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class UvManager extends DataManager {

    public UvManager(Context context) {
        super("UvManager", context, 100);
        STREAM_TYPE = "UV";
        dataBuffer = new DataSeries(DataManagementService.T_UV, BUFFER_SIZE);
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
                                        new CustomBandUvEventListener(info, userID);

                                // Register the listener
                                client.getSensorManager().registerUVEventListener(
                                        aListener);

                                // Save the listener and client
                                listeners.put(info, aListener);
                                clients.put(info, client);

                                // Toast saying connection successful
                                toastStreaming(STREAM_TYPE);
                                // Dismiss notification if necessary
                                notifySuccess(info);
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
                                notifyFailure(info);
                                if (client != null) {
                                    client.disconnect();
                                }
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

        public CustomBandUvEventListener(BandInfo bandInfo, String name) {
            super();
            info = bandInfo;
            uName = name;
            location = BandDataService.locations.get(info);
        }

        @Override
        public void onBandUVChanged(final BandUVEvent event) {
            if (event != null) {
                this.lastDataSample = System.currentTimeMillis();
                Map<String, Object> datapoint = new HashMap<>();
                datapoint.put("Time", Long.toString(event.getTimestamp()));
                datapoint.put("Label", label);
                datapoint.put("Level", event.getUVIndexLevel());
                try {
                    datapoint.put("UV_Exposure_Today", event.getUVExposureToday());
                } catch (InvalidBandVersionException e) {
                    e.printStackTrace();
                }

                dataBuffer.putDataPoint(datapoint, event.getTimestamp());


                if (dataBuffer.isFull()) {
                    try {
                        CouchBaseData.getNewDocument(context).update(new Document.DocumentUpdater() {
                            @Override
                            public boolean update(UnsavedRevision newRevision) {
                                Map<String, Object> properties = newRevision.getUserProperties();
                                properties.putAll(dataBuffer.pack());
                                properties.put(DataManagementService.DEVICE_MAC, info.getMacAddress());
                                properties.put(DataManagementService.T_DEVICE, T_BAND2);
                                properties.put(DataManagementService.USER_ID, userID);

                                newRevision.setUserProperties(properties);
                                return true;
                            }
                        });
                        dataBuffer = new DataSeries(DataManagementService.T_UV, BUFFER_SIZE);
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
