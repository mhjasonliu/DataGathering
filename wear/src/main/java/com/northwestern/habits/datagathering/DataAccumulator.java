package com.northwestern.habits.datagathering;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.northwestern.habits.datagathering.DataThreads.WriteDataThread;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 1/29/2017.
 */

public class DataAccumulator implements Parcelable {

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

    //copy constructor
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

    protected DataAccumulator(Parcel in) {
        capacity = in.readInt();
        firstEntry = in.readLong();
        lastEntry = in.readLong();
        type = in.readString();
    }

    public static final Creator<DataAccumulator> CREATOR = new Creator<DataAccumulator>() {
        @Override
        public DataAccumulator createFromParcel(Parcel in) {
            return new DataAccumulator(in);
        }

        @Override
        public DataAccumulator[] newArray(int size) {
            return new DataAccumulator[size];
        }
    };

    public long getFirstEntry() { return lastEntry; }

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
                    WriteDataThread.writeError(
                            new Exception("The time was a string rather than a long: "
                                    + time.toString()), con);
                }

                c.setTimeInMillis((long) datum.get("Time"));
            } catch (ClassCastException e) {
                WriteDataThread.writeError(e, con);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(capacity);
        dest.writeLong(firstEntry);
        dest.writeLong(lastEntry);
        dest.writeString(type);
    }
}
