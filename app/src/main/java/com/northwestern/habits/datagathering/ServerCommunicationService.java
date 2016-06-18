package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public static final String MESSAGE_EXTRA = "message";

    private Message message;

    private final String TAG = "Server communication";
    private final String urlBase = "https://vfsmpmapps10.fsm.northwestern.edu/php/";
    private SQLiteDatabase db;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private final int STUDY = 0;
    private final int DEVICE = 1;
    private final int SENSOR = 2;
    private final int ACCEL = 3;
    private final int LIMIT = 200;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started service");

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                message = (Message) extras.get(MESSAGE_EXTRA);
                Log.v(TAG, "message is " + message.toString());
            }
        }
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

            Cursor studyCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.StudyTable.TABLE_NAME, null);
            Cursor deviceCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.DeviceTable.TABLE_NAME, null);
            Cursor sensorCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.SensorTable.TABLE_NAME, null);
            Cursor accelCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.AccelerometerTable.TABLE_NAME, null);
            Cursor ambientCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.AmbientLightTable.TABLE_NAME, null);
            Cursor gsrCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.GsrTable.TABLE_NAME, null);
            Cursor gyroCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.GyroTable.TABLE_NAME, null);
            Cursor heartCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.HeartRateTable.TABLE_NAME, null);
            Cursor tempCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.TemperatureTable.TABLE_NAME, null);

            int totalEntries = studyCursor.getCount()
                    + deviceCursor.getCount()
                    + sensorCursor.getCount()
                    + accelCursor.getCount()
                    + ambientCursor.getCount()
                    + gsrCursor.getCount()
                    + gyroCursor.getCount()
                    + heartCursor.getCount()
                    + tempCursor.getCount();
            int entriesSoFar = 0;


            // Send study table
            studyCursor.moveToFirst();
            int id_col = studyCursor.getColumnIndex(DataStorageContract.StudyTable._ID);
            int name_col = studyCursor.getColumnIndex(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID);
            Log.v(TAG, Integer.toString(studyCursor.getCount()));
            while (!studyCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(writeField, write);
                sparams.put("ID", Integer.toString(studyCursor.getInt(id_col)));
                sparams.put("name", studyCursor.getString(name_col));
                sparams.put(tableNameField, "STUDY");
                sparams.put(dbNameField, dbName);
                Log.v(TAG, "Sending study " + studyCursor.getString(name_col));
                sendBytes(sparams);

                entriesSoFar++;
                Log.v(TAG, "Sending study table");
                studyCursor.moveToNext();
            }

            broadcastProgress(entriesSoFar);

            // Send device table
            deviceCursor.moveToFirst();
            id_col = deviceCursor.getColumnIndex(DataStorageContract.DeviceTable._ID);
            int study_col = deviceCursor.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID);
            int type_col = deviceCursor.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE);
            int mac_col = deviceCursor.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_MAC);
            int location_col = deviceCursor.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION);

            while (!deviceCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "devices");
                sparams.put("ID", Integer.toString(deviceCursor.getInt(id_col)));
                sparams.put("study_id", deviceCursor.getString(study_col));
                sparams.put("type", deviceCursor.getString(type_col));
                sparams.put("mac", deviceCursor.getString(mac_col));
                sparams.put("location", deviceCursor.getString(location_col));

                Log.v(TAG, "Sending device " + deviceCursor.getString(type_col));
                sendBytes(sparams);

                deviceCursor.moveToNext();
                entriesSoFar++;
            }

            broadcastProgress(entriesSoFar);


            // Send Sensor table
            sensorCursor.moveToFirst();
            id_col = sensorCursor.getColumnIndex(DataStorageContract.SensorTable._ID);
            int device_col = sensorCursor.getColumnIndex(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID);
            type_col = sensorCursor.getColumnIndex(DataStorageContract.SensorTable.COLUMN_NAME_TYPE);

            while (!sensorCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "sensors");
                sparams.put("ID", Integer.toString(sensorCursor.getInt(id_col)));
                sparams.put("device_id", Integer.toString(sensorCursor.getInt(device_col)));
                sparams.put("type", sensorCursor.getString(type_col));

                Log.v(TAG, "Sending sensor " + sensorCursor.getString(type_col));
                sendBytes(sparams);

                sensorCursor.moveToNext();
                entriesSoFar++;
            }
            broadcastProgress(entriesSoFar);



            // Send accelerometer table
            Log.v(TAG, "Querying Accel table");
            accelCursor = db.rawQuery("SELECT * FROM " +
                    DataStorageContract.AccelerometerTable.TABLE_NAME +
                    " ORDER BY " + DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME +
                    " LIMIT " + Integer.toString(LIMIT), null);
            id_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable._ID);
            int sensor_id_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_SENSOR_ID);
            int date_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME);
            int x_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_X);
            int y_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_Y);
            int z_col = accelCursor.getColumnIndex(DataStorageContract.AccelerometerTable.COLUMN_NAME_Z);

            while (accelCursor.getCount() > 0) {
                accelCursor.moveToFirst();
                // Create a new accelerometer json array
                JSONArray accJArray = new JSONArray();
                HashMap<String, String> sparams1 = new HashMap<>();
                while (!accelCursor.isAfterLast()) {

                    sparams1.put(dbNameField, dbName);
                    sparams1.put(writeField, write);

                    sparams1.put(tableNameField, "accelerometer_data");
//                sparams.put("ID", Integer.toString(accelCursor.getInt(id_col)));
//                sparams.put("sensor_id", Integer.toString(accelCursor.getInt(sensor_id_col)));
//                sparams.put("date", "'" + accelCursor.getString(date_col) + "'");
//                sparams.put("x", Float.toString(accelCursor.getFloat(x_col)));
//                sparams.put("y", Float.toString(accelCursor.getFloat(y_col)));
//                sparams.put("z", Float.toString(accelCursor.getFloat(z_col)));
//
//                sendBytes(sparams);
                    accJArray.put(accelCursor.getInt(id_col));
                    accJArray.put(accelCursor.getInt(sensor_id_col));
                    accJArray.put("'" + accelCursor.getString(date_col) + "'");
                    accJArray.put(Float.toString(accelCursor.getFloat(x_col)));
                    accJArray.put(Float.toString(accelCursor.getFloat(y_col)));
                    accJArray.put(Float.toString(accelCursor.getFloat(z_col)));

                    accelCursor.moveToNext();
                    entriesSoFar++;
                }
                JSONObject accobj = new JSONObject();
                try {
                    accobj.put("Data", accJArray);
                    sendJSONObject(sparams1, accobj);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to put the json array in the json object");
                    e.printStackTrace();
                }

                broadcastProgress(entriesSoFar / totalEntries * 100);
                db.rawQuery("DELETE FROM " + DataStorageContract.AccelerometerTable.TABLE_NAME +
                        " WHERE " + DataStorageContract.AccelerometerTable._ID +
                        " IN (SELECT " + DataStorageContract.AccelerometerTable._ID +
                        " FROM " + DataStorageContract.AccelerometerTable.TABLE_NAME +
                        " ORDER BY " + DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME + " DESC " +
                        "LIMIT " + Integer.toString(46) + ")", null);
                accelCursor = db.rawQuery("SELECT * FROM " +
                        DataStorageContract.AccelerometerTable.TABLE_NAME +
                        " ORDER BY " + DataStorageContract.AccelerometerTable.COLUMN_NAME_DATETIME + " DESC " +
                        "LIMIT " + Integer.toString(LIMIT), null);
                Log.v(TAG, "Cursor size: " + Integer.toString(accelCursor.getCount()));
                break;
            }

            // Send ambient light table
            Log.v(TAG, "Querying ambient light table");
            ambientCursor.moveToFirst();
            id_col = ambientCursor.getColumnIndex(DataStorageContract.AmbientLightTable._ID);
            sensor_id_col = ambientCursor.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_SENSOR_ID);
            date_col = ambientCursor.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_DATETIME);
            int brightness_col = ambientCursor.getColumnIndex(DataStorageContract.AmbientLightTable.COLUMN_NAME_BRIGHTNESS);

            while (!ambientCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "ambient_light_table");
                sparams.put("ID", Integer.toString(ambientCursor.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(ambientCursor.getInt(sensor_id_col)));
                sparams.put("date", "'" + ambientCursor.getString(date_col) + "'");
                sparams.put("BRIGHTNESS", Integer.toString(ambientCursor.getInt(brightness_col)));
                sendBytes(sparams);
                ambientCursor.moveToNext();
                entriesSoFar++;
            }
