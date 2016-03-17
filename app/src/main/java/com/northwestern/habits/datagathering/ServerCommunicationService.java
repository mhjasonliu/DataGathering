package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ServerCommunicationService extends Service {
    public ServerCommunicationService() {
        super();
    }

    private final String TAG = "Server communication";
    private final String urlBase = "http://murphy.wot.eecs.northwestern.edu/~wgs068/habitsData/";
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
            Log.v(TAG, "DB not null");
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            Log.v(TAG, "Cursor has " + c.getCount() + " tables");
            new SendTableTask().execute(STUDY);
            new SendTableTask().execute(DEVICE);
        }
        //new SendTableTask().execute();
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Service was bound when it shouldn't be.");
        return null;
    }


    private class SendTableTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {

            Integer tableName = params[0];
            switch (tableName) {
                case STUDY:
                    // Send study table
                    Cursor c = db.rawQuery("SELECT * FROM " +
                            DataStorageContract.StudyTable.TABLE_NAME, null);
                    Log.v(TAG, "Study table returns " + Integer.toString(c.getCount()));
                    c.moveToFirst();
                    Log.v(TAG, "Selected and got cursor with " + c.getCount());
                    int id_col = c.getColumnIndex(DataStorageContract.StudyTable._ID);
                    int name_col = c.getColumnIndex(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID);

                    try {
                        URL url = new URL(urlBase + "postStudy.py");

                        while (!c.isAfterLast()) {

                            HashMap<String, String> sparams = new HashMap<>();
                            sparams.put("id", Integer.toString(c.getInt(id_col)));
                            sparams.put("name", c.getString(name_col));
                            Log.v(TAG, "Sending study " + c.getString(name_col));
                            sendBytes(sparams, url);

                            c.moveToNext();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    break;
                case DEVICE:
                    // Send device table
                    c = db.rawQuery("SELECT * FROM " +
                            DataStorageContract.DeviceTable.TABLE_NAME, null);
                    c.moveToFirst();
                    id_col = c.getColumnIndex(DataStorageContract.DeviceTable._ID);
                    int study_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID);
                    int type_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE);
                    int mac_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_MAC);
                    int location_col = c.getColumnIndex(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION);

                    Log.v(TAG, "REquest is " + Integer.toString(c.getInt(id_col)) +
                    c.getInt(study_col) +
                    c.getString(type_col) +
                    c.getString(mac_col) +
                    c.getString(location_col));


                    try {
                        URL url = new URL(urlBase + "postDevice.py");

                        while (!c.isAfterLast()) {
                            HashMap<String, String> sparams = new HashMap<>();
                            sparams.put("id", Integer.toString(c.getInt(id_col)));
                            sparams.put("study_id", c.getString(study_col));
                            sparams.put("type", c.getString(type_col));
                            sparams.put("mac_address", c.getString(mac_col));
                            sparams.put("location", c.getString(location_col));

                            Log.v(TAG, "Sending device " + c.getString(type_col));
                            sendBytes(sparams, url);

                            c.moveToNext();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }


                    break;
                case SENSOR:
                    // Send Sensor table

                    break;
                case ACCEL:
                    // Send accelerometer table

                    break;
                default:
                    Log.e(TAG, "Could not send table because of invalid table name");
                    Log.e(TAG, "Table name given is " + tableName);
                    break;
            }
            stopSelf();
            return null;
        }
    }


    private void sendBytes(HashMap<String, String> sparams, URL url) {

        HttpURLConnection conn = null;

        try {

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : sparams.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
            }

            byte[] postDataBytes;
            postDataBytes = postData.toString().getBytes("UTF-8");

            conn = null;
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");


            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            Log.v(TAG, "*********************************" + postData.toString());
            conn.getOutputStream().write(postDataBytes);

            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
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
