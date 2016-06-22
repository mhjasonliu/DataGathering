package com.northwestern.habits.datagathering;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;

/**
 * Created by William on 6/22/2016.
 */
public class CouchBaseData {

    private static Database database = null;
    private static Manager manager = null;

    public static final String DB_NAME = "data_gathering_data";

    public static Database getDatabaseInstance() throws CouchbaseLiteException {
        if ((database == null) && (manager != null)) {
            database = manager.getDatabase(DB_NAME);
        }
        return database;
    }

    public static Manager getManagerInstance(AndroidContext c) throws IOException {
        if (manager == null) {
            manager = new Manager(c, Manager.DEFAULT_OPTIONS);
        }
        return manager;
    }
}
