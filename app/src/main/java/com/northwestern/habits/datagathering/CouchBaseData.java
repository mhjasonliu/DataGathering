package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 6/22/2016.
 */
public class CouchBaseData {

    private static Database database = null;
    private static Document currentDocument = null;
    private static Manager manager = null;
    private static Replication replication = null;
    private static final String URL_STRING = "http://107.170.25.202:4984/db/";

    public static final String DB_NAME = "data_gathering_db";

    public static Database getDatabaseInstance(Context c) throws CouchbaseLiteException {
        if ((database == null)) {
            try {
                database = getManagerInstance(c).getDatabase(DB_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }
            database.setMaxRevTreeDepth(1);
        }
        return database;
    }

    public static void setCurrentDocument(Context c, String id) {
        try {
            currentDocument = getDatabaseInstance(c).getDocument(id);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public static Document getCurrentDocument(Context c) {
        if (currentDocument == null) {
            SharedPreferences prefs = c.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
            String id = prefs.getString(Preferences.CURRENT_DOCUMENT, "");
            if (id == "") {
                try {
                    currentDocument = getDatabaseInstance(c).createDocument();
                    SharedPreferences.Editor e = prefs.edit();
                    e.putString(Preferences.CURRENT_DOCUMENT, currentDocument.getId());
                    e.apply();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                Map<String, Object> properties = new HashMap<>();
                properties.put("Initialized", true);
                try {
                    currentDocument.putProperties(properties);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    currentDocument = getDatabaseInstance(c).getDocument(id);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        }
        return currentDocument;
    }

    public static Manager getManagerInstance(Context c) throws IOException {
        if (manager == null) {
            AndroidContext ac = new AndroidContext(c);
            manager = new Manager(ac, Manager.DEFAULT_OPTIONS);
        }
        return manager;
    }

    public static Replication getReplicationInstance(Context c) throws MalformedURLException {
        if (replication == null) {
            URL url = new URL(URL_STRING);
            try {
                replication = new Replication(getDatabaseInstance(c), url, Replication.Direction.PULL);
                List<String> list = new LinkedList<>();
                list.add("Test");//getCurrentDocument(c).getId());
                replication.setDocIds(list);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return replication;
    }


    public static void exportToCsv(String fName, Context c) {
        // Export all data to a csv

        // Get the current document and create a new one
        Document d = getCurrentDocument(c);
//        c.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE).edit().remove(Preferences.CURRENT_DOCUMENT).apply();
//        currentDocument = null;

        Map<String, Object> properties = d.getUserProperties();
        for (String property :
                properties.keySet()) {
            Object value = properties.get(property);
//            Log.v("CBD", property + ": " + value);

            if (value instanceof String) {
                try {
                    JSONArray array = new JSONArray((String) value);

                    JSONObject o = (JSONObject) array.get(0);
                    StringBuilder header = new StringBuilder();
                    Iterator keys = o.keys();
                    while (keys.hasNext()) {
                        header.append(keys.next());
                        header.append(",");
                    }
                    header.replace(header.length()-1, header.length(), "\n");
                    Log.v("CBD", header.toString());


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
