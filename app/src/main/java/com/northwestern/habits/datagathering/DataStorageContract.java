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
     * Class that defines study table
     */
    public static abstract class StudyTable implements BaseColumns {
        public static final String TABLE_NAME = "study";
        public static final String COLUMN_NAME_STUDY_ID = "name";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_STUDY_ID + TEXT_TYPE +
                " )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class DeviceTable implements BaseColumns {
        public static final String TABLE_NAME = "devices";
        public static final String COLUMN_NAME_STUDY_ID = "study_id";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_MAC = "mac";
        public static final String COLUMN_NAME_LOCATION = "location";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_STUDY_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TYPE + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_MAC + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_LOCATION + TEXT_TYPE +
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
        public static final String COLUMN_NAME_STAIRS_DESCENDED = "STAIR_DESC";
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
                        COLUMN_NAME_STAIRS_DESCENDED + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class AmbientTable implements BaseColumns {
        public static final String TABLE_NAME = "ambient_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_BRIGHTNESS = "BRIGHTNESS";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_BRIGHTNESS + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class BarometerTable implements BaseColumns {

        public static final String TABLE_NAME = "barometer_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_PRESSURE = "pressure";
        public static final String COLUMN_NAME_TEMP = "temp";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_PRESSURE + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TEMP + FLOAT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class CaloriesTable implements BaseColumns {

        public static final String TABLE_NAME = "calories_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_CALORIES = "calories";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_CALORIES + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class ContactTable implements BaseColumns {

        public static final String TABLE_NAME = "contact_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_CONTACT_STATE = "contactState";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_CONTACT_STATE + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    public static abstract class DistanceTable implements BaseColumns {

        public static final String TABLE_NAME = "distance_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_CURRENT_MOTION = "currentMotion";
        public static final String COLUMN_NAME_PACE = "pace";
        public static final String COLUMN_NAME_SPEED = "speed";
        public static final String COLUMN_NAME_TOTAL_DISTANCE = "distance";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_CURRENT_MOTION + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_PACE + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_SPEED + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TOTAL_DISTANCE + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    public static abstract class GsrTable implements BaseColumns {

        public static final String TABLE_NAME = "gsr_data";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_RESISTANCE= "resistance";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_RESISTANCE + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    public static abstract class GyroTable implements BaseColumns {
        public static final String TABLE_NAME = "gyroscope_data";
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

    public static abstract class HeartRateTable implements BaseColumns {

        public static final String TABLE_NAME = "heart_data";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_HEART_RATE = "heart_rate";
        public static final String COLUMN_NAME_QUALITY = "quality";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_QUALITY + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_HEART_RATE + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    public static abstract class PedometerTable implements BaseColumns {

        public static final String TABLE_NAME = "pedometer_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_TOTAL_STEPS = "steps";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_TOTAL_STEPS + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class SkinTemperatureTable implements BaseColumns {

        public static final String TABLE_NAME = "skin_temperature_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_TEMPERATURE = "temperature";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_TEMPERATURE + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    public static abstract class UvTable implements BaseColumns {

        public static final String TABLE_NAME = "ultra_violet_table";
        public static final String COLUMN_NAME_SENSOR_ID = "sensor_id";
        public static final String COLUMN_NAME_DATETIME = "date";
        public static final String COLUMN_NAME_INDEX_LEVEL = "uv";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        COLUMN_NAME_SENSOR_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATETIME + DATETIME_TYPE + COMMA_SEP +
                        COLUMN_NAME_INDEX_LEVEL + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }



    public static class BluetoothDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 11;
        public static final String DATABASE_NAME = "Bluetooth.db";

        public BluetoothDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(StudyTable.SQL_CREATE_ENTRIES);
            db.execSQL(DeviceTable.SQL_CREATE_ENTRIES);
            db.execSQL(SensorTable.SQL_CREATE_ENTRIES);
            db.execSQL(AccelerometerTable.SQL_CREATE_ENTRIES);
            db.execSQL(AltimeterTable.SQL_CREATE_ENTRIES);
            db.execSQL(AmbientTable.SQL_CREATE_ENTRIES);
            db.execSQL(BarometerTable.SQL_CREATE_ENTRIES);
            db.execSQL(CaloriesTable.SQL_CREATE_ENTRIES);
            db.execSQL(ContactTable.SQL_CREATE_ENTRIES);
            db.execSQL(DistanceTable.SQL_CREATE_ENTRIES);
            db.execSQL(GsrTable.SQL_CREATE_ENTRIES);
            db.execSQL(GyroTable.SQL_CREATE_ENTRIES);
            db.execSQL(HeartRateTable.SQL_CREATE_ENTRIES);
            db.execSQL(PedometerTable.SQL_CREATE_ENTRIES);
            db.execSQL(SkinTemperatureTable.SQL_CREATE_ENTRIES);
            db.execSQL(UvTable.SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(StudyTable.SQL_DELETE_ENTRIES);
            db.execSQL(DeviceTable.SQL_DELETE_ENTRIES);
            db.execSQL(SensorTable.SQL_DELETE_ENTRIES);
            db.execSQL(AccelerometerTable.SQL_DELETE_ENTRIES);
            db.execSQL(AltimeterTable.SQL_DELETE_ENTRIES);
            db.execSQL(AmbientTable.SQL_DELETE_ENTRIES);
            db.execSQL(BarometerTable.SQL_DELETE_ENTRIES);
            db.execSQL(CaloriesTable.SQL_DELETE_ENTRIES);
            db.execSQL(ContactTable.SQL_DELETE_ENTRIES);
            db.execSQL(DistanceTable.SQL_DELETE_ENTRIES);
            db.execSQL(GsrTable.SQL_DELETE_ENTRIES);
            db.execSQL(GyroTable.SQL_DELETE_ENTRIES);
            db.execSQL(HeartRateTable.SQL_DELETE_ENTRIES);
            db.execSQL(PedometerTable.SQL_DELETE_ENTRIES);
            db.execSQL(SkinTemperatureTable.SQL_DELETE_ENTRIES);
            db.execSQL(UvTable.SQL_DELETE_ENTRIES);
            Log.v("Db", "Deleted tables");
            onCreate(db);
            Log.v("DB", "created new database");
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
