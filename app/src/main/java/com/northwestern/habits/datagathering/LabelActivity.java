package com.northwestern.habits.datagathering;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LabelActivity extends Activity {

    private final String TAG = "LabelActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);
    }


    public void onNotEatingClicked(View view) {
        long currentTime = System.currentTimeMillis();
        writeMoment("StopEating", currentTime);
        findViewById(R.id.notEatingButton).setEnabled(false);
        findViewById(R.id.eatingButton).setEnabled(true);
    }

    private void writeMoment(String type, long time) {
        SQLiteDatabase database =
                (DataStorageContract.BluetoothDbHelper.getInstance(this.getApplicationContext())
                        .getWritableDatabase());


        // Write the study into database, save the id
        ContentValues values = new ContentValues();
        values.put(DataStorageContract.EatingMomentTable.COLUMN_NAME_TYPE, type);
        values.put(DataStorageContract.EatingMomentTable.COLUMN_NAME_DATETIME, dateFormat.format(time));
        try {
            database.insertOrThrow(
                    DataStorageContract.EatingMomentTable.TABLE_NAME,
                    null,
                    values
            );
        } catch (SQLException e1) {
            e1.printStackTrace();
        }


        Date date = new Date(time);

        Log.v(TAG, "Type: " + type);
        Log.v(TAG, dateFormat.format(date));


        // Insert into csv file
        File folder = new File(Environment.getExternalStorageDirectory() + "/Labels");
        if (!folder.exists()) {
            folder.mkdir();
        }
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(cal.getTime());
        final String filename = folder.toString() + "/" + "Labels " + formattedDate
                + ".csv";

        File file = new File(filename);

        // If file does not exists, then create it
        boolean fpExists = true;
        if (!file.exists()) {
            try {
                boolean fb = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fpExists = false;
        }

        // Post data to the csv
        FileWriter fw;
        try {
            fw = new FileWriter(filename, true);
            if (!fpExists) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                this.sendBroadcast(intent);
                fw.append("Label,Time\n");
            }
            fw.append(type);
            fw.append(',');
            fw.append(dateFormat.format(time));
            fw.append('\n');
            fw.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write to csv");
            e.printStackTrace();
        }
    }

    public void onEatingClicked(View view) {
        findViewById(R.id.eatingButton).setEnabled(false);
        findViewById(R.id.notEatingButton).setEnabled(true);
        long currentTime = System.currentTimeMillis();
        writeMoment("StartEating", currentTime);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    public void onContinue(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }
}
