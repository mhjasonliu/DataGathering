package com.northwestern.habits.datagathering;

import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 1/29/2017.
 */

public class DataAccumulator {

    private int capacity = 0;

    private LinkedList<Map<String, Object>> dataArray = new LinkedList<>();
    private long firstEntry;
    private long lastEntry;
    private String type;

    public DataAccumulator(String type, int capacity) {
        super();

        this.type = type;
        this.capacity = capacity;
        this.firstEntry = 0;
        this.lastEntry = 0;
    }

    public long getFirstEntry() { return firstEntry; }

    public boolean putDataPoint(Map point, long time) {
        if (firstEntry == 0) {
            firstEntry = time;
        }

        lastEntry = time;
        dataArray.add(point);

        return isFull();
    }

    public Iterator<Map<String, Object>> getIterator() {
        return dataArray.listIterator();
    }

    public boolean isFull() {
        return dataArray.size() >= capacity;
    }

    public static List<Map> pack(List<Map> dataSoFar, List<Map<String, Object>> dataToAdd) {
        if (dataSoFar == null) dataSoFar = new LinkedList<>();
        dataSoFar.addAll(dataToAdd);
        return dataSoFar;
    }

    public Map<Integer, List<Map<String, Object>>> splitIntoMinutes(Context con) {
        int minute;
        Calendar c = Calendar.getInstance();
        Map<Integer, List<Map<String, Object>>> split = new HashMap<>();
        for (Map datum : dataArray) {
            try {
                Object time = datum.get("Time");
                if (time instanceof String) {
                    time = Long.valueOf((String) time);
                    WriteDataTask.writeError(
                            new Exception("The time was a string rather than a long: "
                                    + time.toString()), con);
                }

                c.setTimeInMillis((long) datum.get("Time"));
            } catch (ClassCastException e) {
                WriteDataTask.writeError(e, con);
            }
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

}
