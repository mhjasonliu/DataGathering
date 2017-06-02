package com.northwestern.habits.datagathering.database;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 9/15/2016
 */
public class CsvWriter {
    private static final String TAG = "CsvWriter";

    /** Decides the name of the folder from parameters given. Creates the folder if non-existant
     * @param timestamp the timestamp of a datum that will be in the folder
     * @param userID The ID of the user associated with this data
     * @param type The type of data to be contained int he folder
     * @return File with the path of the folder
     */
    public static synchronized File getFolder(long timestamp, String userID, String type) {
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

    /** Creates the csv file in the folder
     * @param folder the folder in which the csv file should reside
     * @param timestamp timestamp of a datum to be contained in the csv
     * @return the file representing the csv
     */
    public static synchronized File getCsv(File folder, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        String hourst = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        int minute = c.get(Calendar.MINUTE);
        String fName = hourst.concat("_" + Integer.toString(minute) + ".csv");
        return new File(folder.getPath(), fName);
    }

    /** Fills out the header of the csv from the given properties
     * @param properties List of csv headers
     * @param csv File representing the csv
     * @param context
     * @return FileWriter for the csv CLOSE TO AVOID MEMORY LEAK
     * @throws IOException
     */
    public static synchronized FileWriter writeProperties(List<String> properties, File csv, Context context)
            throws IOException {
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

    /** Appends the data in dataList to the csvWriter
     * @param csvWriter Open file writer for writing th csv
     * @param dataList List of data to be written
     * @param properties List of headers of the csv (in the order of the csv)
     */
    public static synchronized void writeDataSeries(FileWriter csvWriter, List<Map<String, Object>> dataList,
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
                } catch (IOException e) {e.printStackTrace();}
            }
            try {
                csvWriter.append("\n");
                newLine = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
