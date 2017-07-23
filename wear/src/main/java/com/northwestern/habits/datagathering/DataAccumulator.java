package com.northwestern.habits.datagathering;

import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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

    public String serialize(){

        ListIterator<Map<String, Object>> bufferIter = this.dataArray.listIterator();

        StringBuilder stringBuilder = new StringBuilder();

        int sensorType = 0;

        Map<String, Object> firstPoint = this.dataArray.getFirst();
        for ( String key : firstPoint.keySet() ){
            if(key.contains("acc")){
                stringBuilder.append("Time,accX,accY,accZ\n");
                sensorType = 1;
                break;
            }
            if(key.contains("rot")){
                stringBuilder.append("Time,rotX,rotY,rotZ\n");
                sensorType = 2;
                break;
            }
        }

        while (bufferIter.hasNext()) {
            Map<String, Object> point = bufferIter.next();

            long timeLong = (long) point.get("Time");
            float x;
            float y;
            float z;
            if (sensorType == 1){
                x = (float) point.get("accX");
                y = (float) point.get("accY");
                z = (float) point.get("accZ");
            }
            else {
                x = (float) point.get("rotX");
                y = (float) point.get("rotY");
                z = (float) point.get("rotZ");
            }

            String timeString = Long.toString(timeLong);
            String xString = Float.toString(x);
            String yString = Float.toString(y);
            String zString = Float.toString(z);

            stringBuilder.append(timeString);
            stringBuilder.append(xString);
            stringBuilder.append(yString);
            stringBuilder.append(zString);
            stringBuilder.append("\n");
        }

        String bufferString = stringBuilder.toString();
        return  bufferString;
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

    public static List<Map> pack(List<Map> dataSoFar, List<Map<String, Object>> dataToAdd) {
        if (dataSoFar == null) dataSoFar = new LinkedList<>();
        dataSoFar.addAll(dataToAdd);
        return dataSoFar;
    }

    public int getCount() {
        return dataArray.size();
    }

}
