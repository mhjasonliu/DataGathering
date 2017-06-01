package com.northwestern.habits.datagathering;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.northwestern.habits.datagathering.filewriteservice.SingletonFileWriter;

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
 * Created by William on 2/25/2017.
 */

public class WriteDataTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "WriteDataTask";

    private DataAccumulator mAccumulator;
    private Context mContext;
    private String mType;
    private Queue<Map<String, Object>> mapQueue = null;


    public WriteDataTask(Context context, DataAccumulator accumulator, String type) {
        mAccumulator = accumulator;
        mContext = context;
        mType = type;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        long firstPointTime = mAccumulator.getFirstEntry();

        File folder = SingletonFileWriter.getInstance(mContext).getFolder(firstPointTime, mType);

        // Make csv
        File csv = SingletonFileWriter.getInstance(mContext).getCsv(folder, firstPointTime);

        Map<Integer, List<Map<String, Object>>> dataSplit = mAccumulator.splitIntoMinutes(mContext);

        for (List<Map<String, Object>> series : dataSplit.values()) {
            Map<String, Object> firstPoint = series.get(0);

            LinkedList<String> properties = new LinkedList<String>();
            properties.addAll(firstPoint.keySet());
            FileWriter writer = null;
            try {
                if (!csv.exists()) {
                    writer = SingletonFileWriter.getInstance(mContext).writeProperties(properties, csv);
                } else {
                    writer = new FileWriter(csv, true);
                }

                SingletonFileWriter.getInstance(mContext).writeDataSeries(writer, series, properties);

            } catch (ConcurrentModificationException | IOException e) {
                SingletonFileWriter.getInstance(mContext).writeError(e, mContext);
            } finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        SingletonFileWriter.getInstance(mContext).writeError(e, mContext);
                    }
                }
            }
        }

        return null;
    }
}
