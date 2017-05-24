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
import com.northwestern.habits.datagathering.webapi.WebAPIManager;

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
    public static final String URL_STRING = WebAPIManager.URL_STRING_CB;

    public static final String DB_NAME_BASE = "data_gathering_db";
    public static final String LABEL_DB_NAME = "label_db";

    private static final String TAG = "CBD";
    private static Database database;
    private static Document currentDocument;
    private static Manager currentManager;

    public static Database getDatabase(Context c) throws CouchbaseLiteException, IOException {
        c = c.getApplicationContext();
        Calendar cal = Calendar.getInstance();
        String name = DB_NAME_BASE + Integer.toString(cal.get(Calendar.MONTH)+1) +
                cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.HOUR_OF_DAY);
        int docCount;
        if (database == null || !Objects.equals(database.getName(), name)) {
            Log.d(TAG, "Creating new database");
            database = getManager(c).getDatabase(name);
            docCount = database.getDocumentCount();
        }
        docCount = database.getDocumentCount();
        Log.d(TAG, Integer.toString(docCount) + " documents in database");
        return database;
    }

    public static void compactDatabases(Context c) {
        try {
            Database d = getOldestDatabase(c);
            if (d != null) d.compact();
        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Document getLabelDocument(String userID, Context context) throws CouchbaseLiteException, IOException {
        Calendar date = Calendar.getInstance();

        StringBuilder docID = new StringBuilder();
        docID.append(userID);
        docID.append("_");
        docID.append(date.get(Calendar.MONTH) + 1);
        docID.append("-");
        docID.append(date.get(Calendar.DAY_OF_MONTH));
        docID.append("-");
        docID.append(date.get(Calendar.YEAR));
        docID.append("_");
        int hour = date.get(Calendar.HOUR_OF_DAY);
        if (hour < 10)
            docID.append("0");
        docID.append(hour);
        docID.append("00");
        docID.append("_");
        docID.append("Labels");

        Document d = getDatabase(context).getDocument(docID.toString());
        Map<String, Object> properties = d.getProperties();

        if (properties == null || !properties.containsKey("Version")) {
            Log.v(TAG, "Setting metadata properties");
            properties = new HashMap<>();
            // New document
            properties.put(DataManagementService.TYPE, "Label");
            properties.put("Hour", date.get(Calendar.HOUR_OF_DAY));
            properties.put("User", userID);

            properties.put(DataManagementService.FIRST_ENTRY, date.getTimeInMillis());
            date.set(Calendar.MINUTE, 59);
            date.set(Calendar.SECOND, 59);
            properties.put(DataManagementService.LAST_ENTRY, date.getTimeInMillis());
            properties.put(DataManagementService.USER_ID, userID);
            properties.put(DataManagementService.DATA, new LinkedList<Map>());
            properties.put("Version", "3.0.0");
            d.putProperties(properties);
        }
        return d;
    }

    public static Database getOldestDatabase(Context c) throws IOException, CouchbaseLiteException {
        c = c.getApplicationContext();
        Manager m = getManager(c);
        List<String> names = new ArrayList<>(m.getAllDatabaseNames());
        if (names.size() == 0) return null;
        Collections.sort(names);
        names.remove(LABEL_DB_NAME);
        Log.d(TAG, "List of names: " + names);

        Database db = null;
        int i = 0;
        while (db == null && i < names.size()) {
            db = m.getExistingDatabase(names.get(i));
            if (db != null && db.getDocumentCount() == 0) {
                Log.v(TAG, "Deleting database " + db.getName());
                db.delete();
                db = null;
            } else if (db != null){
                Log.v(TAG, "Database " + db.getName() + " has " + db.getDocumentCount() + " docs");
            }
            i++;
        }
        if (db != null) Log.v(TAG, "Using database " + db.getName());
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

    // TODO: unhack this hack
    private static int documentsGot = 0;
    public static Document getDocument(Calendar date, String type, String userId, Context context)
            throws CouchbaseLiteException, IOException {
        if (documentsGot++ %20 == 0) {Log.v(TAG, "Compacting...");compactDatabases(context);}

        // ID will be formed as follows: UserID_Type_MM-DD-YYYY_HHHH_MM
        StringBuilder docID = new StringBuilder();
        docID.append(userId);
        docID.append("_");
        docID.append(type);
        docID.append("_");
        docID.append(date.get(Calendar.MONTH) + 1);
        docID.append("-");
        docID.append(date.get(Calendar.DAY_OF_MONTH));
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

        Database db = getDatabase(context);
        if (db.getDocumentCount() == 0) {
            // Need to insert the label document
            getLabelDocument(userId, context);
        }
        Document d = db.getDocument(docID.toString());

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
