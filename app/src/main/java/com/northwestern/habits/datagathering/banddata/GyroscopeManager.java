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
import com.northwestern.habits.datagathering.CouchBaseData;
import com.northwestern.habits.datagathering.Preferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 12/31/2015
 */
public class GyroscopeManager extends DataManager {
    private Map<BandInfo, SampleRate> frequencies = new HashMap<>();
    private final long TIMEOUT_INTERVAL = 1000;
    private int restartCount = 1;

    protected void setFrequency(String f, BandInfo bandinfo) {
        switch (f) {
            case "8Hz":
                frequencies.put(bandinfo, SampleRate.MS128);
                break;
            case "31Hz":
                frequencies.put(bandinfo, SampleRate.MS32);
                break;
            case "62Hz":
                frequencies.put(bandinfo, SampleRate.MS16);
                break;
            default:
                frequencies.put(bandinfo, SampleRate.MS128);
        }

        // Record frequency change
        SharedPreferences prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(Preferences.getFrequencyKey(bandinfo.getMacAddress(), Preferences.GYRO), f).apply();
    }

    public GyroscopeManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "GyroscopeManager", db, context);
        STREAM_TYPE = "GYR";
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
        new UnsubscribeThread(info).run();
        new SubscriptionThread(info).run();
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
                                        new CustomBandGyroEventListener(info, studyName);

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


    /* **************************** LISTENER *********************************** */

    private class CustomBandGyroEventListener extends CustomListener
            implements BandGyroscopeEventListener {

        private BandInfo info;
        private final int BUFFER_SIZE = 100;
        private DataSeries dataBuffer = new DataSeries(STREAM_TYPE, BUFFER_SIZE);

        public CustomBandGyroEventListener(BandInfo mInfo, String name) {
            super();
            info = mInfo;
        }

        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {
                this.lastDataSample = System.currentTimeMillis();
                JSONObject datapoint = new JSONObject();
                try {
                    datapoint.put("Time", event.getTimestamp());
                    datapoint.put("Type", STREAM_TYPE);
                    datapoint.put("Label", label);
                    datapoint.put("Linear_Accel_x", event.getAccelerationX());
                    datapoint.put("Linear_Accel_y", event.getAccelerationY());
                    datapoint.put("Linear_Accel_z", event.getAccelerationZ());
                    datapoint.put("Angular_Velocity_x", event.getAngularVelocityX());
                    datapoint.put("Angular_Velocity_y", event.getAngularVelocityY());
                    datapoint.put("Angular_Velocity_z", event.getAngularVelocityZ());

                    dataBuffer.putDataPoint(datapoint, event.getTimestamp());
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (dataBuffer.isFull()) {
                    try {
                        CouchBaseData.getNewDocument(context).update(new Document.DocumentUpdater() {
                            @Override
                            public boolean update(UnsavedRevision newRevision) {
                                Map<String, Object> properties = newRevision.getUserProperties();
//                                properties.put(info.getMacAddress() + "_" + STREAM_TYPE
//                                        + "_" + getDateTime(event), dataBuffer.toString());
                                JSONObject o = dataBuffer.pack();
                                Log.v(TAG, o.toString());
                                properties.put("data_series", o);
                                newRevision.setUserProperties(properties);
                                return true;
                            }
                        });
                        dataBuffer = new DataSeries(STREAM_TYPE, BUFFER_SIZE);
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
