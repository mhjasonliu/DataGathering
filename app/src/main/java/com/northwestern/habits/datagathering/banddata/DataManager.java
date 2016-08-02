package com.northwestern.habits.datagathering.banddata;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandSensorEvent;
import com.northwestern.habits.datagathering.DataStorageContract;

import java.text.SimpleDateFormat;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by William on 12/31/2015
 */
public abstract class DataManager implements EventListener {

    // Constructor
    public DataManager(String tag, Context context, int buffSize) {
        TAG = tag;
        this.context = context;
        toastingFailure = false;
        if (mHandler == null)
            mHandler = new Handler();

        BUFFER_SIZE = buffSize;
    }


    // Fields
    protected HashMap<BandInfo, CustomListener> listeners = new HashMap<>();
    protected HashMap<BandInfo, BandClient> clients = new HashMap<>();

    String studyName;
    protected final String T_BAND2 = "Microsoft_Band_2";
    protected String TAG = "DataManager"; // Should be reset in the constructor
    SQLiteDatabase database; // Should be reset in the constructor
    protected Context context;
    protected Handler mHandler;
    protected long TIMEOUT_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    protected int restartCount = 0;
    protected String STREAM_TYPE;
    protected static int label = 0;

    protected DataSeries dataBuffer;
    protected int BUFFER_SIZE = 1;

    protected static boolean toastingFailure;

    protected static boolean disconnectDetected = false;
    protected static boolean toastEnabled = true;
    protected static Map<String, Integer> connectionFailedMap = new HashMap<>();

    protected abstract void subscribe(BandInfo info);

    protected abstract void unSubscribe(BandInfo info);

