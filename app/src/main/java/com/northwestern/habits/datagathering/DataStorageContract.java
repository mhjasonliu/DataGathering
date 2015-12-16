package com.northwestern.habits.datagathering;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by William Stogin on 11/12/2015.
 *
 * Holds constant strings for use with the database
 */
public final class DataStorageContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DataStorageContract() {}



    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String FLOAT_TYPE = " FLOAT";
    private static final String DATETIME_TYPE = " DATETIME";
    private static final String COMMA_SEP = ",";

    /**
     * Class that defines user table
     */
    public static abstract class UserTable implements BaseColumns {
        public static final String TABLE_NAME = "user";
        public static final String COLUMN_NAME_USER_NAME = "name";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_USER_NAME + TEXT_TYPE +
                " )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class DeviceTable implements BaseColumns {
        public static final String TABLE_NAME = "devices";
        public static final String COLUMN_NAME_USER_ID = "user_id";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_MAC = "mac";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_USER_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TYPE + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_MAC + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class SensorTable implements BaseColumns {
        public static final String TABLE_NAME = "sensors";
        public static final String COLUMN_NAME_DEVICE_ID = "device_id";
        public static final String COLUMN_NAME_TYPE = "type";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_DEVICE_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TYPE + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class AccelerometerTable implements BaseColumns {
        public static final String TABLE_NAME = "accelerometer_data";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "y";
        public static final String COLUMN_NAME_Z = "z";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_X + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_Y + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_Z + FLOAT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class AltimeterTable implements BaseColumns {
        public static final String TABLE_NAME = "altimeter_data";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_TOTAL_GAIN = "total_gain";
        public static final String COLUMN_NAME_TOTAL_LOSS ="total_loss";
        public static final String COLUMN_NAME_STEPPING_GAIN = "STEP_GAIN";
        public static final String COLUMN_NAME_STEPPING_LOSS = "STEP_LOSS";
        public static final String COLUMN_NAME_STEPS_ASCENDED = "STEP_ASC";
        public static final String COLUMN_NAME_STEPS_DESCENDED = "STEP_DESC";
        public static final String COLUMN_NAME_RATE = "RATE";
        public static final String COLUMN_NAME_STAIRS_ASCENDED = "STAIR_ASC";
        public static final String COLUMN_NAME_STAIRS_DESCENEDED = "STAIR_DESC";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_TOTAL_GAIN + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TOTAL_LOSS + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STEPPING_GAIN + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STEPPING_LOSS + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STEPS_ASCENDED + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STEPS_DESCENDED + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_RATE + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STAIRS_ASCENDED + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STAIRS_DESCENEDED + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    public static class BluetoothDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "Bluetooth.db";

        public BluetoothDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(UserTable.SQL_CREATE_ENTRIES);
            db.execSQL(DeviceTable.SQL_CREATE_ENTRIES);
            db.execSQL(SensorTable.SQL_CREATE_ENTRIES);
            Log.v("", AccelerometerTable.SQL_CREATE_ENTRIES);
            db.execSQL(AccelerometerTable.SQL_CREATE_ENTRIES);
            db.execSQL(AltimeterTable.SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(UserTable.SQL_DELETE_ENTRIES);
            db.execSQL(DeviceTable.SQL_DELETE_ENTRIES);
            db.execSQL(SensorTable.SQL_DELETE_ENTRIES);
            db.execSQL(AccelerometerTable.SQL_DELETE_ENTRIES);
            db.execSQL(AltimeterTable.SQL_DELETE_ENTRIES);
            Log.v("Db", "Deleted tables");
            onCreate(db);
            Log.v("DB", "created new database");
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
