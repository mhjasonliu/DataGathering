package com.northwestern.habits.datagathering.database;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 8/2/2016
 */
public class DataSeries {

    private int capacity = 0;

    private LinkedList<Map<String, Object>> dataArray = new LinkedList<>();
    private long firstEntry;
    private long lastEntry;
    private String type;

    public DataSeries(String type, int capacity) {
        super();

        this.type = type;
        this.capacity = capacity;
        this.firstEntry = 0;
        this.lastEntry = 0;
    }

    public long getFirstEntry() { return firstEntry; }

    public void putDataPoint(Map point, long time) {
        if (firstEntry == 0) {
            firstEntry = time;
        }

        lastEntry = time;
        dataArray.add(point);
    }

    public boolean isFull() {
        return dataArray.size() >= capacity;
    }

    public static List<Map> pack(List<Map> dataSoFar, List<Map<String, Object>> dataToAdd) {
        if (dataSoFar == null) dataSoFar = new LinkedList<>();
        dataSoFar.addAll(dataToAdd);
        return dataSoFar;
    }

    public Map<Integer, List<Map<String, Object>>> splitIntoMinutes() {
        int minute;
        Calendar c = Calendar.getInstance();
        Map<Integer, List<Map<String, Object>>> split = new HashMap<>();
        for (Map datum : dataArray) {
            c.setTimeInMillis(Long.valueOf((String) datum.get("Time")));
            minute = c.get(Calendar.MINUTE);
            if (!split.containsKey(minute)) {
                split.put(minute, new LinkedList<Map<String, Object>>());
            }
            split.get(minute).add(datum);
        }
        return split;
    }

    public int getCount() {
        return dataArray.size();
    }

    public void exportCSV(Context c, String userID) {

        Log.e("DataSeries", "Data array size: " + this.dataArray.size());
        new ExportCsvTask(c, userID, this.dataArray, type).execute();
    }
}
