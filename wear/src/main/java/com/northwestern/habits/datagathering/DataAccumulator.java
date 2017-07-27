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
    private long lastEntry;
    private String type;
    private LinkedList<String> keys;

    public DataAccumulator(String type, int capacity) {
        super();

        this.type = type;
        this.capacity = capacity;
        this.firstEntry = 0;
        this.lastEntry = 0;
    }

    public DataAccumulator(DataAccumulator buffer) {
        Iterator<Map<String, Object>> bufferIter = buffer.dataArray.listIterator();
        this.capacity = buffer.capacity;
        this.type = buffer.type;
        this.firstEntry = buffer.firstEntry;
        this.lastEntry = buffer.lastEntry;
        this.keys = buffer.keys;
        this.dataArray = new LinkedList<>();
        while (bufferIter.hasNext()) {
            Map<String, Object> point = bufferIter.next();
            this.putDataPoint(point, (long) point.get("Time"));
        }
    }

    public String getType() {return type;}

    public String getHeader() {
        StringBuilder builder = new StringBuilder();
        for (String key:keys) {
            builder.append(key);
            builder.append(",");
        }
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public String toString(){

        StringBuilder stringBuilder = new StringBuilder();
        ListIterator<Map<String, Object>> bufferIter = dataArray.listIterator();

        while (bufferIter.hasNext()) {
            Map<String, Object> point = bufferIter.next();

            for (String key:keys) {
                stringBuilder.append(point.get(key).toString());
                stringBuilder.append(",");
            }
            stringBuilder.append("\n");
        }

        String bufferString = stringBuilder.toString();
        return bufferString;
    }

    public long getFirstEntry() { return firstEntry; }

    public boolean putDataPoint(Map point, long time) {
        if (firstEntry == 0) {
            firstEntry = time;
        }

        if (keys == null) {
            keys = new LinkedList<String>();
            keys.addAll(point.keySet());
            Collections.sort(keys);
        }

        dataArray.add(point);
        return isFull();
    }

    public boolean isFull() {
        return dataArray.size() >= capacity;
    }
}