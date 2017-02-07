package com.northwestern.habits.datagathering.weardata;

import android.content.Context;
import android.util.Log;

import com.northwestern.habits.datagathering.database.DataSeries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A superclass for data that comes from the Android Wear
 *
 * Created by William on 2/6/2017.
 */

public abstract class WearDataObject {

    protected DataSeries dataSeries;
    protected String TYPE;

    protected abstract void parseObject(byte[] data);

    protected static float getNextFloat(byte[] data, int index) {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat(index);
    }

    protected static long getNextLong(byte[] data, int index) {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getLong(index);
    }

    public void sendToCsv(Context c) {
        Log.v("WearDataObject", "dataSeries size: " + dataSeries.getCount());
        dataSeries.exportCSV(c, TYPE);
    }

}
