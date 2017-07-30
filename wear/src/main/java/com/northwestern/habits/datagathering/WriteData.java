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
import java.util.Locale;
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
    private static final String ACTION_WRITE_BUFFER = "com.northwestern.habits.datagathering.action.WRITEBUFFER";
    private static final String ACTION_WRITE_BUFFER_ERROR = "com.northwestern.habits.datagathering.action.WRITEERROR";

    private static final String PARAM_CONTENT = "com.northwestern.habits.datagathering.extra.CONTENT";
    private static final String PARAM_TIME = "com.northwestern.habits.datagathering.extra.TIME";
    private static final String PARAM_TYPE = "com.northwestern.habits.datagathering.extra.TYPE";
    private static final String PARAM_HEADER = "com.northwestern.habits.datagathering.extra.HEADER";

    private static final String PARAM_ERROR = "com.northwestern.habits.datagathering.extra.ERROR";

    public WriteData() {
        super("WriteData");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void requestWrite(Context context, DataAccumulator buf) {
        Intent intent = new Intent(context, WriteData.class);
        intent.setAction(ACTION_WRITE_BUFFER);
        intent.putExtra(PARAM_CONTENT, buf.toString());
        intent.putExtra(PARAM_TIME, buf.getFirstEntry());
        intent.putExtra(PARAM_TYPE, buf.getType());
        intent.putExtra(PARAM_HEADER, buf.getHeader());
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WRITE_BUFFER.equals(action)) {
                String content = intent.getStringExtra(PARAM_CONTENT);
                long time = intent.getLongExtra(PARAM_TIME, 1L);
                String type = intent.getStringExtra(PARAM_TYPE);
                String header = intent.getStringExtra(PARAM_HEADER);
                handleActionWrite(content, time, type, header);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private void handleActionWrite(String content, long time, String type, String header) {
        File folder = getFolder(time, type);
        File csv = getCsv(folder, time);
        Log.v(TAG, csv.toString());

        try {
            if (!csv.exists()) {
                FileWriter header_writer = new FileWriter(csv, true);
                header_writer.write(header);
                Log.w(TAG, header);
                header_writer.flush();
                header_writer.close();
            }

            FileWriter csv_writer = new FileWriter(csv, true);
            csv_writer.write(content);
            csv_writer.flush();
            csv_writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
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
                ? "0" + Integer.toString(hour)
                : Integer.toString(hour);
        String dateString = Integer.toString(month) + "-"
                + Integer.toString(day) + "-"
                + Integer.toString(year);

        String PATH = this.getExternalFilesDir(null) + "/WearData/" + type + "/" + dateString + "/" + hourst;

        File folder = new File(PATH);
        boolean bret= folder.mkdirs();
        return folder;
    }

    public File getCsv(File folder, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        String hourst = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        int minute = c.get(Calendar.MINUTE);
        String fName = hourst.concat("_" + String.format(Locale.US,"%02d",minute) + ".csv");
        return new File(folder.getPath(), fName);
    }
}
