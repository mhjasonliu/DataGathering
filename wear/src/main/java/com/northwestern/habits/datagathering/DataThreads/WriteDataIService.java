package com.northwestern.habits.datagathering.DataThreads;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.northwestern.habits.datagathering.DataAccumulator;

/**
 * Created by Y.Misal on 7/26/2017.
 */

public class WriteDataIService extends IntentService {
    private static final String TAG = "WriteDataIService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public WriteDataIService(String name) {
        super(name);
        Log.v(TAG, "testIS");
    }

    public WriteDataIService() {
        super("WriteDataIService");
        Log.v(TAG, "testIS");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        DataAccumulator accumulator = intent.getParcelableExtra("buffer");
        Log.v(TAG, "Type " + accumulator.type + ", count " + accumulator.getCount());
        WriteDataMethods.saveAccumulator(accumulator, getBaseContext());
    }

    private class WriteDataToFile extends AsyncTask<Void, Void, Void> {

        private WriteDataToFile(DataAccumulator accumulator) { }

        protected Void doInBackground(Void... urls) {

            return null;
        }

        protected void onPostExecute(Void result) {
        }
    }
}
