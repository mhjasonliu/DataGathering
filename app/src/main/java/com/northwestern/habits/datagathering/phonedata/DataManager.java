package com.northwestern.habits.datagathering.phonedata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by William on 1/23/2016
 */
public abstract class DataManager implements SensorEventListener {

    // Constructor
    public DataManager(String sName, String tag, SQLiteOpenHelper openHelper, Context context) {
        studyName = sName;
        TAG = tag;
        database = openHelper.getWritableDatabase();
        if (database == null) {
            openHelper.onCreate(database);
        }
        this.context = context;
        mSensorManager = ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE));

        // Set mac address
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        mac = wInfo.getMacAddress();
    }

    // Fields
    String studyName;
    protected final String T_PHONE = "Android_Phone";
    protected String TAG = "DataManager"; // Should be reset in the constructor
    SQLiteDatabase database; // Should be reset in the constructor
    protected Context context;
    protected Sensor mSensor;
    protected SensorManager mSensorManager;
    protected String location;
    protected String mac;

    protected abstract void subscribe();
    protected abstract void unSubscribe();

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /**
     * Helper that gets the date and time in proper format for database
     */
    protected String getDateTime(SensorEvent event) {
        return dateFormat.format(event.timestamp);
    }
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
}
