package com.northwestern.habits.datagathering.database;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.northwestern.habits.datagathering.BluetoothConnectionLayer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by William on 6/22/2016
 */
public class CouchBaseData {

    private static Replication replication = null;
    public static final String URL_STRING = "http://107.170.25.202:4984/db/";

    public static final String DB_NAME_BASE = "data_gathering_db";

    private static final String TAG = "CBD";
    private static Database database;
    private static int docCount;
    private static final int DOC_LIMIT = 1000;
    private static Document currentDocument;
    private static Manager currentManager;

    public static Database getDatabase(Context c) throws CouchbaseLiteException, IOException {
        c = c.getApplicationContext();
        Calendar cal = Calendar.getInstance();
        String name = DB_NAME_BASE + Integer.toString(cal.get(Calendar.MONTH)+1) +
                cal.get(Calendar.DAY_OF_WEEK) +cal.get(Calendar.HOUR_OF_DAY);
        if (database == null || !Objects.equals(database.getName(), name)) {
            Log.d(TAG, "Creating new database");
            database = getManager(c).getDatabase(name);
            docCount = database.getDocumentCount();
        }
        docCount = database.getDocumentCount();
        Log.d(TAG, Integer.toString(docCount) + " documents in database");
        return database;
    }


    public static Database getOldestDatabase(Context c) throws IOException, CouchbaseLiteException {
        c = c.getApplicationContext();
        Manager m = getManager(c);
        List<String> names = new ArrayList<>(m.getAllDatabaseNames());
        if (names.size() == 0) return null;
        Collections.sort(names);
        Log.d(TAG, "List of names: " + names);
        Database db = null;
        int i = 0;
        while (db == null && i < names.size()) {
            db = m.getExistingDatabase(names.get(i));
            if (db != null && db.getDocumentCount() == 0) {
                Log.v(TAG, "Deleting database " + db.getName());
                db.delete();
                db = null;
            } else {
                Log.v(TAG, "Database " + db.getName() + " has " + db.getDocumentCount() + " docs");
            }
            i++;
        }
        Log.v(TAG, "Using database " + db.getName());
        return db;
    }

    public static Document getNewDocument(Context c) throws CouchbaseLiteException, IOException {

        try {
            currentDocument = getDatabase(c).getDocument(
                    BluetoothConnectionLayer.getAdapter().getAddress() + "_" + Calendar.getInstance().getTimeInMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "Created new document " + currentDocument.getId());
        Map<String, Object> properties = new HashMap<>();
        properties.put("Time_Created", Long.toString(Calendar.getInstance().getTimeInMillis()));

        currentDocument.putProperties(properties);
        return currentDocument;

    }

    public static Document getDocument(Calendar date, String type, String userId, Context context)
            throws CouchbaseLiteException, IOException {
        // ID will be formed as follows: UserID_Type_MM-DD-YYYY_HHHH_MM
        StringBuilder docID = new StringBuilder();
        docID.append(userId);
        docID.append("_");
        docID.append(type);
        docID.append("_");
        docID.append(date.get(Calendar.MONTH));
        docID.append("-");
        docID.append(date.get(Calendar.DAY_OF_MONTH) + 1);
        docID.append("-");
        docID.append(date.get(Calendar.YEAR));
        docID.append("_");
        int hour = date.get(Calendar.HOUR_OF_DAY);
        if (hour < 10)
            docID.append("0");
        docID.append(hour);
        docID.append("00");
        docID.append("_");
        docID.append(date.get(Calendar.MINUTE));

        Document d = getDatabase(context).getDocument(docID.toString());

        Map<String, Object> properties = d.getProperties();

        if (properties == null || !properties.containsKey("Version")) {
            Log.v(TAG, "Setting metadata properties");
            properties = new HashMap<>();
            // New document
            properties.put(DataManagementService.TYPE, type);
            properties.put("Hour", hour);
            properties.put("User", userId);

            properties.put(DataManagementService.FIRST_ENTRY, date.getTimeInMillis());
            date.set(Calendar.SECOND, 59);
            properties.put(DataManagementService.LAST_ENTRY, date.getTimeInMillis());
            properties.put(DataManagementService.USER_ID, userId);
            properties.put(DataManagementService.DATA, new LinkedList<Map>());
            properties.put("Version", "3.0.0");
            d.putProperties(properties);
        }

        return d;
    }

    @Deprecated
    public static Document getCurrentDocument(Context c) throws CouchbaseLiteException, IOException {
        throw new UnsupportedOperationException("This should be a call to getNewDocument as of the " +
                "new replication method");
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
