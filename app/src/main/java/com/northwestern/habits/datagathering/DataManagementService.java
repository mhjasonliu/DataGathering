package com.northwestern.habits.datagathering;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.replicator.Replication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purpose of this service is to:
 * 1) manage connections and 2) fulfill streaming requests
 * <p/>
 * 1) Manage connections
 * Accomplished by maintaining a
 */
public class DataManagementService extends Service {

    public static final String T_ACCEL = "Accelerometer";
    public static final String T_Altimeter = "Altimeter";
    public static final String T_Ambient = "Ambient_Light";
    public static final String T_Barometer = "Barometer";
    public static final String T_Calories = "Calories";
    public static final String T_Distance = "Distance";
    public static final String T_GSR = "GSR";
    public static final String T_Gyroscope = "Gyroscope";
    public static final String T_Heart_Rate = "Heart_Rate";
    public static final String T_PEDOMETER = "Pedometer";
    public static final String T_SKIN_TEMP = "Skin_Temperature";
    public static final String T_UV = "UV";

    private static final String TAG = "DataManagementService";


    public interface DataManagementFunctions {
        void placeHolder(String text);
    }

    private Map<Activity, DataManagementFunctions> clients = new ConcurrentHashMap<Activity, DataManagementFunctions>();

    public class LocalBinder extends Binder {

        // Registers a Activity to receive updates
        public void registerActivity(Activity activity, DataManagementFunctions callback) {
            clients.put(activity, callback);
        }

        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }
    }

    public DataManagementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    public static final String ACTION_WRITE_CSVS = "write csvs";
    public static final String ACTION_BACKUP = "BACKUP";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        switch (intent.getAction()) {
            case ACTION_WRITE_CSVS:
                String folderName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString(Preferences.CURRENT_DOCUMENT, "folder");
                try {
                    exportToCsv(folderName, getApplicationContext());
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                break;
            case ACTION_BACKUP:

                // Do a push replication
                try {
                    URL url = new URL(CouchBaseData.URL_STRING);
                    Replication push = new Replication(
                            CouchBaseData.getDatabase(getApplicationContext()),
                            url,
                            Replication.Direction.PUSH);




                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                break;
            default:
                Log.e(TAG, "Nonexistant action requested");
        }

        return START_NOT_STICKY;
    }

    public static void exportToCsv(String fName, Context c) throws CouchbaseLiteException {
        // Export all data to a csv

        if (ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("CBD", "permission granted");
        } else {
            Log.e("CBD", "permission denied");
        }

        String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/" + fName;
        File folder = new File(PATH);
        if (!folder.exists()) {
            boolean b = folder.mkdirs();
            Log.v("CBD", "");
        }

        // Get the current document and create a new one
//        Document d = CouchBaseData.getCurrentDocument(c);
//        CouchBaseData.resetCurrentDocument(c);
        SharedPreferences p = c.getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        String id = p.getString(Preferences.CURRENT_DOCUMENT, "");
        Document d = null;
        try {
            d = CouchBaseData.getCurrentDocument(c);

            Map<String, Object> properties = d.getUserProperties();
            for (String property :
                    properties.keySet()) {
                Object value = properties.get(property);

                if (value instanceof String) {
                    try {
                        JSONArray array = new JSONArray((String) value);

                        JSONObject o = (JSONObject) array.get(0);
                        StringBuilder header = new StringBuilder();
                        Iterator keys = o.keys();
                        List<String> jsonKeys = new LinkedList<>();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            header.append(key);
                            jsonKeys.add(key);
                            header.append(",");

                        }
                        header.replace(header.length() - 1, header.length(), "\n");
                        Log.v("CBD", header.toString());
                        String fileName = folder.toString() + "/" + property + ".csv";

                        File file = new File(fileName);

                        // If file does not exists, then create it
                        if (!file.exists()) {
                            try {
                                boolean fb = file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // Post data to the csv
                            FileWriter fw;
                            fw = new FileWriter(file.getPath(), true);
                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(file));
                            c.sendBroadcast(intent);
                            fw.append(header.toString());

                            // Loop through items in the JSONArray and append their fields
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = (JSONObject) array.get(i);
                                for (String key :
                                        jsonKeys) {
                                    fw.append(object.getString(key));
                                    if (key != jsonKeys.get(jsonKeys.size() - 1)) {
                                        fw.append(",");
                                    }
                                }
                                fw.append("\n");
                            }
                            fw.close();
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
