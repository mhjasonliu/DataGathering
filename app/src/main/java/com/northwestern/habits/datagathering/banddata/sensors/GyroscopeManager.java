package com.northwestern.habits.datagathering.banddata.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.DataSeries;
import com.northwestern.habits.datagathering.userinterface.UserActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class GyroscopeManager extends DataManager {
    private Map<BandInfo, SampleRate> frequencies = new HashMap<>();

    protected void setFrequency(String f, BandInfo bandinfo) {
        boolean rateChanged = false;
        Log.v(TAG, "Setting frequency to " + f);

        switch (f) {
            case "8Hz":
                rateChanged = frequencies.get(bandinfo) != SampleRate.MS128;
                frequencies.put(bandinfo, SampleRate.MS128);
                break;
            case "31Hz":
                rateChanged = frequencies.get(bandinfo) != SampleRate.MS32;
                frequencies.put(bandinfo, SampleRate.MS32);
                break;
            case "62Hz":
                rateChanged = frequencies.get(bandinfo) != SampleRate.MS16;
                frequencies.put(bandinfo, SampleRate.MS16);
                break;
            default:
                frequencies.put(bandinfo, SampleRate.MS128);
        }

        if (clients.containsKey(bandinfo) && rateChanged) {
            Log.v(TAG, "Resubscribing for frequency change");
            restartSubscription(bandinfo);
        }

        // Record frequency change
        if (bandinfo != null) {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor e = prefs.edit();
                e.putString(Preferences.getFrequencyKey(bandinfo.getMacAddress(), Preferences.GYRO), f).apply();
            } catch (NullPointerException e1) {
                e1.printStackTrace();
            }
        }
    }

    public GyroscopeManager(Context context) {
        super("GyroscopeManager", context, 100);
        STREAM_TYPE = "GYR";
        dataBuffer = new DataSeries(DataManagementService.T_Gyroscope, BUFFER_SIZE);
    }

    @Override
    protected void subscribe(BandInfo info) {
        new SubscriptionThread(info).start();

    }

    @Override
    protected void unSubscribe(BandInfo info) {
        new UnsubscribeThread(info).start();
    }

    protected void restartSubscription(BandInfo info) {
        new RestartSubscriptionThread(info).start();
    }

    /* *********************************** THREADS *************************************** */

    TimeoutHandler timeoutThread = new TimeoutHandler();

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
                                        new CustomBandGyroEventListener(info, userID);

                                // Get the sample rate
                                SampleRate rate = frequencies.get(info);
                                if (rate == null) {
                                    Log.e(TAG, "Default rate used");
                                    rate = SampleRate.MS128;
                                }

                                if (!listeners.containsKey(info)) {
                                    // Register the listener
                                    Log.v(TAG, "Subscribing to gyro");
                                    Log.v(TAG, String.valueOf(listeners));
                                    client.getSensorManager().registerGyroscopeEventListener(
                                            aListener, rate);

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
                                    Log.w(TAG, "Warning: subscribe called for already subscribed " +
                                            "sensor. Call unsubscribe first.");
                                }
                            } else {
                                Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                        "the band is in range.\n");
                                // Close the client
                                if (client != null) {
                                    client.disconnect();
                                }

//                                toastFailure();
                                notifyFailure(info);

                                reconnectBand();
                            }
                        } else {
                            Log.w(TAG, "Multiple attempts to stream Gyro sensor from this device ignored");
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
                        Log.e(TAG, "Unknown error occurred when getting Gyro data");
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Unknown error connecting to "
                                        + STREAM_TYPE, Toast.LENGTH_LONG).show();
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
                            Log.v(TAG, "unSubscribing to gyro");
                            client.getSensorManager().unregisterGyroscopeEventListener(
                                    (BandGyroscopeEventListener) listeners.get(info)
                            );

                            mHandler.post(unsubscribedToastRunnable);
                            // Remove listener from list
                            listeners.remove(info);
                            // Remove client from list and disconnect from the band
                            clients.get(info).disconnect();
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

    private class RestartSubscriptionThread extends Thread {
        private Runnable r;

        public RestartSubscriptionThread(final BandInfo info) {
            super();

            r = new Runnable() {
                @Override
                public void run() {
                    new UnsubscribeThread(info).run();
                    new SubscriptionThread(info).run();
                }
            };
        }

        @Override
        public void run() {
            r.run();
        }
    }


    /* **************************** LISTENER *********************************** */

    private class CustomBandGyroEventListener extends CustomListener
            implements BandGyroscopeEventListener {

        public CustomBandGyroEventListener(BandInfo mInfo, String name) {
            super();
            info = mInfo;
        }

        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {
                this.lastDataSample = System.currentTimeMillis();
                Map<String, Object> datapoint = new HashMap<>();
                datapoint.put("Time", Long.toString(event.getTimestamp()));
                datapoint.put("Linear_Accel_x", event.getAccelerationX());
                datapoint.put("Linear_Accel_y", event.getAccelerationY());
                datapoint.put("Linear_Accel_z", event.getAccelerationZ());
                datapoint.put("Angular_Velocity_x", event.getAngularVelocityX());
                datapoint.put("Angular_Velocity_y", event.getAngularVelocityY());
                datapoint.put("Angular_Velocity_z", event.getAngularVelocityZ());

                dataBuffer.putDataPoint(datapoint, event.getTimestamp());
                if (dataBuffer.isFull()) {
                    // Send update to the UI about the data frequency
                    Intent i = new Intent(UserActivity.DataUpdateReceiver.FREQUENCY_UPDATE);
                    float numPoints = dataBuffer.getCount();
                    float time = event.getTimestamp() - dataBuffer.getFirstEntry();

                    // Number of points / time(in seconds)
                    float frequency = numPoints / (time / 1000);
                    i.putExtra(UserActivity.DataUpdateReceiver.ACTUAL_EXTRA, frequency);

                    String setting = Preferences.sampleRateToString(frequencies.get(info));
                    i.putExtra(UserActivity.DataUpdateReceiver.SETTING_EXTRA, setting);

                    context.sendBroadcast(i);
                    writeData(context, info, DataManagementService.T_Gyroscope);
                }
            }
        }
    }
}
