package com.northwestern.habits.datagathering;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class WriteData extends IntentService {
    private static final String TAG = "WriteDataThread";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_WRITE = "com.northwestern.habits.datagathering.action.WRITEBUFFER";
    private static final String PARAM_BUFFER = "com.northwestern.habits.datagathering.extra.BUFFER";

    private Context mContext;

    public WriteData() {
        super("WriteData");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, DataAccumulator acc) {
//        Intent intent = new Intent(context, WriteData.class);
//        intent.setAction(ACTION_WRITE);
//        intent.putExtra(PARAM_BUFFER, acc);
//        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WRITE.equals(action)) {
                final DataAccumulator buffer = intent.getParcelableExtra(PARAM_BUFFER);
                handleActionWrite(buffer);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private void handleActionWrite(DataAccumulator accumulator) {
//        Log.v(TAG, "Got Acc to save " + accumulator.type+ " count:"+accumulator.getCount());
        if (accumulator == null) return;
        long firstPointTime = accumulator.getFirstEntry();

        File folder = getFolder(firstPointTime, accumulator.type);

        // Make csv
        File csv = getCsv(folder, firstPointTime);
    }

    public File getFolder(long timestamp, String type) {
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

        String PATH = mContext.getExternalFilesDir(null) + "/WearData/" + type + "/" + dateString + "/" + hourst;

        File folder = new File(PATH);
        boolean bret= folder.mkdirs();
        if (bret) {
            //folder.mkdirs();
            Log.v(TAG, "directory " + folder.getPath() + " Succeeded ");
        } else {
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
//        Log.v(TAG, "writing to not exist: " + csv.getName());
        if (!csv.exists()) {
            // Make the file
            if (!csv.createNewFile()) {
                writeError(new IOException("Failed to create csv " + csv.toString()), mContext);
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
}
