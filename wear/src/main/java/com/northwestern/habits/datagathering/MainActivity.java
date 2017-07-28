package com.northwestern.habits.datagathering;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private TextView mTextView;
    private int REQUEST = 101;
    private int ONGOING_NOTIFICATION_ID = 003;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS);
        if (permissionCheck == -1) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST);
        }

        Intent intent = new Intent(this, DataService.class);
        startService(intent);


//        final AlarmManager localAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        final PendingIntent localPendingIntent = PendingIntent.getService(this , 0, new Intent(this, DataService.class), 0);
//        final Runnable r = new Runnable() {
//            public void run() {
//                localAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), 240000, localPendingIntent);
//            }
//        };
//        handler.postDelayed(r, 6000);
    }
}
