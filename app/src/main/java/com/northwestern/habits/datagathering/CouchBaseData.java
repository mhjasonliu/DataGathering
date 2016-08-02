package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 6/22/2016.
 */
public class CouchBaseData {

    private static Replication replication = null;
    public static final String URL_STRING = "http://107.170.25.202:4984/db/";

    public static final String DB_NAME = "data_gathering_db";

    private static Database database;
    private static Document currentDocument;
    private static Manager currentManager;

    public static Database getDatabase(Context c) throws CouchbaseLiteException, IOException {
        c = c.getApplicationContext();
        if (database == null) {
            database = getManager(c).getDatabase(DB_NAME);
            database.setMaxRevTreeDepth(1);
        }
        return database;
    }

    public static Document getCurrentDocument(Context c) throws CouchbaseLiteException, IOException {
        c = c.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String id = prefs.getString(Preferences.CURRENT_DOCUMENT, "");
        if (id.equals("")) {
            try {
                currentDocument = getDatabase(c).getDocument(
                        BluetoothConnectionLayer.getAdapter().getAddress() + "_" + Calendar.getInstance().getTime());
            } catch (IOException e) {
                e.printStackTrace();
            }
            SharedPreferences.Editor e = prefs.edit();
            e.putString(Preferences.CURRENT_DOCUMENT, currentDocument.getId());
            e.apply();

            // Broadcast the new id across processes
            Intent i = new Intent(DocIdBroadcastReceiver.ACTION_BROADCAST_CHANGE_ID);
            i.putExtra(DocIdBroadcastReceiver.NEW_ID_EXTRA, currentDocument.getId());

            c.sendBroadcast(i);

            Log.v("CBD", "Created new document " + currentDocument.getId());
            Map<String, Object> properties = new HashMap<>();
            properties.put("Time_Created", Calendar.getInstance().getTime());
            properties.put("Version", 1);

            currentDocument.putProperties(properties);
            return currentDocument;
        } else {
            if (currentDocument == null) {
                database = getDatabase(c);
            } else {
                Log.v("CBD", "Accessed old document " + id);
            }
            return database.getDocument(id);
        }
    }

    private static Manager getManager(Context c) throws IOException {
        if (currentManager == null) {
            AndroidContext ac = new AndroidContext(c);
            currentManager = new Manager(ac, Manager.DEFAULT_OPTIONS);
        }
        return currentManager;
    }

    public static Replication getReplicationInstance(Context c) throws MalformedURLException {
        if (replication == null) {
            URL url = new URL(URL_STRING);
            try {
                replication = new Replication(getDatabase(c), url, Replication.Direction.PULL);
                List<String> list = new LinkedList<>();
                list.add("Test");//getCurrentDocument(c).getId());
                replication.setDocIds(list);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return replication;
    }


}
