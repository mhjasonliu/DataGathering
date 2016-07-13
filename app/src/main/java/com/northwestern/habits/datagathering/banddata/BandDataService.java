package com.northwestern.habits.datagathering.banddata;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;
import com.northwestern.habits.datagathering.Preferences;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BandDataService extends Service {

    /* ****************** STRINGS FOR IDENTIFICATION OF EXTRAS ********************************* */
    public static final String ACCEL_REQ_EXTRA = "accelerometer";
    public static final String ALT_REQ_EXTRA = "altimeter";
    public static final String AMBIENT_REQ_EXTRA = "ambient";
    public static final String BAROMETER_REQ_EXTRA = "barometer";
    public static final String CALORIES_REQ_EXTRA = "calories";
    public static final String CONTACT_REQ_EXTRA = "contact";
    public static final String DISTANCE_REQ_EXTRA = "distance";
    public static final String GSR_REQ_EXTRA = "gsr";
    public static final String GYRO_REQ_EXTRA = "gyro";
    public static final String HEART_RATE_REQ_EXTRA = "heartRate";
    public static final String PEDOMETER_REQ_EXTRA = "pedometer";
    public static final String SKIN_TEMP_REQ_EXTRA = "skinTemperature";
    public static final String UV_REQ_EXTRA = "ultraViolet";

    public static final String INDEX_EXTRA = "index";
    public static final String STUDY_ID_EXTRA = "study";
    public static final String LOCATION_EXTRA = "location";
    public static final String FREQUENCY_EXTRA = "frequency";

    public static final String CONTINUE_STUDY_EXTRA = "continue study";
    public static final String STOP_STREAM_EXTRA = "stop stream";


    private static final String TAG = "Band Service";

    public static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    // General stuff (maintained by main)
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
    private HashMap<String, Boolean> modes = new HashMap<>();

    private HashMap<BandInfo, List<String>> bandStreams = new HashMap<>();
    protected static HashMap<BandInfo, String> locations = new HashMap<>();

    protected String studyName;

    private SQLiteOpenHelper dbHelper;

    private int NOTIFICATION_ID = 255;


    // Data managers
    AccelerometerManager accManager = new AccelerometerManager("", null, this);
    AltimeterManager altManager = new AltimeterManager("", null, this);
    AmbientManager ambManager = new AmbientManager("", null, this);
    BarometerManager barometerManager = new BarometerManager("", null, this);
    CaloriesManager calManager = new CaloriesManager("", null, this);
    ContactManager conManager = new ContactManager("", null, this);
    DistanceManager distManager = new DistanceManager("", null, this);
    GsrManager gsrManager = new GsrManager("", null, this);
    GyroscopeManager gyroManager = new GyroscopeManager("", null, this);
    HeartRateManager heartManager = new HeartRateManager("", null, this);
    PedometerManager pedoManager = new PedometerManager("", null, this);
    SkinTempManager skinTempManager = new SkinTempManager("", null, this);
    UvManager uvManager = new UvManager("", null, this);

    boolean isStarted = false;
    private boolean isReconnecting = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle("Data streaming");
        b.setContentText("The habits lab is streaming data from your Microsoft Band");
        b.setSmallIcon(android.R.drawable.arrow_up_float);

        startForeground(NOTIFICATION_ID, b.build());

        SharedPreferences prefs = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);

        // Check for registered bands
        Set<String> bandMacs = prefs.getStringSet(Preferences.REGISTERED_DEVICES, new HashSet<String>());

        // Lock thread while reconnecting
        if (!isReconnecting) {
            isReconnecting = true;
            for (String mac :
                    bandMacs) {
                // Look for streams for this device
                Set<String> streams = prefs.getStringSet(Preferences.getDeviceKey(mac), new HashSet<String>());
                BandInfo band = infoFromMac(mac);
                for (String stream :
                        streams) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    switch (stream) {
                        case Preferences.ACCEL:
                            // Check for a frequency entry
                            String f = prefs.getString(Preferences.getFrequencyKey(mac, stream), "");
                            accManager.setFrequency(f, band);
                            genericSubscriptionFactory(ACCEL_REQ_EXTRA, band);
                            break;
                        case Preferences.ALT:
                            genericSubscriptionFactory(ALT_REQ_EXTRA, band);
                            break;
                        case Preferences.AMBIENT:
                            genericSubscriptionFactory(AMBIENT_REQ_EXTRA, band);
                            break;
                        case Preferences.BAROMETER:
                            genericSubscriptionFactory(BAROMETER_REQ_EXTRA, band);
                            break;
                        case Preferences.CALORIES:
                            genericSubscriptionFactory(CALORIES_REQ_EXTRA, band);
                            break;
                        case Preferences.CONTACT:
                            genericSubscriptionFactory(CONTACT_REQ_EXTRA, band);
                            break;
                        case Preferences.DISTANCE:
                            genericSubscriptionFactory(DISTANCE_REQ_EXTRA, band);
                            break;
                        case Preferences.GSR:
                            genericSubscriptionFactory(GSR_REQ_EXTRA, band);
                            break;
                        case Preferences.GYRO:
                            // Check for a frequency entry
                            String fr = prefs.getString(Preferences.getFrequencyKey(mac, stream), "");
                            Log.v(TAG, "Frequency is " + fr);
                            accManager.setFrequency(fr, band);
                            genericSubscriptionFactory(GYRO_REQ_EXTRA, band);
                            break;
                        case Preferences.HEART:
                            genericSubscriptionFactory(HEART_RATE_REQ_EXTRA, band);
                            break;
                        case Preferences.PEDOMETER:
                            genericSubscriptionFactory(PEDOMETER_REQ_EXTRA, band);
                            break;
                        case Preferences.SKIN_TEMP:
                            genericSubscriptionFactory(SKIN_TEMP_REQ_EXTRA, band);
                            break;
                        case Preferences.UV:
                            genericSubscriptionFactory(UV_REQ_EXTRA, band);
                            break;
                        default:
                    }
                }
            }
            isReconnecting = false;
        }

        isStarted = true;
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        for (BandInfo info :
                bandStreams.keySet()) {
            // For every band that is streaming now
            for (String stream:
                 bandStreams.get(info)) {
                genericUnsubscribeFactory(stream, info);
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void genericUnsubscribeFactory(String request, BandInfo band) {
        Log.v(TAG, "Stopping stream " + request);
        // Check for existance of stream
        if (bandStreams.containsKey(band) && bandStreams.get(band).contains(request)) {
            if (bandStreams.get(band).size() == 1) {
                // Only stream open for this band, remove from bandStreams
                bandStreams.remove(band);
            } else {
                // Other streams open, remove from list
                bandStreams.get(band).remove(request);
            }

            // Unsubscribe from the appropriate stream
            switch (request) {
                case ACCEL_REQ_EXTRA:
                    if (accManager != null)
                        accManager.unSubscribe(band);
                    break;
                case ALT_REQ_EXTRA:
                    if (altManager != null)
                        altManager.unSubscribe(band);
                    break;
                case AMBIENT_REQ_EXTRA:
                    if (ambManager != null)
                        ambManager.unSubscribe(band);
                    break;
                case BAROMETER_REQ_EXTRA:
                    if (barometerManager != null)
                        barometerManager.unSubscribe(band);
                    break;
                case CALORIES_REQ_EXTRA:
                    if (calManager != null)
                        calManager.unSubscribe(band);
                    break;
                case CONTACT_REQ_EXTRA:
                    if (conManager != null)
                        conManager.unSubscribe(band);
                    break;
                case DISTANCE_REQ_EXTRA:
                    if (distManager != null)
                        distManager.unSubscribe(band);
                    break;
                case GSR_REQ_EXTRA:
                    if (gsrManager != null)
                        gsrManager.unSubscribe(band);
                    break;
                case GYRO_REQ_EXTRA:
                    if (gyroManager != null)
                        gyroManager.unSubscribe(band);
                    break;
                case HEART_RATE_REQ_EXTRA:
                    if (heartManager != null)
                        heartManager.unSubscribe(band);
                    break;
                case PEDOMETER_REQ_EXTRA:
                    if (pedoManager != null)
                        pedoManager.unSubscribe(band);
                    break;
                case SKIN_TEMP_REQ_EXTRA:
                    if (skinTempManager != null)
                        skinTempManager.unSubscribe(band);
                    break;
                case UV_REQ_EXTRA:
                    if (uvManager != null)
                        uvManager.unSubscribe(band);
                    break;
                default:
                    Log.e(TAG, "Unknown subscription requested " + request);
            }
        } else {
            if (!bandStreams.containsKey(band)) {
                Log.e(TAG, "Error: unsubscribe request for a band that isnt stored");
                Log.v(TAG, "Band: " + band.toString());
                for (BandInfo info :
                        bandStreams.keySet()) {
                    Log.v(TAG, "Key: " + info.toString());
                }
            } else {
                if (!bandStreams.get(band).contains(request)) {
                    Log.e(TAG, "Error: unsubscribe request for unregistered request");
                }
            }
        }
    }


    private void genericSubscriptionFactory(String request, BandInfo band) {
        Log.v(TAG, request + " requested");
        if (!bandStreams.containsKey(band)) {
            // Make a new list to put into the map with the band
            List<String> list = new LinkedList<>();
            list.add(request);

            // Add the band to the map
            bandStreams.put(band, list);

        } else if (!bandStreams.get(band).contains(request)) {
            // Add sensor to the list in the stream map
            bandStreams.get(band).add(request);
        }

        // Request the appropriate stream
        switch (request) {
            case ACCEL_REQ_EXTRA:
                if (accManager == null)
                    accManager = new AccelerometerManager(studyName, dbHelper, this);

                accManager.subscribe(band);
                break;
            case ALT_REQ_EXTRA:
                if (altManager == null)
                    altManager = new AltimeterManager(studyName, dbHelper, this);

                altManager.subscribe(band);
                break;
            case AMBIENT_REQ_EXTRA:
                if (ambManager == null)
                    ambManager = new AmbientManager(studyName, dbHelper, this);

                ambManager.subscribe(band);
                break;
            case BAROMETER_REQ_EXTRA:
                if (barometerManager == null)
                    barometerManager = new BarometerManager(studyName, dbHelper, this);

                barometerManager.subscribe(band);
                break;
            case CALORIES_REQ_EXTRA:
                if (calManager == null)
                    calManager = new CaloriesManager(studyName, dbHelper, this);

                calManager.subscribe(band);
                break;
            case CONTACT_REQ_EXTRA:
                if (conManager == null)
                    conManager = new ContactManager(studyName, dbHelper, this);

                conManager.subscribe(band);
                break;
            case DISTANCE_REQ_EXTRA:
                if (distManager == null)
                    distManager = new DistanceManager(studyName, dbHelper, this);

                distManager.subscribe(band);
                break;
            case GSR_REQ_EXTRA:
                if (gsrManager == null)
                    gsrManager = new GsrManager(studyName, dbHelper, this);

                gsrManager.subscribe(band);
                break;
            case GYRO_REQ_EXTRA:
                if (gyroManager == null)
                    gyroManager = new GyroscopeManager(studyName, dbHelper, this);

                gyroManager.subscribe(band);
                break;
            case HEART_RATE_REQ_EXTRA:
                if (heartManager == null)
                    heartManager = new HeartRateManager(studyName, dbHelper, this);

                heartManager.subscribe(band);
                break;
            case PEDOMETER_REQ_EXTRA:
                if (pedoManager == null)
                    pedoManager = new PedometerManager(studyName, dbHelper, this);

                pedoManager.subscribe(band);
                break;
            case SKIN_TEMP_REQ_EXTRA:
                if (skinTempManager == null)
                    skinTempManager = new SkinTempManager(studyName, dbHelper, this);

                skinTempManager.subscribe(band);
                break;
            case UV_REQ_EXTRA:
                if (uvManager == null)
                    uvManager = new UvManager(studyName, dbHelper, this);

                uvManager.subscribe(band);
                break;
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
        }
    }

    public class StopAllStreams extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            for (BandInfo band : bandStreams.keySet()) {
                for (String type : bandStreams.get(band)) {
                    genericUnsubscribeFactory(type, band);
                }
            }
            return null;
        }
    }


    /* ******************************** IPC STUFF **************************************** */
    /*
    Return our Messenger interface for sending messages to
    the service by the clients.
    */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private Messenger mMessenger = new Messenger(new IncomingHandler(this));
    public static final int MSG_STREAM = 1;
    public static final int MSG_FREQUENCY = 2;

    public static final String MAC_EXTRA = "mac";
    public static final String REQUEST_EXTRA = "request";

    // Incoming messages Handler
    private static class IncomingHandler extends Handler {

        private final WeakReference<BandDataService> mService;

        IncomingHandler(BandDataService s) {
            mService = new WeakReference<>(s);
        }

        @Override
        public void handleMessage(Message msg) {
            BandDataService service = mService.get();
            Bundle bundle = msg.getData();
            String mac = bundle.getString(MAC_EXTRA);
            BandInfo bandInfo = service.infoFromMac(mac);
            String request = bundle.getString(REQUEST_EXTRA);
            switch (msg.what) {
                case MSG_STREAM:
                    boolean stopStream = bundle.getBoolean(STOP_STREAM_EXTRA);

                    if (mac != (null) && request != null) {
                        if (stopStream) {
                            service.genericUnsubscribeFactory(request, bandInfo);
                        } else {
                            service.genericSubscriptionFactory(request, bandInfo);
                        }
                    }
                    break;
                case MSG_FREQUENCY:
                    String frequency = bundle.getString(FREQUENCY_EXTRA);
                    if (request != null) {
                        switch (request) {
                            case ACCEL_REQ_EXTRA:
                                service.setAccelFrequency(frequency, bandInfo);
                                break;
                            case GYRO_REQ_EXTRA:
                                service.setGyroFrequency(frequency, bandInfo);
                                break;
                            default:
                                Log.e(TAG, "Frequency request sent for an unsupported type");
                        }
                    } else {
                        Log.w(TAG, "Request in frequency message was null.");
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    private void setAccelFrequency(String f, BandInfo bandInfo) {
        // Change the manager's stored frequency
        accManager.setFrequency(f, bandInfo);

        // Unsubscribe and resubscribe if necessary
        if (bandStreams.containsKey(bandInfo) && bandStreams.get(bandInfo).contains(ACCEL_REQ_EXTRA)) {
            accManager.unSubscribe(bandInfo);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            accManager.subscribe(bandInfo);
        }

    }

    private void setGyroFrequency(String f, BandInfo bandInfo) {
        // Change the manager's stored frequency
        gyroManager.setFrequency(f, bandInfo);

        // Unsubscribe and resubscribe if necessary
        if (bandStreams.containsKey(bandInfo) && bandStreams.get(bandInfo).contains(GYRO_REQ_EXTRA)) {
            gyroManager.unSubscribe(bandInfo);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gyroManager.subscribe(bandInfo);
        }
    }

    private BandInfo infoFromMac(String mac) {
        // New band, get new info
        BandInfo[] bands = pairedBands;//BandClientManager.getInstance().getPairedBands();
        for (BandInfo b :
                bands) {
            if (b.getMacAddress().equals(mac)) {
                return b;
            }
        }

        // Failed to find the bandInfo matching the MAC address, create one and store
        Log.e(TAG, "Failed to find the MAC address");
        return null;
    }

}
