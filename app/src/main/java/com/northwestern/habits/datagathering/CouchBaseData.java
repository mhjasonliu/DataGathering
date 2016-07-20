package com.northwestern.habits.datagathering;

import android.content.Context;
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

    public static Database getDatabase(Context c) throws CouchbaseLiteException, IOException {
        Database database;
        database = getManager(c).getDatabase(DB_NAME);
        database.setMaxRevTreeDepth(1);
        return database;
    }

//    public static void setCurrentDocument(Context c, String id) {
//        try {
//            currentDocument = getDatabaseInstance(c).getDocument(id);
//        } catch (CouchbaseLiteException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void resetCurrentDocument(Context c) {
//        currentDocument = null;
//        c.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE).edit().remove(Preferences.CURRENT_DOCUMENT).apply();
//    }

    public static Document getCurrentDocument(Context c) throws CouchbaseLiteException, IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String id = prefs.getString(Preferences.CURRENT_DOCUMENT, "");
        if (id.equals("")) {
            Document currentDocument = null;
            try {
                currentDocument = getDatabase(c).createDocument();
            } catch (IOException e) {
                e.printStackTrace();
            }
            SharedPreferences.Editor e = prefs.edit();
            e.putString(Preferences.CURRENT_DOCUMENT, currentDocument.getId());
            e.apply();
            Log.v("CBD", "Created new document " + currentDocument.getId());
            Map<String, Object> properties = new HashMap<>();
            properties.put("Initialized", true);
            currentDocument.putProperties(properties);
            return currentDocument;
        } else {
            Database database;
            AndroidContext ac = new AndroidContext(c);
            Manager manager = new Manager(ac, Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase(DB_NAME);

            Log.v("CBD", "Accessed old document " + id);
            return database.getDocument(id);
        }
    }

    public static Manager getManager(Context c) throws IOException {
        AndroidContext ac = new AndroidContext(c);
        return new Manager(ac, Manager.DEFAULT_OPTIONS);
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
