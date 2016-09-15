package com.northwestern.habits.datagathering.database;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

/**
 * Created by William on 9/15/2016
 */
public class CsvWriter {
    private static final String TAG = "CsvWriter";

    public static File getFolder(long timestamp, String userID, String type) {
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

        String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/" +
                userID + "/" + type + "/" + dateString + "/" + hourst;
        File folder = new File(PATH);
        if (!folder.exists()) {
            Log.v(TAG, "directory " + folder.getPath() + " Succeeded " + folder.mkdirs());
        }
        return folder;
    }

}
