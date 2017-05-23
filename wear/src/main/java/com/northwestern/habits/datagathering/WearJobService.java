package com.northwestern.habits.datagathering;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Y.Misal on 4/25/2017.
 */

public class WearJobService extends JobService {

    private static final String TAG = "WearJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent i = new Intent(this, DataService.class);
        startService(i);
        Log.e(TAG, "Data collection service");
        return true; // true if we're not done yet
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // true if we'd like to be rescheduled
        return true;
    }
}
