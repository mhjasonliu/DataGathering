package com.northwestern.habits.datagathering;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.Calendar;

/**
 * Created by William on 2/25/2017
 */

public class Receiver extends BroadcastReceiver {
    private static final String TAG = "habitsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        ComponentName serviceComponent = new ComponentName(context, WearJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(1000); // wait at least
        builder.setOverrideDeadline(5 * 1000); // maximum delay
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        builder.setRequiresDeviceIdle(true); // device should be idle
        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());

        /*String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, DataService.class);
            context.startService(i);
        }

        final PendingIntent localPendingIntent = PendingIntent.getService(context, 0, new Intent(context, DataService.class), 0);
        final AlarmManager localAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                localAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), 5000, localPendingIntent);
            }
        };
        handler.postDelayed(r, 5000);*/
    }
}
