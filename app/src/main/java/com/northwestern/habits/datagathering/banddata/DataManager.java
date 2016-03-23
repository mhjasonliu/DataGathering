package com.northwestern.habits.datagathering.banddata;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
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

/**
 * Created by William on 12/31/2015
 */
public abstract class DataManager implements EventListener {

    // Constructor
    public DataManager(String sName, String tag, SQLiteOpenHelper helper, Context context) {
        studyName = sName;
        TAG = tag;
        database = helper.getWritableDatabase();
        this.context = context;
        toastingFailure = false;
        if (mHandler == null)
            mHandler = new Handler();
    }


    // Fields
    protected HashMap<BandInfo, EventListener> listeners = new HashMap<>();
    protected HashMap<BandInfo, BandClient> clients = new HashMap<>();

    String studyName;
    protected final String T_BAND2 = "Microsoft_Band_2";
    protected String TAG = "DataManager"; // Should be reset in the constructor
    SQLiteDatabase database; // Should be reset in the constructor
    protected Context context;
    protected Handler mHandler;

    protected static boolean toastingFailure;

    protected abstract void subscribe(BandInfo info);

    protected abstract void unSubscribe(BandInfo info);




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

    protected BandClient connectBandClient(BandInfo band, BandClient client) throws InterruptedException, BandException {
        if (client == null) {
            client = BandClientManager.getInstance().create(context, band);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return client;
        }

        if (ConnectionState.CONNECTED == client.connect().await()) {
            return client;
        } else {
            return null;
        }
    }

    protected void toastStreaming(final String type) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Band is streaming " + type, Toast.LENGTH_LONG).show();
            }
        });
        cancelToast = null;
    }

    protected void toastFailure() {
        mHandler.post(r);
    }

    private static Toast cancelToast = null;

    Runnable r = new Runnable() {
        @Override
        public void run() {
            if (cancelToast == null) {
                cancelToast = Toast.makeText(context, "Band isn't connected. Please make sure bluetooth is on and " +
                    "the band is in range.", Toast.LENGTH_LONG);
                cancelToast.show();
            }
        }
    };

}
