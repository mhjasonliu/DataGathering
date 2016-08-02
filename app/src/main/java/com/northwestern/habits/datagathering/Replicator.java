package com.northwestern.habits.datagathering;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.ReplicationState;
import com.couchbase.lite.replicator.ReplicationStateTransition;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by William on 8/1/2016.
 */
public class Replicator implements DocIdBroadcastReceiver {

    private static final String TAG = "Replicator";
    private Context mContext;
    private IntentFilter repFilter = new IntentFilter(ACTION_BACKUP);

    BroadcastReceiver backupCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getStringExtra(TYPE_EXTRA)) {
                case CSV_EXTRA:
                    String folderName = PreferenceManager.getDefaultSharedPreferences(mContext)
                            .getString(Preferences.CURRENT_DOCUMENT, "folder");
                    try {
                        exportToCsv(folderName, mContext);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }
                    break;
                case START_EXTRA:

                    // Do a push replication
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    List<String> docs = new ArrayList<>();
                    docs.add(prefs.getString(Preferences.CURRENT_DOCUMENT, ""));
                    assert mReplication != null;
                    mReplication.setDocIds(docs);

                    // Continuous request
                    if (mReplication.isRunning()) {
                        // Push already running, if continuous, leave it, if one-shot, leave it
                        // aka do nothing
                        break;
                    } else {
                        // Push not running, no need to worry about one-shot, just start it
                        mReplication.setContinuous(true);
                        Log.v(TAG, "Starting continuous replication");
                        mReplication.start();
                    }
                    break;
                case STOP_EXTRA:
                    // If the backup is not null, is running, and is continuous, stop it
                    // If it is not continuous, forced replication was performed.
                    if (mReplication != null && mReplication.isRunning() && mReplication.isContinuous()) {
                        // Alert user if the backup is not finished
                        final String toastText;
                        if (mReplication.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                            toastText = "Database backup prematurely stopped: " +
                                    mReplication.getCompletedChangesCount() + " out of " + Integer.toString(mReplication.getChangesCount())
                                    + " changes backed up.";
                        } else {
                            toastText = "Database backup stopped";
                        }
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, toastText,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.v(TAG, "Stopping continuous backup");
                        mReplication.stop();
                    }

                    break;
                default:
                    Log.e(TAG, "Nonexistant action requested");
            }
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new DocIdBroadcastReceiver.DocIDRecieveRunnable().run(context, intent);
            // Update replication to also match the new document
            try {
                getReplicationInstance();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    public void registerReceiver() {
        mContext.registerReceiver(mReceiver, _filter);
    }

    @Override
    public void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }


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

    public static final String ACTION_BACKUP = "BACKUP";
    public static final String TYPE_EXTRA = "backup type";
    public static final String START_EXTRA = "start";
    public static final String STOP_EXTRA = "stop";
    public static final String CSV_EXTRA = "csv";


    private Handler mHandler;
    private Replication mReplication;

    public Replicator(Context c) {
        mContext = c;
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        try {
            mReplication = getReplicationInstance();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }


        mContext.registerReceiver(backupCastReceiver, repFilter);
        registerReceiver();
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
        SharedPreferences p = c.getSharedPreferences(Preferences.NAME, c.MODE_PRIVATE);
        String id = p.getString(Preferences.CURRENT_DOCUMENT, "");
        Document d;
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

    public Replication getReplicationInstance() throws IOException, CouchbaseLiteException {
        URL url = new URL(CouchBaseData.URL_STRING);
        if (mReplication == null) {
            mReplication = new Replication(
                    CouchBaseData.getDatabase(mContext),
                    url,
                    Replication.Direction.PUSH);

            mReplication.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(final Replication.ChangeEvent event) {
                    Log.v(TAG, event.toString());
                    Log.v(TAG, "Listening for document " + mReplication.getDocIds().toString());
                    ReplicationStateTransition t = event.getTransition();

                    if (t != null
                            && t.getSource() == ReplicationState.STOPPING
                            && t.getDestination() == ReplicationState.STOPPED) {

                        if (t.getDestination() == ReplicationState.RUNNING) {
                            try {
                                Map<String, Object> properties = new HashMap<>();
                                properties.put("Time_Updated", Calendar.getInstance().getTime());
                                CouchBaseData.getCurrentDocument(mContext).putProperties(properties);

                            } catch (CouchbaseLiteException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Throwable error = mReplication.getLastError();
                        if (error != null && error instanceof HttpResponseException) {
                            switch (((HttpResponseException) error).getStatusCode()) {
                                case 503:
                                    Log.e(TAG, "Service unavailable");
                                    error.printStackTrace();
                                    mHandler.post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(mContext,
                                                            "Remote database is offline",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                    break;
                                case 404:
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mContext,
                                                    "Could not find the server",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    break;
                                default:
                                    Log.e(TAG, "Unhandled status code received: " +
                                            ((HttpResponseException) error).getStatusCode());
                            }
                        } else if (error != null && error instanceof HttpHostConnectException) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "Could not connect to " +
                                                    "remote database, please connect to the internet.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // No error and transition from running to stopped -> success
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext,
                                            "Successfully backed up database replications " +
                                                    Integer.toString(event.getCompletedChangeCount()) +
                                                    " out of " + Integer.toString(event.getChangeCount()) +
                                                    "\nDocument: " +
                                                    PreferenceManager.getDefaultSharedPreferences(
                                                            mContext).getString(Preferences.CURRENT_DOCUMENT, "_"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }


                        // If wifi connected and charging, continue
                        if (!mReplication.isContinuous()
                                && isWifiConnected(mContext)
                                && isCharging(mContext)) {

                            if (event.getCompletedChangeCount() > 0
                                    && event.getCompletedChangeCount()
                                    == event.getCompletedChangeCount()) {
                                // Full replication completed successfully...
                                // Delete the old document and start a new one
                                try {
                                    CouchBaseData.getCurrentDocument(mContext).delete();
                                    mContext.sendBroadcast(new Intent(ACTION_BROADCAST_CHANGE_ID));
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mContext,
                                                    "Deleted old database after backing it up",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (CouchbaseLiteException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            Log.v(TAG, "Restarting replication as continuous (from one-shot)");
                            mReplication.setContinuous(true);
                            mReplication.restart();
                        }
                    }
                }
            });
        } else {
            // mReplication is not null, make sure it is listening for the correct document
            List<String> list = new LinkedList<>();
            list.add(CouchBaseData.getCurrentDocument(mContext).getId());
            mReplication.setDocIds(list);
        }
        return mReplication;
    }


    private boolean isWifiConnected(Context c) {
        SupplicantState supState;
        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        supState = wifiInfo.getSupplicantState();
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
                && supState == SupplicantState.COMPLETED;
    }

    boolean isCharging(Context context) {
        // Check for charging
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }


}
