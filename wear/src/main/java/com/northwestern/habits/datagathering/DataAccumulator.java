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
    public String type;

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
        this.dataArray = new LinkedList<>();
        while (bufferIter.hasNext()) {
            Map<String, Object> point = bufferIter.next();
            this.putDataPoint(point, (long) point.get("Time"));
        }
    }

    @Override
    public String toString(){

        // Parsing the keys
        Map<String, Object> firstElement = dataArray.getFirst();
        LinkedList<String> keys = new LinkedList<String>();
        keys.addAll(firstElement.keySet());

        Collections.sort(keys);


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(keys.toString());
        stringBuilder.append("\n");

        ListIterator<Map<String, Object>> bufferIter = this.dataArray.listIterator();

        while (bufferIter.hasNext()) {
            Map<String, Object> point = bufferIter.next();

            for (String key:keys) {
                stringBuilder.append(point.get(key).toString());
                stringBuilder.append(",");
            }
            stringBuilder.append("\n");
        }
        stringBuilder.append("END OF BUFFER\n");

        String bufferString = stringBuilder.toString();
        return bufferString;
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

    public boolean isFull() {
        return dataArray.size() >= capacity;
    }

    public int getCount() {
        return dataArray.size();
    }
}