package com.northwestern.habits.datagathering.banddata;

import com.northwestern.habits.datagathering.database.DataManagementService;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 8/2/2016.
 */
public class DataSeries {

    private int capacity = 0;

    private List<Map> dataArray = new LinkedList<>();
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

    public Map<String, Object> pack() {
        Map<String, Object> m = new HashMap<>();

        m.put(DataManagementService.FIRST_ENTRY, firstEntry);
        m.put(DataManagementService.LAST_ENTRY, lastEntry);
        m.put(DataManagementService.TYPE, type);
        m.put(DataManagementService.DATA, dataArray);
        m.put(DataManagementService.DATA_KEYS, dataArray.get(0).keySet());

        return m;
    }

}
