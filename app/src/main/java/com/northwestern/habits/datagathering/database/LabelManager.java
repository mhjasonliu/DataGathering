package com.northwestern.habits.datagathering.database;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 9/15/2016
 */
public class LabelManager {
    private static final String TAG = "LabelManager";

    public static void addLabelChange(String userID, Context c, final String label, final long timeStamp) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("Time", timeStamp);
        dataMap.put("Label", label);
        final List<Map<String, Object>> dataList = new LinkedList<>();
        dataList.add(dataMap);
        List<String> properties = Arrays.asList("Time", "Label");

        // Add change to the csv
        File folder = CsvWriter.getFolder(timeStamp, userID, "Label");
        File csv = CsvWriter.getCsv(folder, timeStamp);
        FileWriter writer = null;
        try {
            if (!csv.exists()) {
                writer = CsvWriter.writeProperties(properties, csv, c);
            } else {
                writer = new FileWriter(csv, true);
            }

            CsvWriter.writeDataSeries(writer, dataList, properties);
        } catch (IOException e) {
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


        // Add change to the database
    }


}
