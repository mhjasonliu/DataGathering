package com.northwestern.habits.datagathering.database;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import com.northwestern.habits.datagathering.userinterface.UserActivity;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * The purpose of this service is to:
 * 1) manage connections and 2) fulfill streaming requests
 * <p/>
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
    public static final String FIRST_ENTRY = "First_Entry";
    public static final String TYPE = "Type";
    public static final String LAST_ENTRY = "Last_Entry";
    public static final String DATA = "data_series";
    public static final String DATA_KEYS = "Data_Keys";

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
//                testWriteCSV(getBaseContext());
//                String folderName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                        .getString(Preferences.CURRENT_DOCUMENT, "csvs");
//                try {
//                    exportToCsv(folderName, getApplicationContext());
//                } catch (CouchbaseLiteException e) {
//                    e.printStackTrace();
//                }
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
//                startOneShotRep();
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
        Log.e(TAG, "ERROR: startrepcalled");
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
                                        // noinspection ThrowableResultOfMethodCallIgnored
                                        Toast.makeText(getBaseContext(),
                                                "Error occurred while replicating " +
                                                        event.getError(), Toast.LENGTH_SHORT).show();
                                        // noinspection ThrowableResultOfMethodCallIgnored
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
                                        Document d = db.getDocument(id);
                                        // exportToCsv(d, getBaseContext());
                                        d.purge();
//                                    } catch (IOException e) {
                                        // Don't purge
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }

                        // Restart rep if needed
                        if (event.getTransition() != null &&
                                event.getTransition().

                                        getDestination()

                                        == ReplicationState.STOPPED)

                        {
                            try {
                                // Sleep to prevent overuse of the battery
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // Start a new rep if plugged in and charging
                            if (MyReceiver.isCharging(getBaseContext()) &&
                                    MyReceiver.isWifiConnected(getBaseContext())) {
//                                startOneShotRep();
                            } else {
//                                 Not starting new rep -> unknown status
                                Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                                i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                        UserActivity.DbUpdateReceiver.STATUS_UNKNOWN);
                                sendBroadcast(i);
                            }
                        }
                    }
                });
                isReplicating = true;

                // Write each document to csv
                if (result.getCount() > 0) {
                    Intent i = new Intent();
                    i.setAction(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                    i.setAction(UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                    sendBroadcast(i);
                } else {
                    Intent i = new Intent();
                    i.setAction(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                    i.setAction(UserActivity.DbUpdateReceiver.STATUS_SYNCED);
                    sendBroadcast(i);
                }
                while (result.hasNext()) {
                    Document d = result.next().getDocument();
//                    exportToCsv(d, getBaseContext());
                    d.purge();
                }

                // Wait for 15 seconds and update again
                Thread.sleep(30000);
//                push.start();


            } catch (CouchbaseLiteException | IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            isReplicating = false;
            if (MyReceiver.isCharging(getBaseContext()) && MyReceiver.isWifiConnected(getBaseContext())) {
                // Start over again
//                startOneShotRep();
            } else {
                Intent i = new Intent();
                i.setAction(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                i.setAction(UserActivity.DbUpdateReceiver.STATUS_UNKNOWN);
                sendBroadcast(i);
            }
        }
    }

//    public static void exportToCsv(Document document, Context c) throws IOException {
//        // Export all data to a csv
//
//        // Make csv name
//        Map<String, Object> docProps = document.getProperties();
//        String fName = (String) docProps.get(USER_ID);
//        fName += "_" + docProps.get(TYPE) + "_";
//        fName += docProps.get(FIRST_ENTRY);
//        fName += "_thru_";
//        fName += docProps.get(LAST_ENTRY) + ".csv";
//
//        if (ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            Log.v("CBD", "permission granted");
//        } else {
//            Log.e("CBD", "permission denied");
//        }
//
//        String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/";
//        File folder = new File(PATH);
//        if (!folder.exists()) {
//            Log.v(TAG, "directory " + folder.getPath() + " Succeeded " + folder.mkdirs());
//        }
//
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//            Log.e(TAG, "ASdf");
//        }
//
//        File csv = new File(PATH, fName);
//        if (!csv.exists()) {
//            try {
//                // Make the file
//                if (!csv.createNewFile()) {
//                    throw new IOException();
//                }
//                FileWriter csvWriter = new FileWriter(csv.getPath(), true);
//                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                intent.setData(Uri.fromFile(csv));
//                c.sendBroadcast(intent);
//
//
//                List<Map<String, Object>> dataSeries = (List<Map<String, Object>>) docProps.get(DATA);
//                List<String> properties = (List<String>) docProps.get(DATA_KEYS);
//
//                for (int i = 0; i < properties.size(); i++) {
//                    csvWriter.append(properties.get(i));
//                    if (i == properties.size() - 1) {
//                        csvWriter.append("\n");
//                    } else {
//                        csvWriter.append(",");
//                    }
//                }
//
//                // Write the file
//                for (Map<String, Object> dataPoint :
//                        dataSeries) {
//
//                    for (int i = 0; i < properties.size(); i++) {
//                        Object datum = dataPoint.get(properties.get(i));
//
//                        if (datum instanceof String) {
//                            csvWriter.append(datum.toString());
//                        } else if (datum instanceof Double) {
//                            csvWriter.append(Double.toString((Double) datum));
//                        } else if (datum instanceof Integer) {
//                            csvWriter.append(Integer.toString((Integer) datum));
//                        } else if (datum instanceof Long) {
//                            csvWriter.append(Long.toString((Long) datum));
//                        } else {
//                            Log.e(TAG, "Unhandled case");
//                            csvWriter.append(datum.toString());
//                        }
//
//
//                        if (i == properties.size() - 1) {
//                            csvWriter.append("\n");
//                        } else {
//                            csvWriter.append(",");
//                        }
//                    }
//                }
//                csvWriter.flush();
//                csvWriter.close();
//
//            } catch (IOException | NullPointerException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
