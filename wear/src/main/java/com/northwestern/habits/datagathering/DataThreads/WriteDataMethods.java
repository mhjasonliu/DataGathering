package com.northwestern.habits.datagathering.DataThreads;

import android.content.Context;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Y.Misal on 7/26/2017.
 */

public class WriteDataMethods implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "WriteDataMethods";
    private Context mContext;
    private final Object mAccelObj;
    private static ConcurrentLinkedQueue<DataAccumulator> mQueue = null;

    public WriteDataMethods(Context context) {
        mContext = context;
        mAccelObj = new Object();
        mQueue    = new ConcurrentLinkedQueue<>();
    }

    public static void addToQueue(DataAccumulator accumulator, Context context) {
        try {
            mQueue.add(accumulator);
            Log.v(TAG, "current queue size " + mQueue.size() + " recently added "+accumulator.type+" & size "+accumulator.getCount());
        } catch (IllegalStateException e) {
            writeError(e, context);
            e.printStackTrace();
        }
        if(mQueue.size() > 0) {
            DataAccumulator accumulator1 = mQueue.remove();
            Log.v(TAG, "queue size after remove " + mQueue.size() + " removed type:" + accumulator1.type);
            saveAccumulator(accumulator1, context);
        }
    }

    public static void saveAccumulator(DataAccumulator accumulator, Context context) {
//        Log.v(TAG, "Got Acc to save " + accumulator.type+ " count:"+accumulator.getCount());
        if (accumulator == null) return;
        long firstPointTime = accumulator.getFirstEntry();

        File folder = getFolder(firstPointTime, accumulator.type, context);

        // Make csv
        File csv = getCsv(folder, firstPointTime);

        Map<Integer, List<Map<String, Object>>> dataSplit = accumulator.splitIntoMinutes(context);

        for (List<Map<String, Object>> series : dataSplit.values()) {
            Map<String, Object> firstPoint = series.get(0);

            LinkedList<String> properties = new LinkedList<String>();
            properties.addAll(firstPoint.keySet());
            if (!accumulator.type.equalsIgnoreCase("HeartRate"))
                Collections.sort(properties);
            FileWriter writer = null;
            Log.v(TAG, "writing to : " + csv.getAbsolutePath());
            try {
                if (!csv.exists()) {
                    writer = writeProperties(properties, csv, context);
                } else {
                    writer = new FileWriter(csv, true);
                }

                writeDataSeries(writer, series, properties, context);
                String text = String.format("Writing %s data to CSV file", accumulator.type);
//                writeLogs( text + "_" + System.currentTimeMillis(), mContext );

            } catch (ConcurrentModificationException | IOException e) {
                writeError(e, context);
            } finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        writeError(e, context);
                    }
                }
            }
        }
    }

    public static File getFolder(long timestamp, String type, Context context) {
        // Date for use in the folder path
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        int day = date.get(Calendar.DAY_OF_MONTH);
        int month = date.get(Calendar.MONTH) + 1;
        int year = date.get(Calendar.YEAR);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        /*String hourst = (hour < 10)
                ? "0" + Integer.toString(hour) + "00"
                : Integer.toString(hour) + "00";*/
        String hourst = (hour < 10)
                ? "0" + Integer.toString(hour)
                : Integer.toString(hour);
        String dateString = Integer.toString(month) + "-"
                + Integer.toString(day) + "-"
                + Integer.toString(year);

        String PATH = context.getExternalFilesDir(null) + "/WearData/" + type + "/" + dateString + "/" + hourst;

        File folder = new File(PATH);
        boolean bret= folder.mkdirs();
        if (bret) {
            //folder.mkdirs();
            Log.v(TAG, "directory " + folder.getPath() + " Succeeded type " + type);
        } else {
            Log.v(TAG, "directory " + folder.getPath() + " FAILED type " + type);
        }
        return folder;
    }

    public static File getCsv(File folder, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        String hourst = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        int minute = (c.get(Calendar.MINUTE));
        String minute1 = (minute < 10)
                ? "0" + Integer.toString(minute)
                : Integer.toString(minute);
        String fName = hourst.concat("_" + minute1 + ".csv");
        Log.v(TAG, "writing to getCsv(): " + fName);
        return new File(folder.getPath(), fName);
    }

    public static FileWriter writeProperties(List<String> properties, File csv, Context context)
            throws IOException {
//        Log.v(TAG, "writing to not exist: " + csv.getName());
        if (!csv.exists()) {
            // Make the file
            if (!csv.createNewFile()) {
                writeError(new IOException("Failed to create csv " + csv.toString()), context);
            }

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

    public static void writeDataSeries(FileWriter csvWriter, List<Map<String, Object>> dataList,
                                 List<String> properties, Context context) {
        boolean newLine = true;
        for (Map<String, Object> datum :
                dataList) {
            for (String property :
                    properties) {
                try {
                    if (!newLine) csvWriter.append(",");
                    csvWriter.append(datum.get(property).toString());
                    newLine = false;
                } catch (IOException e) {
                    writeError(e, context);
                }
            }
            try {
                csvWriter.append("\n");
                newLine = true;
            } catch (IOException e) {
                writeError(e, context);
            }
        }
    }

    public static void writeLogs(String str, Context context, Context context1) {
        String[] message = str.split("_");
        String PATH = context.getExternalFilesDir(null) + "/WearData/LOGS/";
        File folder = new File(PATH);
        if (!folder.exists()) folder.mkdirs();

        File errorReport = new File(folder.getPath()
                + "/Logs_"
                + System.currentTimeMillis()
                + ".txt");

        FileWriter writer = null;
        try {
            writer = new FileWriter(errorReport, true);
            String string = "\n\n-----------------" + message[0] + "-----------------\n\n";
            writer.write(string);
            writer.write(message[1]);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void writeError(Throwable e, Context context) {
        Log.e(TAG, "WRITING ERROR TO DISK: \n");
        e.printStackTrace();

        String PATH = context.getExternalFilesDir(null) + "/WearData/ERRORS/";
        File folder = new File(PATH);
        if (!folder.exists()) folder.mkdirs();

        Calendar c = Calendar.getInstance();
        File errorReport = new File(folder.getPath()
                + "/Exception_"
                + c.get(Calendar.HOUR_OF_DAY)
                + c.get(Calendar.MINUTE)
                + c.get(Calendar.SECOND)
                + ".txt");

        FileWriter writer = null;
        try {
            writer = new FileWriter(errorReport, true);
            writer.write("\n\n-----------------BEGINNING OF EXCEPTION-----------------\n\n");
            writer.write(e.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        writeError(e, mContext);
    }

}
