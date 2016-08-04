package com.northwestern.habits.datagathering.Database;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.ReplicationState;
import com.northwestern.habits.datagathering.MyReceiver;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.userinterface.UserActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The purpose of this service is to:
 * 1) manage connections and 2) fulfill streaming requests
 * <p>
 * 1) Manage connections
 * Accomplished by maintaining a
 */
public class DataManagementService extends Service {

    /**
     * Constants of data types for storing in the database
     */
    public static final String T_ACCEL = "Accelerometer";
    public static final String T_Altimeter = "Altimeter";
    public static final String T_Ambient = "Ambient_Light";
    public static final String T_Barometer = "Barometer";
    public static final String T_Calories = "Calories";
    public static final String T_Contact = "Contact_State";
    public static final String T_Distance = "Distance";
    public static final String T_GSR = "GSR";
    public static final String T_Gyroscope = "Gyroscope";
    public static final String T_Heart_Rate = "Heart_Rate";
    public static final String T_PEDOMETER = "Pedometer";
    public static final String T_SKIN_TEMP = "Skin_Temperature";
    public static final String T_UV = "UV";

    public static final String T_DEVICE = "Device_Type";
    public static final String DEVICE_MAC = "Device_Mac";
    public static final String USER_ID = "User_ID";

    public static final int L_NOTHING = 0;
    public static final int L_EATING = 1;
    public static final int L_DRINKING = 2;
    public static final int L_SWALLOW = 3;


    private static final String TAG = "DataManagementService";

    /**
     * Constructor
     */
    public DataManagementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Intent actions for starting the service
     */
    public static final String ACTION_WRITE_CSVS = "write csvs";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_STOP_BACKUP = "stop_backup";


    private Handler mHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        switch (intent.getAction()) {
            case ACTION_WRITE_CSVS:
                String folderName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString(Preferences.CURRENT_DOCUMENT, "csvs");
                try {
                    exportToCsv(folderName, getApplicationContext());
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                break;
            case ACTION_BACKUP:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Starting database backup",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                        UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                sendBroadcast(i);
                startOneShotRep();
                break;
            case ACTION_STOP_BACKUP:
                try {
                    Database db = CouchBaseData.getDatabase(getBaseContext());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Stopping all database replications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    Intent broadcastIntent = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                    broadcastIntent.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                            UserActivity.DbUpdateReceiver.STATUS_UNKNOWN);
                    sendBroadcast(broadcastIntent);
                    for (Replication r :
                            db.getActiveReplications()) {
                        r.stop();
                    }
                } catch (CouchbaseLiteException | IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Log.e(TAG, "Non-existant action requested " + intent.getAction());
        }

        return START_NOT_STICKY;
    }


    private boolean isReplicating = false;

    private void startOneShotRep() {
        // Only start a replication if another is not running
        if (!isReplicating) {
            try {
                final Database db = CouchBaseData.getDatabase(this);
                Query q = db.createAllDocumentsQuery();
                q.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);

                QueryEnumerator result = q.run();
                // Pack the docIDs into a list
                final List<String> ids = new LinkedList<>();
                while (result.hasNext()) {
                    ids.add(result.next().getDocumentId());
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
                        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                        boolean errorDidOccur = event.getError() != null;
                        boolean changesNotZero = event.getCompletedChangeCount() != 0;
                        boolean isTransitioningToStopping = false;
                        try {
                            isTransitioningToStopping =
                                    event.getTransition().getDestination() == ReplicationState.STOPPING;
                        } catch (NullPointerException ignored) {
                        }


                        // For broadcasts
                        if (event.getTransition() != null) {
                            switch (event.getTransition().getDestination()) {
                                case RUNNING:
                                    // Rep starting, broadcast syncing
                                    // No error and not stopping... broadcast syncing
                                    Intent syncingIntent = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                                    syncingIntent.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                            UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                                    sendBroadcast(syncingIntent);
                                    break;
                                case STOPPING:
                                    if (errorDidOccur) {
                                        // Broadcast error occurring
                                        Intent errorIntent = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                                        errorIntent.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                                UserActivity.DbUpdateReceiver.STATUS_DB_ERROR);
                                        sendBroadcast(errorIntent);
                                        Log.e(TAG, "Error reported");
                                    } else if (changesNotZero) {
                                        // Data was sent, still syncing so do nothing
                                    } else {
                                        // Data was not sent and no error occurred, we are up to date
                                        Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                                        i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                                UserActivity.DbUpdateReceiver.STATUS_SYNCED);
                                        sendBroadcast(i);
                                        Log.e(TAG, "Complete sync reported");
                                    }
                                    break;
                            }
                        }


                        // Allow another replication to be created
                        if (isTransitioningToStopping) {
                            isReplicating = false;
                        }

                        if (isTransitioningToStopping) {

                            if (errorDidOccur) {
                                // Broadcast to user activity

                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //noinspection ThrowableResultOfMethodCallIgnored
                                        Toast.makeText(getBaseContext(),
                                                "Error occurred while replicating " +
                                                        event.getError(), Toast.LENGTH_SHORT).show();
                                        //noinspection ThrowableResultOfMethodCallIgnored
                                        event.getError().printStackTrace();
                                    }
                                });
                            }

                            if (!changesNotZero) {
                                // Zero changes, sync complete
                            }

                            // Replication completed
                            if (didCompleteAll && changesNotZero) {
                                // Successful IFF complete and a replication went through
                                Log.v(TAG, "Successful backup of " + event.getCompletedChangeCount()
                                        + " docs.");
                                Log.v(TAG, "Deleting docs");

                                // Delete old documents
                                for (String id : ids) {
                                    try {
                                        // Purge so deletion does not replicate to server
                                        db.getDocument(id).purge();
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            try {
                                // Sleep to prevent overuse of the battery
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            startOneShotRep();
                            // Start a new rep if plugged in and charging
                            if (MyReceiver.isCharging(getBaseContext()) &&
                                    MyReceiver.isWifiConnected(getBaseContext())) {
                            } else {
                                // Not starting new rep -> unknown status
                                Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                                i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                        UserActivity.DbUpdateReceiver.STATUS_SYNCED);
                                sendBroadcast(i);
                            }
                        }
                    }
                });
                isReplicating = true;
                push.start();


            } catch (CouchbaseLiteException | IOException e) {
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
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
            Log.v("CBD", "");
        }

        // Get the current document and create a new one
//        Document d = CouchBaseData.getCurrentDocument(c);
//        CouchBaseData.resetCurrentDocument(c);
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        p.getString(Preferences.CURRENT_DOCUMENT, "");
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
                                //noinspection ResultOfMethodCallIgnored
                                file.createNewFile();
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
                                    if (!Objects.equals(key, jsonKeys.get(jsonKeys.size() - 1))) {
                                        fw.append(",");
                                    }
                                }
                                fw.append("\n");
                            }
                            fw.close();
                        }


                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}