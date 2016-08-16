package com.northwestern.habits.datagathering.banddata;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.northwestern.habits.datagathering.database.DataManagementService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public void exportCSV(Context c, String userID, String type) {
        new ExportCSVTask(c, userID, type).doInBackground(null);
    }


    private class ExportCSVTask extends AsyncTask {
        private final String TAG = "ExportCSV_TAsk";
        private Context context;
        private String userID;
        private String type;

        public ExportCSVTask(Context c, String uID, String t) {
            context = c;
            userID = uID;
            type = t;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            // Make csv name
            String fName = userID;
            fName += "_" + type + "_";
            fName += firstEntry;
            fName += "_thru_";
            fName += lastEntry + ".csv";

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("CBD", "permission granted");
            } else {
                Log.e("CBD", "permission denied");
            }

            String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/";
            File folder = new File(PATH);
            if (!folder.exists()) {
                Log.v(TAG, "directory " + folder.getPath() + " Succeeded " + folder.mkdirs());
            }

            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "ASdf");
            }

            File csv = new File(PATH, fName);
            if (!csv.exists()) {
                try {
                    // Make the file
                    if (!csv.createNewFile()) {
                        throw new IOException();
                    }
                    FileWriter csvWriter = new FileWriter(csv.getPath(), true);
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(csv));
                    context.sendBroadcast(intent);


                    List<Map> dataSeries = dataArray;
                    List<String> properties = (List<String>) dataArray.get(0).keySet();

                    for (int i = 0; i < properties.size(); i++) {
                        csvWriter.append(properties.get(i));
                        if (i == properties.size() - 1) {
                            csvWriter.append("\n");
                        } else {
                            csvWriter.append(",");
                        }
                    }

                    // Write the file
                    for (Map<String, Object> dataPoint :
                            dataSeries) {

                        for (int i = 0; i < properties.size(); i++) {
                            Object datum = dataPoint.get(properties.get(i));

                            if (datum instanceof String) {
                                csvWriter.append(datum.toString());
                            } else if (datum instanceof Double) {
                                csvWriter.append(Double.toString((Double) datum));
                            } else if (datum instanceof Integer) {
                                csvWriter.append(Integer.toString((Integer) datum));
                            } else if (datum instanceof Long) {
                                csvWriter.append(Long.toString((Long) datum));
                            } else {
                                Log.e(TAG, "Unhandled case");
                                csvWriter.append(datum.toString());
                            }


                            if (i == properties.size() - 1) {
                                csvWriter.append("\n");
                            } else {
                                csvWriter.append(",");
                            }
                        }
                    }
                    csvWriter.flush();
                    csvWriter.close();

                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

}
