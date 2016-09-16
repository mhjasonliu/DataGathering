package com.northwestern.habits.datagathering.banddata.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.northwestern.habits.datagathering.database.CsvWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 8/2/2016
 */
public class DataSeries {

    private int capacity = 0;

    private List<Map<String, Object>> dataArray = new LinkedList<>();
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

    public static List<Map> pack(List<Map> dataSoFar, List<Map> dataToAdd) {
        if (dataSoFar == null) dataSoFar = new LinkedList<>();
        dataSoFar.addAll(dataToAdd);
        return dataSoFar;
    }

    public Map<Integer, List<Map>> splitIntoMinutes() {
        int minute;
        Calendar c = Calendar.getInstance();
        Map<Integer, List<Map>> split = new HashMap<>();
        for (Map datum : dataArray) {
            c.setTimeInMillis(Long.valueOf((String) datum.get("Time")));
            minute = c.get(Calendar.MINUTE);
            if (!split.containsKey(minute)) {
                split.put(minute, new LinkedList<Map>());
            }
            split.get(minute).add(datum);
        }
        return split;
    }

    public int getCount() {
        return dataArray.size();
    }

    public void exportCSV(Context c, String userID, String type) {
        new ExportCSVTask(c, userID).doInBackground(null);
    }


    private class ExportCSVTask extends AsyncTask {
        private final String TAG = "ExportCSV_TAsk";
        private Context context;
        private String userID;

        public ExportCSVTask(Context c, String uID) {
            context = c;
            userID = uID;
        }

        @Override
        protected Object doInBackground(Object[] params) {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "permission denied");
            }

            File folder = CsvWriter.getFolder(firstEntry, userID, type);

            // Make csv
            File csv = CsvWriter.getCsv(folder, firstEntry);

            List<Map<String, Object>> dataSeries = dataArray;
            List<String> properties = new LinkedList(dataArray.get(0).keySet());
            FileWriter writer = null;
            try {
                if (!csv.exists()) {
                    writer = CsvWriter.writeProperties(properties, csv, context);
                } else {
                    writer = new FileWriter(csv, true);
                }

                CsvWriter.writeDataSeries(writer, dataSeries, properties);
                Log.v(TAG, "Wrote the file");

            } catch (ConcurrentModificationException | IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }
}