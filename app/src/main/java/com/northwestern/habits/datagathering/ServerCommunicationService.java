package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerCommunicationService extends Service {
    public ServerCommunicationService() {
        super();
    }

    private final String TAG = "Server communication";
    private final String urlBase = "https://vfsmpmapps10.fsm.northwestern.edu/php/pdotest.cgi";
    private SQLiteDatabase db;

    private final int STUDY = 0;
    private final int DEVICE = 1;
    private final int SENSOR = 2;
    private final int ACCEL = 3;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started service");
        db = (new DataStorageContract.BluetoothDbHelper(getApplicationContext())).getWritableDatabase();
        if (db != null) {
//            Log.v(TAG, "DB not null");
//            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
//            Log.v(TAG, "Cursor has " + c.getCount() + " tables");
//            c.moveToFirst();
//            while (!c.isAfterLast()) {
//                Log.v(TAG, c.getString(c.getColumnIndex("name")));
//                c.moveToNext();
//            }
            new Thread(sendTableRunnable).start();
        }
        //new SendTableTask().execute();
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Service was bound when it shouldn't be.");
        return null;
    }

    private Runnable sendTableRunnable = new Runnable() {
        @Override
        public void run() {

            /* ******* USE FOR EVERY QUERY ********/
            String dbNameField = "dbName";
            String dbName = "testdb";
            String writeField = "write";
            String write = "TRUE";
            String tableNameField = "tableName";


            // Send study table
            Cursor c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.StudyTable.TABLE_NAME, null);
            c.moveToFirst();
            int id_col = c.getColumnIndex(DataStorageContract.StudyTable._ID);
            int name_col = c.getColumnIndex(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(writeField, write);
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("name", c.getString(name_col));
                sparams.put(tableNameField, "STUDY");
                sparams.put(dbNameField, dbName);
                Log.v(TAG, "Sending study " + c.getString(name_col));
                sendBytes(sparams);

                c.moveToNext();
            }

            // Send device table
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.DeviceTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.DeviceTable._ID);
            int study_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID);
            int type_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE);
            int mac_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_MAC);
            int location_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "devices");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("study_id", c.getString(study_col));
                sparams.put("type", c.getString(type_col));
                sparams.put("mac", c.getString(mac_col));
                sparams.put("location", c.getString(location_col));

                Log.v(TAG, "Sending device " + c.getString(type_col));
                sendBytes(sparams);

                c.moveToNext();
            }

            // Send Sensor table
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.SensorTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.SensorTable._ID);
            int device_col = c.getColumnIndex(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID);
            type_col = c.getColumnIndex(DataStorageContract.SensorTable.COLUMN_NAME_TYPE);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "sensors");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("device_id", Integer.toString(c.getInt(device_col)));
                sparams.put("type", c.getString(type_col));

                Log.v(TAG, "Sending sensor " + c.getString(type_col));
                sendBytes(sparams);

                c.moveToNext();
            }


            // Send accelerometer table
            Log.v(TAG, "Querying Accel table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.AccelerometerTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.AccelerometerTable._ID);
            int sensor_id_col = c.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID);
            int date_col = c.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME);
            int x_col = c.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_X);
            int y_col = c.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y);
            int z_col = c.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "accelerometer_data");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("x", Float.toString(c.getFloat(x_col)));
                sparams.put("y", Float.toString(c.getFloat(y_col)));
                sparams.put("z", Float.toString(c.getFloat(z_col)));

                sendBytes(sparams);
                c.moveToNext();
            }

            // Send ambient light table
            Log.v(TAG, "Querying ambient light table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.AmbientLightTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.AmbientLightTable._ID);
            sensor_id_col = c.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_SENSOR_ID);
            date_col = c.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_DATETIME);
            int brightness_col = c.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_BRIGHTNESS);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "ambient_light_table");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("BRIGHTNESS", Integer.toString(c.getInt(brightness_col)));
                sendBytes(sparams);
                c.moveToNext();
            }


            // Send gsr table
            Log.v(TAG, "Querying gsr table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.GsrTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.GsrTable._ID);
            sensor_id_col = c.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_SENSOR_ID);
            date_col = c.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_DATETIME);
            int resist_col = c.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_RESISTANCE);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "gsr");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("resistance", Integer.toString(c.getInt(resist_col)));
                Log.v(TAG, "Sending GSR");
                sendBytes(sparams);
                c.moveToNext();
            }

            // Send gyroscope table
            Log.v(TAG, "Querying Gyro table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.GyroTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.GyroTable._ID);
            sensor_id_col = c.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_SENSOR_ID);
            date_col = c.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_DATETIME);
            x_col = c.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_X);
            y_col = c.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_Y);
            z_col = c.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_Z);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "gyroscope_data");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("x", Float.toString(c.getFloat(x_col)));
                sparams.put("y", Float.toString(c.getFloat(y_col)));
                sparams.put("z", Float.toString(c.getFloat(z_col)));

                sendBytes(sparams);
                c.moveToNext();
            }

            // Send heart table
            Log.v(TAG, "Querying heart table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.HeartRateTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.HeartRateTable._ID);
            sensor_id_col = c.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_SENSOR_ID);
            date_col = c.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_DATETIME);
            int rate_col = c.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_HEART_RATE);
            int quality_col = c.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_QUALITY);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "heart_data");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("quality", c.getString(quality_col));
                sparams.put("heart_rate", Integer.toString(c.getInt(rate_col)));
                sendBytes(sparams);
                c.moveToNext();
            }

            // Send skin temp table
            Log.v(TAG, "Querying skin temp table");
            c = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.TemperatureTable.TABLE_NAME, null);
            c.moveToFirst();
            id_col = c.getColumnIndex(DataStorageContract.TemperatureTable._ID);
            sensor_id_col = c.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_SENSOR_ID);
            date_col = c.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_DATETIME);
            int temp_col = c.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_TEMPERATURE);

            while (!c.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "skin_temp");
                sparams.put("ID", Integer.toString(c.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(c.getInt(sensor_id_col)));
                sparams.put("date", "'" + c.getString(date_col) + "'");
                sparams.put("temp", Float.toString(c.getFloat(temp_col)));
                sendBytes(sparams);
                c.moveToNext();
            }
            Log.e(TAG, "Could not send table because of invalid table name");
        }
    };


    private void sendBytes(HashMap<String, String> sparams) {

        HttpURLConnection conn = null;

        try {

            List<Map.Entry> params = new ArrayList<>();
            for (Map.Entry<String, String> param : sparams.entrySet()) {
                params.add(param);
            }

            URL url = new URL(urlBase + "?" + getQuery(params));
            conn = null;
            Log.v(TAG, "Opening connection");
            conn = (HttpURLConnection) url.openConnection();
            Log.v(TAG, "opened");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Log.v(TAG, "Query: " + url.getQuery());
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            conn.connect();
            Log.v(TAG, "encoding is " + conn.getContentEncoding());

            BufferedReader reader = null;
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null; ) {
                builder.append(line).append("\n");
            }
            Log.v(TAG, "Response: " + builder.toString());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }

    }

    private String getQuery(List<Map.Entry> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry pair : params) {
            if (first)
                first = false;
            else
                result.append("&");

            try {
                result.append(URLEncoder.encode((String) pair.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            result.append("=");
            try {
                result.append(URLEncoder.encode((String) pair.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return result.toString();

    }
}
