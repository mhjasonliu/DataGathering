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
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.ReplicationState;
import com.northwestern.habits.datagathering.userinterface.UserActivity;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private Replication push;

    private void startOneShotRep() {
        // Only start a replication if another is not running
        if (!isReplicating) {
            isReplicating = true;

            try {
                // Get all the documents from the database
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
                push = new Replication(db,
                        url,
                        Replication.Direction.PUSH);

                push.setContinuous(false);
                push.setDocIds(ids);
                push.addChangeListener(changeListener);
                push.start();

            } catch (CouchbaseLiteException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void restartOneShot() {
        isReplicating = false;
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(30));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startOneShotRep();
    }

    Replication.ChangeListener changeListener = new Replication.ChangeListener() {
        @Override
        public void changed(Replication.ChangeEvent event) {
            Log.v(TAG, event.toString());
            boolean didCompleteAll = event.getChangeCount() == event.getCompletedChangeCount();
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            boolean errorDidOccur = event.getError() != null;
            boolean changesAreZero = event.getChangeCount() == 0;
            boolean isTransitioningToStopped = false;
            try {
                isTransitioningToStopped =
                        event.getTransition().getDestination() == ReplicationState.STOPPED;
            } catch (NullPointerException ignored) {
            }


            if (errorDidOccur) {
                // Broadcast error
                Intent errorIntent = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                errorIntent.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                        UserActivity.DbUpdateReceiver.STATUS_DB_ERROR);
                sendBroadcast(errorIntent);
                Log.e(TAG, "Error reported");
                if (isTransitioningToStopped) {
                    // Restart?
                    restartOneShot();
                } else {
                    // Stop replication
                    push.stop();
                }


            } else if (isTransitioningToStopped) {
                // STOPPING WITHOUT ERROR
                Log.v(TAG, "Stopped");
                if (didCompleteAll && !changesAreZero) {
                    // Broadcast synced
                    Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                    i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                            UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                    sendBroadcast(i);
                    Log.v(TAG, "Broadcasted syncing after successful sync");
                    // Delete old documents
                    List<String> pushed = push.getDocIds();
                    try {
                        Database db = CouchBaseData.getDatabase(getBaseContext());
                        for (String id :
                                pushed) {
                            // Purge the doc
                            db.getDocument(id).purge();
                        }
                        Log.v(TAG, "Deleted " + Integer.toString(pushed.size()) + " documents");
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (changesAreZero) {
                    // Zero changes and stopped -> synced
                    Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                    i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                            UserActivity.DbUpdateReceiver.STATUS_SYNCED);
                    sendBroadcast(i);
                    Log.v(TAG, "Broadcasted synced");

                } else {
                        // Broadcast syncing
                        Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                        i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                                UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                        sendBroadcast(i);
                        Log.v(TAG, "Broadcasted syncing by default 1");
                        Log.v(TAG, "Did complete all is " + didCompleteAll);
                        Log.v(TAG, "Changes are zero is " + changesAreZero);
                }

                // Restart?
                restartOneShot();


            } else if (event.getChangeCount() > 0) {
                // NOT STOPPING && NO ERROR
                // Broadcast syncing
                Intent i = new Intent(UserActivity.DbUpdateReceiver.ACTION_DB_STATUS);
                i.putExtra(UserActivity.DbUpdateReceiver.STATUS_EXTRA,
                        UserActivity.DbUpdateReceiver.STATUS_SYNCING);
                sendBroadcast(i);
                Log.v(TAG, "Broadcasted syncing by default 2");
            }

        }
    };
}
