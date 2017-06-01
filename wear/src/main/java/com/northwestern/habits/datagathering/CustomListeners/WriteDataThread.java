package com.northwestern.habits.datagathering.CustomListeners;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Y.Misal on 5/30/2017.
 */

public class WriteDataThread extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "WriteDataThread";

    volatile boolean mRunning = true;
    //set mRunning to false when terminating appln
    Queue<DataAccumulator> mQueue;
    Object obj;
    private Context mContext;

    public WriteDataThread(Context context)
    {
        mContext = context;
        mQueue = new LinkedList<DataAccumulator>();
        obj= new Object();
    }

    public synchronized void SaveToFile(DataAccumulator acc)
    {
        synchronized (obj)
        {
            mQueue.add(acc);
            Log.v(TAG, "current queue size " + mQueue.size() + " recently added "+acc.type);
        }

    }

    private void SaveAccumulator(DataAccumulator accumulator)
    {
        Log.v(TAG, "Got Acc to save " + accumulator.type+ " count:"+accumulator.getCount());
        long firstPointTime = accumulator.getFirstEntry();

        File folder = getFolder(firstPointTime, accumulator.type);

        // Make csv
        File csv = getCsv(folder, firstPointTime);

        Map<Integer, List<Map<String, Object>>> dataSplit = accumulator.splitIntoMinutes(mContext);

        for (List<Map<String, Object>> series : dataSplit.values()) {
            Map<String, Object> firstPoint = series.get(0);

            LinkedList<String> properties = new LinkedList<String>();
            properties.addAll(firstPoint.keySet());
            FileWriter writer = null;
            try {
                if (!csv.exists()) {
                    writer = writeProperties(properties, csv);
                } else {
                    writer = new FileWriter(csv, true);
                }

               writeDataSeries(writer, series, properties);

            } catch (ConcurrentModificationException | IOException e) {
                writeError(e, mContext);
            } finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        writeError(e, mContext);
                    }
                }
            }
        }
    }
    @Override
    protected Void doInBackground(Void... voids) {
        while(mRunning)
        {
            if(mQueue.size()>0)
            {
                DataAccumulator first;
                synchronized (obj)
                {
                    first= mQueue.remove();
                    Log.v(TAG, "queue size after remove " + mQueue.size()+" removed type:"+first.type);
                }
                //process first.
                SaveAccumulator(first);
            }
            else
            {

            }
        }
        return null;
    }

    public File getFolder(long timestamp, String type) {
        // Date for use in the folder path
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        int day = date.get(Calendar.DAY_OF_MONTH);
        int month = date.get(Calendar.MONTH) + 1;
        int year = date.get(Calendar.YEAR);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        String hourst = (hour < 10)
                ? "0" + Integer.toString(hour) + "00"
                : Integer.toString(hour) + "00";
        String dateString = Integer.toString(month) + "-"
                + Integer.toString(day) + "-"
                + Integer.toString(year);

        String PATH = mContext.getExternalFilesDir(null) + "/WearData/" + type + "/" + dateString + "/" + hourst;

        File folder = new File(PATH);
        boolean bret= folder.mkdirs();
        if (bret) {
            //folder.mkdirs();
            Log.v(TAG, "directory " + folder.getPath() + " Succeeded ");
        }
        else
        {
            Log.v(TAG, "----------------- directory creattion FAILED --------------");
            Log.v(TAG, "directory " + folder.getPath() + " FAILED ");
        }
        return folder;
    }

    public File getCsv(File folder, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        String hourst = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        int minute = c.get(Calendar.MINUTE);
        String fName = hourst.concat("_" + Integer.toString(minute) + ".csv");
        return new File(folder.getPath(), fName);
    }

    public FileWriter writeProperties(List<String> properties, File csv)
            throws IOException {
        if (!csv.exists()) {
            // Make the file
            if (!csv.createNewFile()) {
                writeError(new IOException("Failed to create csv " + csv.toString()), mContext);
            }
//            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            intent.setData(Uri.fromFile(csv));
//            context.sendBroadcast(intent);


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

    public void writeDataSeries(FileWriter csvWriter, List<Map<String, Object>> dataList,
                                List<String> properties) {
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
                    writeError(e, mContext);
                }
            }
            try {
                csvWriter.append("\n");
                newLine = true;
            } catch (IOException e) {
                writeError(e, mContext);
            }
        }
    }

    public void writeError(Throwable e, Context context) {
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
}
