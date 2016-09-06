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

import com.microsoft.band.sensors.HeartRateQuality;
import com.microsoft.band.sensors.MotionType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by William on 8/2/2016
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

    public Set getDataKeys() {return dataArray.get(0).keySet();}

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

            // Date for use in the folder path
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(firstEntry);
            int day = date.get(Calendar.DAY_OF_MONTH);
            int month = date.get(Calendar.MONTH) + 1;
            int year = date.get(Calendar.YEAR);
            int hour = date.get(Calendar.HOUR_OF_DAY);
            String hourst = (hour < 10)
                    ? "0" + Integer.toString(hour) + "00"
                    : Integer.toString(hour) + "00";
            int minute = date.get(Calendar.MINUTE);
            String dateString = Integer.toString(month) + "-"
                    + Integer.toString(day) + "-"
                    + Integer.toString(year);

            String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/" +
                    userID + "/" + type + "/" + dateString + "/" + hourst;
            File folder = new File(PATH);
            if (!folder.exists()) {
                Log.v(TAG, "directory " + folder.getPath() + " Succeeded " + folder.mkdirs());
            }

            // Make csv name
            hourst = Integer.toString(hour);
            String fName = hourst.concat(":" + Integer.toString(minute) + ".csv");

            File csv = new File(PATH, fName);
            List<Map> dataSeries = dataArray;
            List<String> properties = new LinkedList(dataArray.get(0).keySet());
            try {
                FileWriter csvWriter = null;
                try {
                csvWriter = writeProperties(properties, csv);
                    // Write the file
                    for (Map<String, Object> dataPoint :
                            dataSeries) {
                        for (int i = 0; i < properties.size(); i++) {
                            long timestamp = Long.valueOf((String) dataPoint.get("Time"));
                            date.setTimeInMillis(timestamp);
                            if (minute != date.get(Calendar.MINUTE)) {
                                minute = date.get(Calendar.MINUTE);
                                fName = hourst.replace("0","").concat(":" + Integer.toString(minute) + ".csv");
                                Log.v(TAG, fName);
                                csv = new File(PATH, fName);
                                csvWriter.flush();
                                csvWriter.close();
                                csvWriter = writeProperties(properties, csv);
                            }


                            try {
                                Object datum = dataPoint.get(properties.get(i));

                                if (datum instanceof String) {
                                    csvWriter.append(datum.toString());
                                } else if (datum instanceof Double) {
                                    csvWriter.append(Double.toString((Double) datum));
                                } else if (datum instanceof Integer) {
                                    csvWriter.append(Integer.toString((Integer) datum));
                                } else if (datum instanceof Long) {
                                    csvWriter.append(Long.toString((Long) datum));
                                } else if (datum instanceof Float) {
                                    csvWriter.append(Float.toString((Float) datum));
                                } else if (datum instanceof HeartRateQuality) {
                                    csvWriter.append(datum.toString());
                                } else if (datum instanceof MotionType) {
                                    csvWriter.append(datum.toString());
                                } else {
                                    Log.e(TAG, "Unhandled case " + datum.getClass());
                                    csvWriter.append(datum.toString());
                                }
                                if (i == properties.size() - 1) {
                                    csvWriter.append("\n");
                                } else {
                                    csvWriter.append(",");
                                }
                            } catch (NullPointerException e) {
                                Log.e(TAG, "Row was null");
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Log.v(TAG, "Wrote the file");

                } catch (ConcurrentModificationException | IOException e) {
                    e.printStackTrace();
                } finally {
                    if (csvWriter != null) {
                        csvWriter.flush();
                        csvWriter.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private FileWriter writeProperties(List<String> properties, File csv) throws IOException {
            if (!csv.exists()) {
                // Make the file
                if (!csv.createNewFile()) {
                    throw new IOException();
                }
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(csv));
                context.sendBroadcast(intent);


                FileWriter csvWriter = new FileWriter(csv.getPath(), true);
                for (int i = 0; i < properties.size(); i++) {
                    csvWriter.append(properties.get(i));
                    if (i == properties.size() - 1) {
                        csvWriter.append("\n");
                    } else {
                        csvWriter.append(",");
                    }
                }
                return csvWriter;
            } else {
                return new FileWriter(csv.getPath(), true);
            }
        }
    }
}
