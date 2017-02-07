package com.northwestern.habits.datagathering.weardata;

import com.northwestern.habits.datagathering.database.DataSeries;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by William on 2/6/2017.
 */

class AccelerometerData extends WearDataObject {
    public AccelerometerData(byte[] dataBuffer) {
        dataSeries = new DataSeries(TYPE, dataBuffer.length/ENTRY_SIZE);
        parseObject(dataBuffer);
    }

    private static final int ENTRY_SIZE = 3*4 + 8;
    private static final String TYPE = "Accel_Wear";
    private static final String TAG = "AccelerometerData";

    @Override
    protected void parseObject(byte[] data) {
        int i = 0;
        float x, y, z;
        long t;

        while (i < data.length) {
            x = getNextFloat(data, i);
            i += 4;

            y = getNextFloat(data, i);
            i += 4;

            z = getNextFloat(data, i);
            i += 4;

            t = getNextLong(data, i);
            i += 8;

            Map<String, Object> map = new HashMap<>();
            map.put("Time", t);
            map.put("x", x);
            map.put("y", y);
            map.put("z", z);

            dataSeries.putDataPoint(map, t);
        }
    }
}
