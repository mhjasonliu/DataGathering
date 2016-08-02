package com.northwestern.habits.datagathering;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
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
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
public class DataManagementService extends Service implements DocIdBroadcastReceiver {

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

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new DocIDRecieveRunnable().run(context, intent);
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
        registerReceiver(mReceiver, _filter);
    }

    @Override
    public void unregisterReceiver() {
        unregisterReceiver(mReceiver);
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

    public DataManagementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    public static final String ACTION_WRITE_CSVS = "write csvs";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_STOP_BACKUP = "stop_backup";
    public static final String CONTINUOUS_EXTRA = "continuous";

    private Handler mHandler;
    private Replication mReplication;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        Replication push = null;
        try {
            push = getReplicationInstance();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        switch (intent.getAction()) {
            case ACTION_WRITE_CSVS:
//                String folderName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                        .getString(Preferences.CURRENT_DOCUMENT, "folder");
//                try {
//                    exportToCsv(folderName, getApplicationContext());
//                } catch (CouchbaseLiteException e) {
//                    e.printStackTrace();
//                }
                break;
            case ACTION_BACKUP:
                startOneShotRep();
                break;
            case ACTION_STOP_BACKUP:
                // If the backup is not null, is running, and is continuous, stop it
                // If it is not continuous, forced replication was performed.
//                if (push != null && push.isRunning() && push.isContinuous()) {
//                    // Alert user if the backup is not finished
//                    final String toastText;
//                    if (push.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
//                        toastText = "Database backup prematurely stopped: " +
//                                push.getCompletedChangesCount() + " out of " + Integer.toString(push.getChangesCount())
//                                + " changes backed up.";
//                    } else {
//                        toastText = "Database backup stopped";
//                    }
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getBaseContext(), toastText,
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                    Log.v(TAG, "Stopping continuous backup");
//                    push.stop();
//                }

                break;
            default:
                Log.e(TAG, "Nonexistant action requested");
        }

        registerReceiver();
        return START_NOT_STICKY;
    }


    private boolean isReplicating = false;

    private void startOneShotRep() {

        if (!isReplicating) {
            try {
                final Database db = CouchBaseData.getDatabase(this);
                Query q = db.createAllDocumentsQuery();
                q.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);

                QueryEnumerator result = q.run();
                // Pack the docIDs into a list
                final List<String> ids = new LinkedList<>();
                for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                    ids.add(it.next().getDocumentId());
                }

                // Do a one-shot replication
                URL url = new URL(CouchBaseData.URL_STRING);
                Replication push = new Replication(db,
                        url,
                        Replication.Direction.PUSH);

                push.setContinuous(false);
                push.setDocIds(ids);
                push.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(final Replication.ChangeEvent event) {
                        Log.v(TAG, event.toString());
                        boolean didCompleteAll = event.getChangeCount() == event.getCompletedChangeCount();
                        boolean errorDidOccur = event.getError() != null;
                        boolean changesNotZero = event.getCompletedChangeCount() != 0;
                        boolean isTransitioningToStopped = false;
                        try {
                            isTransitioningToStopped =
                                    event.getTransition().getDestination() == ReplicationState.STOPPED;
                        } catch (NullPointerException e) {}

                        if (isTransitioningToStopped) {
                            isReplicating = false;
                        }


                        if (errorDidOccur) {
                            // TODO handle error
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getBaseContext(),
                                            "Error occurred while replicating " +
                                                    event.getError(), Toast.LENGTH_SHORT).show();
                                    event.getError().printStackTrace();
                                }
                            });
                        } else if (isTransitioningToStopped) {

                            if (didCompleteAll && changesNotZero) {
                                Log.v(TAG, "Successful backup of " + event.getCompletedChangeCount() + " docs.");
                                Log.v(TAG, "Deleting docs");
                                // Delete old documents
                                for (String id : ids) {
                                    try {
                                        db.getDocument(id).purge();
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }

                            // Start a new rep if plugged in and charging
                            if (isCharging(getBaseContext()) && isWifiConnected(getBaseContext())) {
                                try {
                                    Thread.sleep(30000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                startOneShotRep();
                            }
                        }
                    }
                });
                push.start();


            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    private void curlDbOnline() throws MalformedURLException {
        String address = CouchBaseData.URL_STRING;
        address = "http://107.170.25.202:4984/db/" + "_online";

        URL url = new URL(address);

        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            OutputStream os = con.getOutputStream();
            Log.v(TAG, "Aasdf status code: " + Integer.toString(con.getResponseCode()));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Replication getReplicationInstance() throws IOException, CouchbaseLiteException {
        URL url = new URL(CouchBaseData.URL_STRING);
        if (mReplication == null) {
            mReplication = new Replication(
                    CouchBaseData.getDatabase(getApplicationContext()),
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
                                                    Toast.makeText(getBaseContext(),
                                                            "Remote database is offline",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                    break;
                                case 404:
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getBaseContext(),
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
                                    Toast.makeText(getBaseContext(), "Could not connect to " +
                                                    "remote database, please connect to the internet.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // No error and transition from running to stopped -> success
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getBaseContext(),
                                            "Successfully backed up database replications " +
                                                    Integer.toString(event.getCompletedChangeCount()) +
                                                    " out of " + Integer.toString(event.getChangeCount()) +
                                                    "\nDocument: " +
                                                    PreferenceManager.getDefaultSharedPreferences(
                                                            getBaseContext()).getString(Preferences.CURRENT_DOCUMENT, "_"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }


                        // If wifi connected and charging, continue
                        if (!mReplication.isContinuous()
                                && isWifiConnected(getBaseContext())
                                && isCharging(getBaseContext())) {

                            if (event.getCompletedChangeCount() > 0
                                    && event.getCompletedChangeCount()
                                    == event.getCompletedChangeCount()) {
                                // Full replication completed successfully...
                                // Delete the old document and start a new one
                                try {
                                    CouchBaseData.getCurrentDocument(getApplicationContext()).delete();
                                    sendBroadcast(new Intent(ACTION_BROADCAST_CHANGE_ID));
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getBaseContext(),
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
            list.add(CouchBaseData.getCurrentDocument(getBaseContext()).getId());
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


    @Override
    public void onDestroy() {
        // Unregister broadcast receiver
        unregisterReceiver();
        super.onDestroy();
    }
}
