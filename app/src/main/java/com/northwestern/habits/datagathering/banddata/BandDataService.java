package com.northwestern.habits.datagathering.banddata;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;
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

    public static final String FREQUENCY_EXTRA = "frequency";

    public static final String STOP_STREAM_EXTRA = "stop stream";


    private static final String TAG = "Band Service";

    public static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    // General stuff (maintained by main)
    private BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();

    private HashMap<BandInfo, List<String>> bandStreams = new HashMap<>();
    protected static HashMap<BandInfo, String> locations = new HashMap<>();

    protected String studyName;

    @SuppressWarnings("FieldCanBeLocal")
    private int NOTIFICATION_ID = 255;

    private BroadcastReceiver userIDReceiver;
    public static final String ACTION_USER_ID = "User_ID";
    public static final String USER_ID_EXTRA = "User_ID_Extra";


    // Data managers
    AccelerometerManager accManager = new AccelerometerManager(getBaseContext());
    AltimeterManager altManager = new AltimeterManager(getBaseContext());
    AmbientManager ambManager = new AmbientManager(getBaseContext());
    BarometerManager barometerManager = new BarometerManager(getBaseContext());
    CaloriesManager calManager = new CaloriesManager(getBaseContext());
    ContactManager conManager = new ContactManager(getBaseContext());
    DistanceManager distManager = new DistanceManager(getBaseContext());
    GsrManager gsrManager = new GsrManager(getBaseContext());
    GyroscopeManager gyroManager = new GyroscopeManager(getBaseContext());
    HeartRateManager heartManager = new HeartRateManager(getBaseContext());
    PedometerManager pedoManager = new PedometerManager(getBaseContext());
    SkinTempManager skinTempManager = new SkinTempManager(getBaseContext());
    UvManager uvManager = new UvManager(getBaseContext());

    boolean isStarted = false;
    private boolean isReconnecting = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "OnStartCommand called");
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle("Data streaming");
        b.setContentText("The habits lab is streaming data from your Microsoft Band");
        b.setSmallIcon(android.R.drawable.arrow_up_float);

        startForeground(NOTIFICATION_ID, b.build());

        // Set receivers
        if (userIDReceiver == null) {
            userIDReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DataManager.userID = intent.getStringExtra(USER_ID_EXTRA);
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                            .edit().putString(Preferences.USER_ID, DataManager.userID).apply();
                }
            };
            getBaseContext().registerReceiver(userIDReceiver, new IntentFilter(ACTION_USER_ID));
        }
        String id = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(Preferences.USER_ID, "");
        if (!id.equals("")) {
            DataManager.userID = id;
        }

        if (labelReceiver == null) {
            labelReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DataManager.label = intent.getIntExtra(LABEL_EXTRA, 0);
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                            .edit().putInt(Preferences.LABEL, DataManager.label).apply();
                }
            };
            getBaseContext().registerReceiver(labelReceiver, new IntentFilter(ACTION_LABEL));
        }
        DataManager.label = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(Preferences.LABEL, 0);

        if (tileEventReceiver == null) {
            tileEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    TileButtonEvent e = (TileButtonEvent) intent.getExtras().get(TileEvent.TILE_EVENT_DATA);
                    new HandleBroadcastTask(getBaseContext(), e).execute();
                }
            };
            registerReceiver(tileEventReceiver, new IntentFilter(TileEvent.ACTION_TILE_BUTTON_PRESSED));
        }


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Update the managers' contexts
        accManager.context = getApplicationContext();
        altManager.context = getApplicationContext();
        ambManager.context = getApplicationContext();
        barometerManager.context = getApplicationContext();
        calManager.context = getApplicationContext();
        conManager.context = getApplicationContext();
        distManager.context = getApplicationContext();
        gsrManager.context = getApplicationContext();
        gyroManager.context = getApplicationContext();
        heartManager.context = getApplicationContext();
        pedoManager.context = getApplicationContext();
        skinTempManager.context = getApplicationContext();
        uvManager.context = getApplicationContext();

        // Check for registered bands
        Set<String> bandMacs = prefs.getStringSet(Preferences.REGISTERED_DEVICES, new HashSet<String>());

        // Lock thread while reconnecting
        if (!isReconnecting) {
            Log.v(TAG, "Reconnecting streams");
            isReconnecting = true;
            Log.v(TAG, "Bandmacs: " + bandMacs);
            for (String mac :
                    bandMacs) {
                // Look for streams for this device
                Set<String> streams = prefs.getStringSet(Preferences.getDeviceKey(mac), new HashSet<String>());
                Log.v(TAG, "MAC: " + mac);
                Log.v(TAG, "Streams: " +streams);
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
                            gyroManager.setFrequency(fr, band);
                            genericSubscriptionFactory(GYRO_REQ_EXTRA, band);
                            break;
                        case Preferences.HEART:
                            genericSubscriptionFactory(HEART_RATE_REQ_EXTRA, band);
                            break;
                        case Preferences.PEDOMETER:
                            genericSubscriptionFactory(PEDOMETER_REQ_EXTRA, band);
                            break;
                        case Preferences.SKIN_TEMP:
                            Log.v(TAG, "Subing to skin temp " + band.getMacAddress());
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
            for (String stream :
                    bandStreams.get(info)) {
                genericUnsubscribeFactory(stream, info);
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Unregister receivers
        getBaseContext().unregisterReceiver(userIDReceiver);
        userIDReceiver = null;

        super.onDestroy();
    }

    private void genericUnsubscribeFactory(String request, BandInfo band) {
        // Check for existance of stream
        if (bandStreams.containsKey(band) && bandStreams.get(band).contains(request)) {
            if (bandStreams.get(band).size() == 1) {
                // Only stream open for this band, remove from bandStreams
                bandStreams.remove(band);
            } else {
                // Other streams open, remove from list
                bandStreams.get(band).remove(request);
            }

            // Remove stream from preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            Set<String> streams = prefs.getStringSet(Preferences.getDeviceKey(band.getMacAddress()), new HashSet<String>());
            if (streams.contains(request)) {
                Log.v(TAG, "Removing stream " + request);
                streams.remove(request);
                Log.v(TAG, "Streams after removing " + request + ": " + streams);
                prefs.edit().putStringSet(Preferences.getDeviceKey(band.getMacAddress()), streams).apply();
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

        // Remove stream from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Set<String> streams = prefs.getStringSet(Preferences.getDeviceKey(band.getMacAddress()), new HashSet<String>());
        if (!streams.contains(request)) {
            Log.v(TAG, "Adding stream " + request);
            streams.add(request);
            prefs.edit().putStringSet(Preferences.getDeviceKey(band.getMacAddress()), streams).apply();
        }

        // Request the appropriate stream
        switch (request) {
            case ACCEL_REQ_EXTRA:
                if (accManager == null)
                    accManager = new AccelerometerManager(getApplicationContext());

                accManager.subscribe(band);
                break;
            case ALT_REQ_EXTRA:
                if (altManager == null)
                    altManager = new AltimeterManager(getApplicationContext());

                altManager.subscribe(band);
                break;
            case AMBIENT_REQ_EXTRA:
                if (ambManager == null)
                    ambManager = new AmbientManager(getApplicationContext());

                ambManager.subscribe(band);
                break;
            case BAROMETER_REQ_EXTRA:
                if (barometerManager == null)
                    barometerManager = new BarometerManager(getApplicationContext());

                barometerManager.subscribe(band);
                break;
            case CALORIES_REQ_EXTRA:
                if (calManager == null)
                    calManager = new CaloriesManager(getApplicationContext());

                calManager.subscribe(band);
                break;
            case CONTACT_REQ_EXTRA:
                if (conManager == null)
                    conManager = new ContactManager(getApplicationContext());

                conManager.subscribe(band);
                break;
            case DISTANCE_REQ_EXTRA:
                if (distManager == null)
                    distManager = new DistanceManager(getApplicationContext());

                distManager.subscribe(band);
                break;
            case GSR_REQ_EXTRA:
                if (gsrManager == null)
                    gsrManager = new GsrManager(getApplicationContext());

                gsrManager.subscribe(band);
                break;
            case GYRO_REQ_EXTRA:
                if (gyroManager == null)
                    gyroManager = new GyroscopeManager(getApplicationContext());

                gyroManager.subscribe(band);
                break;
            case HEART_RATE_REQ_EXTRA:
                if (heartManager == null)
                    heartManager = new HeartRateManager(getApplicationContext());

                heartManager.subscribe(band);
                break;
            case PEDOMETER_REQ_EXTRA:
                if (pedoManager == null)
                    pedoManager = new PedometerManager(getApplicationContext());

                pedoManager.subscribe(band);
                break;
            case SKIN_TEMP_REQ_EXTRA:
                if (skinTempManager == null)
                    skinTempManager = new SkinTempManager(getApplicationContext());

                skinTempManager.subscribe(band);
                break;
            case UV_REQ_EXTRA:
                if (uvManager == null)
                    uvManager = new UvManager(getApplicationContext());

                uvManager.subscribe(band);
                break;
            default:
                Log.e(TAG, "Unknown subscription requested " + request);
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
        Log.e(TAG, "Failed to find the MAC address " + mac);
        return null;
    }

}
