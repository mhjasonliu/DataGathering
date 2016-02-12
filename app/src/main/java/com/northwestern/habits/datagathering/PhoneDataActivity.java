package com.northwestern.habits.datagathering;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.northwestern.habits.datagathering.phonedata.PhoneDataService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PhoneDataActivity extends AppCompatActivity {

    public static final String STUDY_NAME_EXTRA = "studyname";

    private String studyName;
    private final String TAG = "PhoneDataActivity";

    private HashMap<String, Boolean> modes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_data);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            studyName = extras.getString(STUDY_NAME_EXTRA);

        }

        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Deactivate boxes depending on availability of sensors
        findViewById(R.id.accelBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
        findViewById(R.id.tempBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null);
        findViewById(R.id.gravBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null);
        findViewById(R.id.gyroBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
        findViewById(R.id.lightBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_LIGHT) != null);
        findViewById(R.id.linAccBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null);
        findViewById(R.id.magFieldBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);
        findViewById(R.id.barometerBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null);
        findViewById(R.id.proxBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null);
        findViewById(R.id.humidBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null);
        findViewById(R.id.rotationBox).setEnabled(
                manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);


    }


    public void startStreamPressed(View view) {
        Intent phoneIntent = new Intent(this, PhoneDataService.class);
        phoneIntent.putExtra(PhoneDataService.STUDY_ID_EXTRA, studyName);
        phoneIntent.putExtra(PhoneDataService.CONTINUE_STUDY_EXTRA, true);
        phoneIntent.putExtra(PhoneDataService.STOP_STREAM_EXTRA, false);

        phoneIntent.putExtra(PhoneDataService.ACCELEROMETER_EXTRA,
                ((CheckBox) findViewById(R.id.accelBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.TEMP_EXTRA,
                ((CheckBox) findViewById(R.id.tempBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.GRAVITY_EXTRA,
                ((CheckBox) findViewById(R.id.gravBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.GYRO_EXTRA,
                ((CheckBox) findViewById(R.id.gyroBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.LIGHT_EXTRA,
                ((CheckBox) findViewById(R.id.lightBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.LINEAR_ACCEL_EXTRA,
                ((CheckBox) findViewById(R.id.linAccBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.MAG_FIELD_EXTRA,
                ((CheckBox) findViewById(R.id.magFieldBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.PRESSURE_EXTRA,
                ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.PROXIMITY_EXTRA,
                ((CheckBox) findViewById(R.id.proxBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.HUMIDIDTY_EXTRA,
                ((CheckBox) findViewById(R.id.humidBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.ROTATION_EXTRA,
                ((CheckBox) findViewById(R.id.rotationBox)).isChecked());

        startService(phoneIntent);
    }


    public void stopStreamPressed(View view) {

        Intent phoneIntent = new Intent(this, PhoneDataService.class);
        phoneIntent.putExtra(PhoneDataService.STUDY_ID_EXTRA, studyName);
        phoneIntent.putExtra(PhoneDataService.CONTINUE_STUDY_EXTRA, true);
        phoneIntent.putExtra(PhoneDataService.STOP_STREAM_EXTRA, true);

        phoneIntent.putExtra(PhoneDataService.ACCELEROMETER_EXTRA,
                ((CheckBox) findViewById(R.id.accelBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.TEMP_EXTRA,
                ((CheckBox) findViewById(R.id.tempBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.GRAVITY_EXTRA,
                ((CheckBox) findViewById(R.id.gravBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.GYRO_EXTRA,
                ((CheckBox) findViewById(R.id.gyroBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.LIGHT_EXTRA,
                ((CheckBox) findViewById(R.id.lightBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.LINEAR_ACCEL_EXTRA,
                ((CheckBox) findViewById(R.id.linAccBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.MAG_FIELD_EXTRA,
                ((CheckBox) findViewById(R.id.magFieldBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.PRESSURE_EXTRA,
                ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.PROXIMITY_EXTRA,
                ((CheckBox) findViewById(R.id.proxBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.HUMIDIDTY_EXTRA,
                ((CheckBox) findViewById(R.id.humidBox)).isChecked());
        phoneIntent.putExtra(PhoneDataService.ROTATION_EXTRA,
                ((CheckBox) findViewById(R.id.rotationBox)).isChecked());

        startService(phoneIntent);
    }


    public void onChewClicked(View view) {
        long currentTime = System.currentTimeMillis();
        writeMoment("Chew", currentTime);
    }

    public void onHtmClicked(View view) {
        long currentTime = System.currentTimeMillis();
        writeMoment("Hand-to-mouth", currentTime);
    }

    private void writeMoment(String type, long time) {
        int studyId;
        SQLiteDatabase database =
                (new DataStorageContract.BluetoothDbHelper(getApplicationContext())).getWritableDatabase();
        try {
            studyId = getStudyId(studyName, database);
        } catch (Resources.NotFoundException e) {

            // study not found, use lowest available
            studyId = getNewStudy(database);


            // Write the study into database, save the id
            ContentValues values = new ContentValues();
            values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, studyId);
            values.put(DataStorageContract.StudyTable._ID, studyId);
            try {
                database.insertOrThrow(
                        DataStorageContract.StudyTable.TABLE_NAME,
                        null,
                        values
                );
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        Date date = new Date(time);

        Log.v(TAG, "Study name is: " + studyName);
        Log.v(TAG, "Type: " + type);
        Log.v(TAG, dateFormat.format(date));

        ContentValues values = new ContentValues();
        values.put(DataStorageContract.EatingMomentTable.COLUMN_NAME_DATETIME, dateFormat.format(date));
        values.put(DataStorageContract.EatingMomentTable.COLUMN_NAME_TYPE, type);
        values.put(DataStorageContract.EatingMomentTable.COLUMN_NAME_STUDY_ID, studyId);

        database.insert(DataStorageContract.EatingMomentTable.TABLE_NAME, null, values);
    }

    protected int getStudyId(String studyId, SQLiteDatabase db) throws Resources.NotFoundException {

        // Querry databse for the study name
        String[] projection = new String[]{
                DataStorageContract.StudyTable._ID,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID
        };

        // Query for the specified studyId
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID + "=?",
                new String[]{studyId},
                null,
                null,
                null
        );
        cursor.moveToFirst();

        if (cursor.getCount() == 0)
            throw new Resources.NotFoundException();


        int tmp = cursor.getInt(cursor.getColumnIndexOrThrow(DataStorageContract.StudyTable._ID));
        cursor.close();
        return tmp;
    }

    protected int getNewStudy(SQLiteDatabase db) {
        String[] projection = new String[]{
                DataStorageContract.StudyTable._ID,
        };

        // Get the table of studies
        Cursor cursor = db.query(
                DataStorageContract.StudyTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DataStorageContract.StudyTable._ID + " DESC",
                "1"
        );
        cursor.moveToFirst();

        // First entry
        if (cursor.getCount() == 0)
            return 0;

        // Cursor currently points at study entry with largest ID
        int studyIdCol = cursor.getColumnIndexOrThrow(
                DataStorageContract.StudyTable._ID);

        int tmp = cursor.getInt(studyIdCol) + 1;
        cursor.close();
        return tmp;
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
}
