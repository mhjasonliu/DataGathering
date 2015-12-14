package com.northwestern.habits.datagathering;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by William on 11/12/2015.
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


    public static class BluetoothDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
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
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(UserTable.SQL_DELETE_ENTRIES);
            db.execSQL(DeviceTable.SQL_DELETE_ENTRIES);
            db.execSQL(SensorTable.SQL_DELETE_ENTRIES);
            db.execSQL(AccelerometerTable.SQL_DELETE_ENTRIES);
            Log.v("Db", "Deleted tables");
            onCreate(db);
            Log.v("DB", "created new database");
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    public static class DisplayDatabaseTask extends AsyncTask<SQLiteDatabase, Void, Void> {
        @Override
        protected Void doInBackground(SQLiteDatabase... params) {
            Log.v("", "Displaying database");

            SQLiteDatabase db = params[0];
            Cursor userCursor;
            String[] projection = {
                    UserTable._ID,
                    UserTable.COLUMN_NAME_USER_NAME
            };

            userCursor = db.query (
                    UserTable.TABLE_NAME,  // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    UserTable._ID + " DESC"                             // The sort order
            );

            // Log the info
            userCursor.moveToFirst();
            int idCol = userCursor.getColumnIndexOrThrow(UserTable._ID);
            int userNameCol = userCursor.getColumnIndexOrThrow(UserTable.COLUMN_NAME_USER_NAME);
            int devIdCol, devTypeCol, devMacCol;
            int sensIdCol, sensTypeCol;
            int accIdCol, accDateCol, accXCol, accYCol, accZCol;

            int userId, devId, sensId;
            // Loop over users
            Log.v("", "Logging users");
            while( !userCursor.isAfterLast() ) {
                userId = userCursor.getInt(idCol);
                Log.i("", "User ID is: " + Integer.toString(userId));
                Log.i("", "User name is: " + userCursor.getString(userNameCol));

                // Get devices where user
                projection = new String[]{
                        DeviceTable._ID,
                        DeviceTable.COLUMN_NAME_MAC,
                        DeviceTable.COLUMN_NAME_TYPE,
                        DeviceTable.COLUMN_NAME_USER_ID
                };

                // Loop over devices connected to user
                Cursor devCursor = db.query(
                        DeviceTable.TABLE_NAME,
                        projection,
                        DeviceTable.COLUMN_NAME_USER_ID + "=?",
                        new String[] { Integer.toString(userId) },
                        null,
                        null,
                        DeviceTable._ID + " DESC"
                );

                devIdCol = devCursor.getColumnIndexOrThrow(DeviceTable._ID);
                devTypeCol = devCursor.getColumnIndexOrThrow(DeviceTable.COLUMN_NAME_TYPE);
                devMacCol = devCursor.getColumnIndexOrThrow(DeviceTable.COLUMN_NAME_MAC);

                devCursor.moveToFirst();
                while ( !devCursor.isAfterLast() ) {
                    devId = devCursor.getInt(devIdCol);
                    Log.i("", "Device id is: " + Integer.toString(devId));
                    Log.i("", "Device type is: " + devCursor.getString(devTypeCol));
                    Log.i("", "Device Mac address is: " + devCursor.getString(devMacCol));

                    // Loop over sensors on the device
                    projection = new String[]{
                            SensorTable._ID,
                            SensorTable.COLUMN_NAME_TYPE
                    };

                    Cursor sensCursor = db.query(
                            SensorTable.TABLE_NAME,
                            projection,
                            SensorTable.COLUMN_NAME_DEVICE_ID + "=?",
                            new String[] { Integer.toString(devId) },
                            null,
                            null,
                            SensorTable._ID + " DESC"
                    );

                    sensCursor.moveToFirst();
                    sensIdCol = sensCursor.getColumnIndexOrThrow( SensorTable._ID );
                    sensTypeCol = sensCursor.getColumnIndexOrThrow( SensorTable.COLUMN_NAME_TYPE );
                    while ( !sensCursor.isAfterLast() ) {
                        // Display data belonging to sensor
                        sensId = sensCursor.getInt(sensIdCol);
                        Log.i("", "Sensor id is: " + Integer.toString(sensId));
                        Log.i("", "Sensor type is: " + sensCursor.getString(sensTypeCol));


                        projection = new String[] {
                                AccelerometerTable._ID,
                                AccelerometerTable.COLUMN_NAME_DATETIME,
                                AccelerometerTable.COLUMN_NAME_X,
                                AccelerometerTable.COLUMN_NAME_Y,
                                AccelerometerTable.COLUMN_NAME_Z
                        };
                        // Accelerometer data
                        Cursor accCursor = db.query(
                                AccelerometerTable.TABLE_NAME,
                                projection,
                                AccelerometerTable.COLUMN_NAME_SENSOR_ID + "=?",
                                new String[] { Integer.toString(sensId) },
                                null,
                                null,
                                AccelerometerTable.COLUMN_NAME_DATETIME + " ASC"
                        );

                        accCursor.moveToFirst();
                        accDateCol = accCursor.getColumnIndexOrThrow(AccelerometerTable.COLUMN_NAME_DATETIME);
                        accXCol = accCursor.getColumnIndexOrThrow(AccelerometerTable.COLUMN_NAME_X);
                        accYCol = accCursor.getColumnIndexOrThrow(AccelerometerTable.COLUMN_NAME_Y);
                        accZCol = accCursor.getColumnIndexOrThrow(AccelerometerTable.COLUMN_NAME_Z);
                        accIdCol = accCursor.getColumnIndexOrThrow(AccelerometerTable._ID);
                        while ( !accCursor.isAfterLast() ) {
                            Log.i("", "Accelerometer data id: " + accCursor.getInt(accIdCol));
                            Log.i("", "Date and time: " + accCursor.getString(accDateCol));
                            Log.i("", "X: " + accCursor.getInt(accXCol) +
                            "Y: " + accCursor.getInt(accYCol) + "Z: " + accCursor.getInt(accZCol));

                            accCursor.moveToNext();
                        }

                        sensCursor.moveToNext();
                    }
                    devCursor.moveToNext();
                }
                userCursor.moveToNext();
            }
            return null;
        }
    }



}
