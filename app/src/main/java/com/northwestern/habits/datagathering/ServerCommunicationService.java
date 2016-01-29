package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
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
        if(db != null) {
            Log.v(TAG, "DB not null");
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            Log.v(TAG, "Cursor has" + c.getCount());
            new SendTableTask().execute(STUDY);
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
                    try {

                        Cursor c = db.rawQuery("SELECT * FROM " +
                                DataStorageContract.StudyTable.TABLE_NAME, null);
                        c.moveToFirst();
                        Log.v(TAG, "Selected and got cursor with " + c.getCount());
                        int id_col = c.getColumnIndex(DataStorageContract.StudyTable._ID);
                        int name_col = c.getColumnIndex(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID);

                        java.net.URL url = new URL(urlBase + "postStudy.py");

                        while (!c.isAfterLast()) {
                            Log.v(TAG, "Iterating in while loop");
                            HashMap<String, String> send_params = new HashMap<>();
                            send_params.put("id", c.getString(id_col));
                            send_params.put("name", c.getString(name_col));

                            StringBuilder postData = new StringBuilder();
                            for (Map.Entry<String, String> param : send_params.entrySet()) {
                                if (postData.length() != 0) {
                                    postData.append('&');
                                }
                                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                                postData.append('=');
                                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                            }

                            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                            Log.v(TAG, "Encoded post data");

                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                            conn.setDoOutput(true);
                            conn.getOutputStream().write(postDataBytes);

                            Log.v(TAG, "Posted data");

                            InputStreamReader inReader = new InputStreamReader(conn.getInputStream(), "UTF-8");
                            Log.v(TAG, "Made instreamreader");
                            BufferedReader reader = new BufferedReader(inReader);
                            Log.v(TAG, "Got buffered reader");
                            StringBuilder builder = new StringBuilder();
                            //Log.v(TAG, "Reading response");
                            for (String line = null; (line = reader.readLine()) != null; ) {
                                builder.append(line).append("\n");
                            }

                            reader.close();
                            conn.disconnect();
                            //Log.v(TAG, "Disconnected");
                            Log.v(TAG, "Response: " + builder.toString());

                            c.moveToNext();
                        }
                        c.close();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NetworkOnMainThreadException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    break;
                case DEVICE:
                    // Send Device table

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


    private void sendTable(Cursor c) {


    }
}