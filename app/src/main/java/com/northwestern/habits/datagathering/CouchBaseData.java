package com.northwestern.habits.datagathering;

import android.content.Context;

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
            try {
                currentDocument = getDatabaseInstance(c).createDocument();
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
                replication = new Replication(getDatabaseInstance(c), url, Replication.Direction.PUSH);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return replication;
    }
}
