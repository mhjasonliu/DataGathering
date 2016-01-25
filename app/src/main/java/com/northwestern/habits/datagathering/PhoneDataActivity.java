package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

import com.northwestern.habits.datagathering.phonedata.PhoneDataService;

import java.util.HashMap;

public class PhoneDataActivity extends AppCompatActivity {

    public static final String STUDY_NAME_EXTRA = "studyname";

    private String studyName;

    private HashMap<String,Boolean> modes = new HashMap<>();

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

    }


    public void onChewClicked(View view) {
        long currentTime = System.currentTimeMillis();

    }

    public void onHtmClicked(View view) {
        long currentTime = System.currentTimeMillis();

    }

}