//            broadcastProgress(entriesSoFar/totalEntries*100);

            // Send gsr table
            Log.v(TAG, "Querying gsr table");
            gsrCursor.moveToFirst();
            id_col = gsrCursor.getColumnIndex(DataStorageContract.GsrTable._ID);
            sensor_id_col = gsrCursor.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_SENSOR_ID);
            date_col = gsrCursor.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_DATETIME);
            int resist_col = gsrCursor.getColumnIndex(DataStorageContract.GsrTable.COLUMN_NAME_RESISTANCE);

            while (!gsrCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "gsr");
                sparams.put("ID", Integer.toString(gsrCursor.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(gsrCursor.getInt(sensor_id_col)));
                sparams.put("date", "'" + gsrCursor.getString(date_col) + "'");
                sparams.put("resistance", Integer.toString(gsrCursor.getInt(resist_col)));
                Log.v(TAG, "Sending GSR");
                sendBytes(sparams);
                gsrCursor.moveToNext();
                entriesSoFar++;
            }
//            broadcastProgress(entriesSoFar/totalEntries*100);

            // Send gyroscope table
            Log.v(TAG, "Querying Gyro table");
            gyroCursor.moveToFirst();
            id_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable._ID);
            sensor_id_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_SENSOR_ID);
            date_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_DATETIME);
            x_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_X);
            y_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_Y);
            z_col = gyroCursor.getColumnIndex(DataStorageContract.GyroTable.COLUMN_NAME_Z);

            while (!gyroCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();

                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "gyroscope_data");
                sparams.put("ID", Integer.toString(gyroCursor.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(gyroCursor.getInt(sensor_id_col)));
                sparams.put("date", "'" + gyroCursor.getString(date_col) + "'");
                sparams.put("x", Float.toString(gyroCursor.getFloat(x_col)));
                sparams.put("y", Float.toString(gyroCursor.getFloat(y_col)));
                sparams.put("z", Float.toString(gyroCursor.getFloat(z_col)));

                sendBytes(sparams);
                gyroCursor.moveToNext();
                entriesSoFar++;
            }