    protected void reconnectBand() {
        if (!disconnectDetected) {
            disconnectDetected = true;
            toastEnabled = false;
            // Capture all of the redundant disconnect requests
            try {
                Log.v(TAG, "Reconnecting in 1 minute...");
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Send a start request to the service to attempt to reconnect to sensors
            Log.v(TAG, "Reconnecting...");
            context.startService(new Intent(context, BandDataService.class));
            toastEnabled = true;
            disconnectDetected = false;
        }
    }


    /*
     * Below are methods from the old implementation of BandDataService. This implementation
     * allows for a less cluttered BandDataService and eliminates need for static calls.
     */

    /**
     * Helper that gets the date and time in proper format for database
     */
    protected String getDateTime(BandSensorEvent event) {
        return dateFormat.format(event.getTimestamp());
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /**
     * Gets the _ID value for the study in the database
     *
     * @param studyId study name to search for
     * @param db      database to search for the study name
     * @return the integer _ID or
     * @throws android.content.res.Resources.NotFoundException when study name cannot be found
     */
    protected int getStudyId(String studyId, SQLiteDatabase db) throws Resources.NotFoundException {

        // Querry databse for the study name
        String[] projection = new String[]{
                DataStorageContract.StudyTable._ID,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID
        };

        // Query for the specified studyId
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID + "=?",
                new String[]{studyId},
                null,
                null,
                null
        );
        cursor.moveToFirst();

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();


        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.StudyTable._ID));
        cursor.close();
        return tmp;
    }


    /**
     * Uses the next unused ID
     *
     * @param db database to find the lowest study in
     * @return the lowest unused _ID
     */
    protected int getNewStudy(SQLiteDatabase db) {
        String[] projection = new String[]{
                DataStorageContract.StudyTable._ID,
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.StudyTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at study entry with largest ID
        int studyIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.StudyTable._ID);

        int tmp = cursor.getInt(studyIdCol) + 1;
        cursor.close();
        return tmp;
    }

    /**
     * Gets the device id for the device specified
     *
     * @param mac   address (physical) of the device
     * @param study id of the study
     * @param db    database to query
     * @return id of the device
     * @throws android.content.res.Resources.NotFoundException
     */
    protected int getDevId(String location, String mac, int study, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[]{
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC,
                DataStorageContract.DeviceTable._ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID,
                DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION
        };


        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                DataStorageContract.DeviceTable.COLUMN_NAME_MAC + "=?" + " AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID + "=?" + " AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION + "=?",
                new String[]{mac, Integer.toString(study), location},
                null,
                null,
                null
        );

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();

        cursor.moveToFirst();

        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.DeviceTable._ID));
        cursor.close();
        return tmp;
    }

    /**
     * Gets the next largest ID for the device
     *
     * @param db to search
     * @return int available ID in the device list
     */
    protected int getNewDev(SQLiteDatabase db) {
        String[] projection = new String[]{
                DataStorageContract.DeviceTable._ID
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.DeviceTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.DeviceTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at Study entry with largest ID
        int devIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.DeviceTable._ID);

        int tmp = cursor.getInt(devIdCol) + 1;
        cursor.close();
        return tmp;
    }

    /**
     * Gets the ID for the sensor associated with the device and sensor type
     *
     * @param type   of sensor
     * @param device ID in the SQLite db associated with the sensor
     * @param db     to query
     * @return ID of the sensor or not
     * @throws android.content.res.Resources.NotFoundException
     */
    protected int getSensorId(String type, int device, SQLiteDatabase db)
            throws Resources.NotFoundException {
        String[] projection = new String[]{
                DataStorageContract.SensorTable.COLUMN_NAME_TYPE,
                DataStorageContract.SensorTable._ID,
                DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID
        };


        Cursor cursor = db.query(
                DataStorageContract.SensorTable.TABLE_NAME,
                projection,
                DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID + "=?" +
                        " AND " +
                        DataStorageContract.SensorTable.COLUMN_NAME_TYPE + "=?",
                new String[]{Integer.toString(device), type},
                null,
                null,
                null
        );

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();

        cursor.moveToFirst();

        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.SensorTable._ID));
        cursor.close();
        return tmp;
    }

    protected int getNewSensor(SQLiteDatabase db) {
        String[] projection = new String[]{
                DataStorageContract.SensorTable._ID
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.SensorTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.SensorTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at Study entry with largest ID
        int sensIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.SensorTable._ID);

        int tmp = cursor.getInt(sensIdCol) + 1;
        cursor.close();
        return tmp;
    }

    protected BandClient connectBandClient(BandInfo band, BandClient client)
            throws InterruptedException, BandException {
        if (client == null) {
            if (band == null) {
                throw new NullPointerException();
            }
            client = BandClientManager.getInstance().create(context, band);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return client;
        }

        try {
            if (ConnectionState.CONNECTED == client.connect().await(15, TimeUnit.SECONDS)) {
                return client;
            } else {
                return null;
            }
        } catch (TimeoutException e) {
            return null;
        }
    }


    /* ******************************** TOASTS ***************************** */

    protected void toastStreaming(final String type) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (toastEnabled) {
                    Toast.makeText(context, "Band is streaming " + type, Toast.LENGTH_SHORT).show();
                }
            }
        });
        cancelToast = null;
    }

    protected void notifySuccess(BandInfo info) {
        if (connectionFailedMap.containsKey(info.getMacAddress())) {
            int id = connectionFailedMap.get(info.getMacAddress());
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
        }
    }

    protected void notifyFailure(BandInfo info) {
        // Create a notification asking the user to make sure that the band is on
        // And if it is to restart it and the phone
        if (!connectionFailedMap.containsKey(info.getMacAddress())) {
            Notification.Builder b = new Notification.Builder(context);
            b.setSmallIcon(android.R.drawable.alert_light_frame);
            b.setContentTitle("Could not connect to band " + info.getName());
            b.setStyle(new Notification.BigTextStyle().bigText("Could not connect to the band '" + info.getName()
                    + "'. Please check that the band is on and in range. If it is, " +
                    "please restart the band and wait a few minutes. If " +
                    "this alert does not go away, please restart both " +
                    "the band and your phone. Sorry for the inconvenience."));
            b.setOngoing(true);
            b.setVibrate(new long[]{0, 500});
            b.setUsesChronometer(true);
            b.setPriority(Notification.PRIORITY_HIGH);
            Uri alarmsound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            b.setSound(alarmsound);


            Notification n = b.build();
            int id = new Random().nextInt(1024);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
            connectionFailedMap.put(info.getMacAddress(), id);

        }
    }

    protected void toastFailure() {
        mHandler.post(failure_toast_runnable);
    }

    protected void toastAlreadyStreaming() {
        mHandler.post(alreadyStreamingRunnable);
    }

    private static Toast cancelToast = null;

    private Runnable alreadyStreamingRunnable = new Runnable() {
        @Override
        public void run() {
            cancelToast = Toast.makeText(context, "Band is already streaming " + STREAM_TYPE,
                    Toast.LENGTH_SHORT);
            cancelToast.show();
        }
    };

    private Runnable failure_toast_runnable = new Runnable() {
        @Override
        public void run() {
            cancelToast = Toast.makeText(context, "Could not connect to band. You may need to " +
                    "restart the band and the phone.", Toast.LENGTH_SHORT);
            cancelToast.show();
        }
    };

    protected Runnable unsubscribedToastRunnable = new Runnable() {
        @Override
        public void run() {
            if (toastEnabled) {
                Toast.makeText(context, "Unsubscribed from " + STREAM_TYPE, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /* ******************************** TIMEOUT STUFF ***************************** */

    protected class TimeoutHandler extends Thread {
        private boolean shouldTerminate = false;

        public TimeoutHandler() {
            super();
            shouldTerminate = false;
        }

        public void makeThreadTerminate() {
            shouldTerminate = true;
        }

        @Override
        public void run() {
            while (!shouldTerminate) {
                // Iterate through stored event handlers
                long timeout;
                long interval;
                for (BandInfo this_info : listeners.keySet()) {
                    CustomListener listener = listeners.get(this_info);
                    // Check timeout field
                    timeout = listener.lastDataSample;
                    interval = System.currentTimeMillis() - timeout;

                    if (timeout != 0
                            && interval > TIMEOUT_INTERVAL) {
                        // Timeout occurred, un-subscribe the current listener
//                        unSubscribe(this_info);
                        Log.d(TAG, STREAM_TYPE + " timeout detected");
                        Log.e(TAG, "interval: " + interval);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, STREAM_TYPE +
                                        " timeout detected...", Toast.LENGTH_SHORT).show();
                            }
                        });

                        try {
                            Thread.sleep(TIMEOUT_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Subscribe again
//                        subscribe(this_info);
                        if (DataManager.this instanceof GyroscopeManager) {
                            ((GyroscopeManager) DataManager.this).restartSubscription(this_info);
                        }
                        final int innerCount = ++restartCount;
                        final String restartText = "Restarting " +
                                STREAM_TYPE + " for the " + Integer.toString(innerCount) +
                                "th time";
                        Log.v(TAG, restartText);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, restartText, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                try {
                    Thread.sleep(TIMEOUT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (listeners.size() == 0) {
                    // All listeners have been unsubscribed
                    break;
                }
            }
        }
    }

    protected class CustomListener implements EventListener {
        protected long lastDataSample;
        protected BandInfo info;
    }

}