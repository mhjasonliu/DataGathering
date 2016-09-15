package com.northwestern.habits.datagathering.database;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 9/15/2016
 */
public class LabelManager {

    public static void addLabelChange(String userID, Context c, final String label, final long timeStamp) {
        // Add change to the csv


        // Add change to the database
        try {
            Document d = CouchBaseData.getLabelDocument(userID, c);
            d.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    Map<String, Object> properties = newRevision.getProperties();
                    Map<Long, String> labels = (Map<Long, String>) properties.get(DataManagementService.DATA);
                    if (labels == null) {
                        labels = new HashMap();
                        properties.put(DataManagementService.DATA, labels);
                    }
                    labels.put(timeStamp,label);
                    return true;
                }
            });
        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
        }

    }




}
