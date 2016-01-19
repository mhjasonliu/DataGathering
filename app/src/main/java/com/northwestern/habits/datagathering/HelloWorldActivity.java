package com.northwestern.habits.datagathering;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HelloWorldActivity extends AppCompatActivity implements HeartRateConsentListener {

    public static final String BAND_MAC_EXTRA = "band info";
    private String TAG = "HelloWorld";
    private BandInfo band;
    public HelloWorldActivity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_world);

        qualityView = (TextView) findViewById(R.id.hrStatus);
        rateView = (TextView) findViewById(R.id.hrRate);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String mac = extras.getString(BAND_MAC_EXTRA);

            // Check for heart rate consent
            band = BluetoothConnectionLayer.bandMap.get(mac);
            com.microsoft.band.sensors.BandSensorManager manager =
                    BandClientManager.getInstance().create(this,
                            band).getSensorManager();

            if (manager.getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                // Request heart rate consent
                Log.v(TAG, "Requesting heart rate consent");
                // Get permission
                manager.requestHeartRateConsent(this, this);
            } else {
                userAccepted(true);
            }
        }
    }

    @Override
    public void userAccepted(boolean b) {

        if (b) {
            Log.v(TAG, "User accepted heart rate request");
            // Tick the heart rate box

            // Begin streaming
            new SubscriptionTask().execute(band);

/*
            // Setup because hack
            HashMap<BandInfo, BandClient> clients = new HashMap<>();

            try {
                if (!clients.containsKey(band)) {
                    // No registered clients streaming heart rate data
                    BandClient client = connectBandClient(band, null);
                    if (client != null &&
                            client.getConnectionState() == ConnectionState.CONNECTED) {
                        // Create the listener
                        HelloHRListener aListener =
                                new HelloHRListener();

                        aListener.hwActivity = this;

                        // Register the listener
                        client.getSensorManager().registerHeartRateEventListener(
                                aListener);

                        // Save the listener and client
                        clients.put(band, client);
                    } else {
                        Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                "the band is in range.\n");
                    }
                } else {
                    Log.w(TAG, "Multiple attempts to stream heart rate sensor from this device ignored");
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
                Log.e(TAG, "Unknown error occurred when getting heart rate data");
                e.printStackTrace();
            }
*/
        } else {
            Log.v(TAG, "User has rejected heart rate request");
            // Untick the heart rate box
        }

    }

    private class HelloHRListener implements BandHeartRateEventListener {

        public HelloWorldActivity hwActivity;

        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent heartEvent) {
            // Update quality
            /*
            ((TextView) hwActivity.findViewById(R.id.hrStatus)).setText(heartEvent.getQuality().toString());
            Log.v(TAG, "ASDFL;KJADSFLKJ");
            */
            hwActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String quality = heartEvent.getQuality().toString();
                    ((TextView) hwActivity.findViewById(R.id.hrStatus)).setText(quality);

                    if (quality == "LOCKED") {
                        ((TextView) hwActivity.findViewById(R.id.hrRate)).setText(
                                Integer.toString(heartEvent.getHeartRate())
                        );

                        // Update background color to green
                        hwActivity.findViewById(R.id.background).setBackgroundColor(
                                Color.argb(155, 0, 255, 0)
                        );

                        // Make main text say world
                        ((TextView) hwActivity.findViewById(R.id.mainText)).setText("world!");

                    } else {
                        ((TextView) hwActivity.findViewById(R.id.hrRate)).setText("");

                        // Update background color to red
                        hwActivity.findViewById(R.id.background).setBackgroundColor(
                                Color.argb(155, 255, 0, 0));

                        // Make main text say hello
                        ((TextView) hwActivity.findViewById(R.id.mainText)).setText("Hello...");
                    }
                }
            });

            // If the quality is locked, post to server
            if (heartEvent.getQuality().toString() == "LOCKED") {

                Log.v(TAG, "SEnding post.....");
                try {
                    URL url = new URL("http://murphy.wot.eecs.northwestern.edu/~wgs068/postSQL.py");

                    HashMap<String, String> params = new HashMap<>();
                    params.put("Rate", Integer.toString(heartEvent.getHeartRate()));
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(heartEvent.getTimestamp());
                    params.put("Year", Integer.toString(cal.get(Calendar.YEAR)));
                    params.put("Month", Integer.toString(cal.get(Calendar.MONTH)));
                    params.put("Day", Integer.toString(cal.get(Calendar.DATE)));
                    params.put("Hour", Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
                    params.put("Minute", Integer.toString(cal.get(Calendar.MINUTE)));
                    params.put("Second", Integer.toString(cal.get(Calendar.SECOND)));

                    //Log.v(TAG, "Put params");

                    Set set = params.entrySet();
                    Iterator i = set.iterator();
                    StringBuilder postData = new StringBuilder();
                    for (Map.Entry<String, String> param : params.entrySet()) {
                        if (postData.length() != 0) {
                            postData.append('&');
                        }
                        postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                        postData.append('=');
                        postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                    }

                    byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                    //Log.v(TAG, "Encoded post data");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(postDataBytes);

                    //Log.v(TAG, "Posted data");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    //Log.v(TAG, "Reading response");
                    for (String line = null; (line = reader.readLine()) != null;) {
                        builder.append(line).append("\n");
                    }

                    reader.close();
                    conn.disconnect();
                    //Log.v(TAG, "Disconnected");
                    Log.v(TAG, "Response: " + builder.toString());

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static TextView qualityView;
    public static TextView rateView;

    private class SubscriptionTask extends AsyncTask<BandInfo, Void, Void> {
        @Override
        protected Void doInBackground(BandInfo... params) {

            // Setup because hack
            HashMap<BandInfo, BandClient> clients = new HashMap<>();

            if (params.length > 0) {
                BandInfo band = params[0];
                try {
                    if (!clients.containsKey(band)) {
                        // No registered clients streaming heart rate data
                        BandClient client = connectBandClient(band, null);
                        if (client != null &&
                                client.getConnectionState() == ConnectionState.CONNECTED) {
                            // Create the listener
                            HelloHRListener aListener =
                                    new HelloHRListener();

                            aListener.hwActivity = HelloWorldActivity.this;
                            // Register the listener
                            client.getSensorManager().registerHeartRateEventListener(
                                    aListener);

                            // Save the listener and client
                            clients.put(band, client);
                        } else {
                            Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and " +
                                    "the band is in range.\n");
                        }
                    } else {
                        Log.w(TAG, "Multiple attempts to stream heart rate sensor from this device ignored");
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
                    Log.e(TAG, "Unknown error occurred when getting heart rate data");
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private BandClient connectBandClient(BandInfo band, BandClient client) throws InterruptedException, BandException {
        if (client == null) {
            client = BandClientManager.getInstance().create(this, band);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return client;
        }

        if (ConnectionState.CONNECTED == client.connect().await()) {
            return client;
        } else {
            return null;
        }
    }

}
