package com.northwestern.habits.datagathering.database;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.northwestern.habits.datagathering.DataGatheringApplication;

/**
 * The purpose of this service is to:
 * 1) manage connections and 2) fulfill streaming requests
 * <p/>
 * 1) Manage connections
 * Accomplished by maintaining a
 */
public class DataManagementService extends Service {

    /**
     * Constants of data types for storing in the database
     */
    public static final String T_ACCEL = "Accelerometer";
    public static final String T_Altimeter = "Altimeter";
    public static final String T_Ambient = "Ambient_Light";
    public static final String T_Barometer = "Barometer";
    public static final String T_Calories = "Calories";
    public static final String T_Contact = "Contact_State";
    public static final String T_Distance = "Distance";
    public static final String T_GSR = "GSR";
    public static final String T_Gyroscope = "Gyroscope";
    public static final String T_Heart_Rate = "Heart_Rate";
    public static final String T_PEDOMETER = "Pedometer";
    public static final String T_SKIN_TEMP = "Skin_Temperature";
    public static final String T_UV = "UV";

    public static final String T_DEVICE = "Device_Type";
    public static final String DEVICE_MAC = "Device_Mac";
    public static final String USER_ID = "User_ID";
    public static final String FIRST_ENTRY = "First_Entry";
    public static final String TYPE = "Type";
    public static final String LAST_ENTRY = "Last_Entry";
    public static final String DATA = "data_series";
    public static final String DATA_KEYS = "Data_Keys";

    public static final int L_NOTHING = 0;
    public static final int L_EATING = 1;
    public static final int L_DRINKING = 2;
    public static final int L_SWALLOW = 3;


    private static final String TAG = "DataManagementService";

    /**
     * Constructor
     */
    public DataManagementService() {
        Thread.setDefaultUncaughtExceptionHandler(DataGatheringApplication.getInstance());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Intent actions for starting the service
     */
    public static final String ACTION_WRITE_CSVS = "write csvs";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_STOP_BACKUP = "stop_backup";


    private Handler mHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        switch (intent.getAction()) {
            case ACTION_WRITE_CSVS:
//                testWriteCSV(getBaseContext());
//                String folderName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                        .getString(Preferences.CURRENT_DOCUMENT, "csvs");
//                try {
//                    exportToCsv(folderName, getApplicationContext());
//                } catch (CouchbaseLiteException e) {
//                    e.printStackTrace();
//                }
                break;
            case ACTION_BACKUP:
                break;
            case ACTION_STOP_BACKUP:
                break;
            default:
                Log.e(TAG, "Non-existant action requested " + intent.getAction());
        }

        return START_REDELIVER_INTENT;
    }

}
