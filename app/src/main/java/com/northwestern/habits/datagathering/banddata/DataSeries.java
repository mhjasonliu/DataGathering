package com.northwestern.habits.datagathering.banddata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by William on 8/2/2016.
 */
public class DataSeries extends JSONObject {

    private int capacity = 0;

    private JSONArray dataArray = new JSONArray();
    private long firstEntry;
    private long lastEntry;
    private String type;

    private final String FIRST_ENTRY = "First_Entry";
    private final String TYPE = "Type";
    private final String LAST_ENTRY = "Last_Entry";
    private final String DATA = "Data";

    public DataSeries(String type, int capacity) {
        super();

        this.type = type;
        this.capacity = capacity;
        this.firstEntry = 0;
        this.lastEntry = 0;
    }

    public void putDataPoint(JSONObject point, long time) {
        if (firstEntry == 0) {
            firstEntry = time;
        }

        lastEntry = time;
        dataArray.put(point);
    }

    public boolean isFull() {
        return dataArray.length() >= capacity;
    }

    public JSONObject pack() {
        try {
            this.put(FIRST_ENTRY, firstEntry);
            this.put(LAST_ENTRY, lastEntry);
            this.put(TYPE, type);
            this.put(DATA, dataArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return this;
    }

}