//            broadcastProgress(entriesSoFar/totalEntries*100);

            // Send heart table
            Log.v(TAG, "Querying heart table");
            heartCursor.moveToFirst();
            id_col = heartCursor.getColumnIndex(DataStorageContract.HeartRateTable._ID);
            sensor_id_col = heartCursor.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_SENSOR_ID);
            date_col = heartCursor.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_DATETIME);
            int rate_col = heartCursor.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_HEART_RATE);
            int quality_col = heartCursor.getColumnIndex(DataStorageContract.HeartRateTable.COLUMN_NAME_QUALITY);

            while (!heartCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "heart_data");
                sparams.put("ID", Integer.toString(heartCursor.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(heartCursor.getInt(sensor_id_col)));
                sparams.put("date", "'" + heartCursor.getString(date_col) + "'");
                sparams.put("quality", heartCursor.getString(quality_col));
                sparams.put("heart_rate", Integer.toString(heartCursor.getInt(rate_col)));
                sendBytes(sparams);
                heartCursor.moveToNext();
                entriesSoFar++;
            }
//            broadcastProgress(entriesSoFar/totalEntries*100);

            // Send skin temp table
            Log.v(TAG, "Querying skin temp table");
            tempCursor.moveToFirst();
            id_col = tempCursor.getColumnIndex(DataStorageContract.TemperatureTable._ID);
            sensor_id_col = tempCursor.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_SENSOR_ID);
            date_col = tempCursor.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_DATETIME);
            int temp_col = tempCursor.getColumnIndex(DataStorageContract.TemperatureTable.COLUMN_NAME_TEMPERATURE);

            while (!tempCursor.isAfterLast()) {
                HashMap<String, String> sparams = new HashMap<>();
                sparams.put(dbNameField, dbName);
                sparams.put(writeField, write);

                sparams.put(tableNameField, "skin_temp");
                sparams.put("ID", Integer.toString(tempCursor.getInt(id_col)));
                sparams.put("sensor_id", Integer.toString(tempCursor.getInt(sensor_id_col)));
                sparams.put("date", "'" + tempCursor.getString(date_col) + "'");
                sparams.put("temp", Float.toString(tempCursor.getFloat(temp_col)));
                sendBytes(sparams);
                tempCursor.moveToNext();
                entriesSoFar++;
            }
//            broadcastProgress(entriesSoFar / totalEntries * 100);
            broadcastProgress(100);
        }
    };


    private void sendBytes(HashMap<String, String> sparams) {

        HttpURLConnection conn = null;

        try {

            List<Map.Entry> params = new ArrayList<>();
            for (Map.Entry<String, String> param : sparams.entrySet()) {
                params.add(param);
            }

            URL url = new URL(urlBase + "pdotest.cgi" + "?" + getQuery(params));
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
            } catch (NullPointerException e1) {
                e1.printStackTrace();
            }
        }

        return result.toString();

    }

    private void broadcastProgress(float progress) {
        Intent bIntent = new Intent(MainActivity.PROGRESS_BROADCAST_EXTRA);
        bIntent.putExtra(MainActivity.PROGRESS_EXTRA, Math.round(progress));
        Log.v(TAG, "BORADCSTING " + Float.toString(progress));
        sendBroadcast(bIntent);
    }

    private void sendJSONObject(HashMap<String,String> sparams, JSONObject object) {
        HttpURLConnection conn = null;

        sparams.put("Data", object.toString());
        try {

            List<Map.Entry> params = new ArrayList<>();
            for (Map.Entry<String, String> param : sparams.entrySet()) {
                params.add(param);
            }

            URL url = new URL(urlBase + "accelJSON.cgi" + "?" + getQuery(params));
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
            Log.e(TAG, object.toString());
            writer.write(object.toString());
            Log.e(TAG, "1");
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
}


