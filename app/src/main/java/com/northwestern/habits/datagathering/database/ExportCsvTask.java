package com.northwestern.habits.datagathering.database;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Exports a series to csv
 * Created by William on 2/6/2017.
 */

public class ExportCsvTask extends AsyncTask<Void, Void, Void> {
    private final String TAG = "ExportCSV_Task";
    private Context context;
    private String userID;
    private String type;
    private LinkedList<Map<String, Object>> series;

    public ExportCsvTask(Context c, String uID, LinkedList<Map<String, Object>> series, String type) {
        context = c;
        userID = uID;
        this.series = series;
        this.type = type;
    }

    @Override
    protected Void doInBackground(Void[] params) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission denied");
        }

        Map<String, Object> firstPoint = series.getFirst();
        Long firstPointTime = (Long) firstPoint.get("Time");

        File folder = CsvWriter.getFolder(firstPointTime, userID, type);

        // Make csv
        File csv = CsvWriter.getCsv(folder, firstPointTime);

        List<Map<String, Object>> dataSeries = series;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(firstPointTime);
        int firstMinute = c.get(Calendar.MINUTE);
        int lastMinute = c.get(Calendar.MINUTE);

        Log.v(TAG, "Minute of first entry: " + firstMinute);
        Log.v(TAG, "Minute of last entry: " + lastMinute);
        if (firstMinute != lastMinute) {
            Log.e(TAG, "WARNING: minute not properly split");
        }

        List<String> properties = new LinkedList(firstPoint.keySet());
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