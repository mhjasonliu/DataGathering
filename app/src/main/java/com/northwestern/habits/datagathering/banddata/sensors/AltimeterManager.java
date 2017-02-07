package com.northwestern.habits.datagathering.banddata.sensors;

import android.content.Context;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.DataSeries;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class AltimeterManager extends DataManager {

    private final String TAG = "AltimeterManager";
    private TimeoutHandler timeoutThread = new TimeoutHandler();

    public AltimeterManager(Context context) {
        super("AltimeterManager",context, 100);
        STREAM_TYPE = "Altimeter";
        dataBuffer = new DataSeries(DataManagementService.T_Altimeter, BUFFER_SIZE);
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
                                        new CustomBandAltimeterEventListener(band, userID);

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

        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {

            if (event != null) {
                this.lastDataSample = System.currentTimeMillis();
                Map<String, Object> datapoint = new HashMap<>();
                datapoint.put("Time", Long.toString(event.getTimestamp()));
                datapoint.put("Total_Gain", event.getTotalGain());
                datapoint.put("Total_Loss", event.getTotalLoss());
                datapoint.put("Stepping_Gain", event.getSteppingGain());
                datapoint.put("Stepping_Loss", event.getSteppingLoss());
                datapoint.put("Steps_Ascended", event.getStepsAscended());
                datapoint.put("Steps_Descended", event.getStepsDescended());
                datapoint.put("Rate", event.getRate());
                datapoint.put("Flights_Ascended", event.getFlightsAscended());
                datapoint.put("Flights_Descended", event.getFlightsDescended());

                dataBuffer.putDataPoint(datapoint, event.getTimestamp());

                if (dataBuffer.isFull())
                    writeData(context, info, DataManagementService.T_Altimeter);
            }

        }
    }
}
