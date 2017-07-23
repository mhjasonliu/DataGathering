package com.northwestern.habits.datagathering;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class WriteData extends IntentService {
    private static final String TAG = "WriteData";

    private Context mContext;
    public static final String ACTION_WRITE = "com.northwestern.habits.datagathering.action.writecsv";

    public static final String TIME = "com.northwestern.habits.datagathering.extra.TIME";
    public static final String BUFFER = "com.northwestern.habits.datagathering.extra.BUFFER";
    public static final String TYPE = "com.northwestern.habits.datagathering.extra.TYPE";

    public WriteData() {
        super("WriteData");
        mContext = getBaseContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WRITE.equals(action)) {
                final String time = intent.getStringExtra(TIME);
                final String buffer = intent.getStringExtra(BUFFER);
                final String type = intent.getStringExtra(TYPE);
                writeCSV(time, buffer, type);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void writeCSV(String time, String buffer, String type) {
        Long timestamp = Long.parseLong(time);
        File folder = getFolder(timestamp, type);
        File csv = getCsv(folder,timestamp);
        try {
            FileWriter csvWriter = new FileWriter(csv.getPath(), true);
            csvWriter.append(buffer);
        }
        catch (ConcurrentModificationException | IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
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
}
