package com.northwestern.habits.datagathering;

import android.content.Context;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by William on 1/29/2017.
 */

public class DataAccumulator {

    private int capacity = 0;

    private LinkedList<Map<String, Object>> dataArray = new LinkedList<>();
    private long firstEntry;
    private String type;
    private LinkedList<String> keys;

    public DataAccumulator(String type, int capacity) {
        super();

        this.type = type;
        this.capacity = capacity;
        this.firstEntry = 0;
        this.keys = null;
    }

    public DataAccumulator(DataAccumulator buffer) {
        this.capacity = buffer.capacity;
        this.type = buffer.type;
        this.firstEntry = buffer.firstEntry;
        this.keys = buffer.keys;

        this.dataArray = new LinkedList<>();
        for(Map<String, Object> point:buffer.dataArray) {
            this.putDataPoint(point, (long) point.get("Time"));
        }
    }

    public String getType() {return type;}

    public String getHeader() {
        StringBuilder builder = new StringBuilder();
        String prefix = "";
        for (String key:keys) {
            builder.append(prefix);
            builder.append(key);
            prefix = ",";
        }
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public String toString(){

        StringBuilder stringBuilder = new StringBuilder();
        for (Map<String,Object> point: dataArray) {
            String prefix = "";
            for (String key:keys) {
                stringBuilder.append(prefix);
                stringBuilder.append(point.get(key).toString());
                prefix = ",";
            }
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    public long getFirstEntry() { return firstEntry; }

    public boolean putDataPoint(Map<String, Object> point, long time) {
        if (firstEntry == 0) {
            firstEntry = time;
        }

        if (keys == null) {
            keys = new LinkedList<>(point.keySet());
            Collections.sort(keys);
        }

        dataArray.add(point);
        return isFull();
    }

    public boolean isFull() {
        return dataArray.size() >= capacity;
    }
}