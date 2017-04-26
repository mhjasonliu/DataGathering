package com.northwestern.habits.datagathering;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

/**
 * Created by Y.Misal on 4/25/2017.
 */

public class WearJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent i = new Intent(this, DataService.class);
        startService(i);
        return true; // true if we're not done yet
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // true if we'd like to be rescheduled
        return true;
    }


}
