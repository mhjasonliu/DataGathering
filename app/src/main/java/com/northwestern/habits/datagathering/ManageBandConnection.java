package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;


public class ManageBandConnection extends AppCompatActivity {

    private final String TAG = "ManageBandConnection";

    private int index;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_band_connection);

        // Extract the Band for the device
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            index = extras.getInt("Index");
            Log.v(TAG, "Loaded band info");
        }

    }

    public void onStreamClick(View view) {
        // Create the intent
        Intent managementIntent = new Intent(this, BandDataService.class);
        managementIntent.putExtra("stopStream", false);
        managementIntent.putExtra("index", index);
        managementIntent.putExtra("accelerometer", (Boolean) ((CheckBox) findViewById(R.id.accelerometerBox)).isChecked());
        managementIntent.putExtra("altimeter", (Boolean) ((CheckBox) findViewById(R.id.altimeterBox)).isChecked());
        managementIntent.putExtra("ambient light", (Boolean) ((CheckBox) findViewById(R.id.ambientBox)).isChecked());
        managementIntent.putExtra("barometer", (Boolean) ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        managementIntent.putExtra("gsr", (Boolean) ((CheckBox) findViewById(R.id.gsrBox)).isChecked());
        managementIntent.putExtra("heart rate", (Boolean) ((CheckBox) findViewById(R.id.heartRateBox)).isChecked());

        managementIntent.putExtra("userId", ((EditText) findViewById(R.id.userIdField)).getText().toString());

        managementIntent.putExtra("continueStudy", true);

        startService(managementIntent);
    }

    public void onStopStreamClick (View viewl) {
        // Create the intent
        Intent managementIntent = new Intent(this, BandDataService.class);
        managementIntent.putExtra("stopStream", true);
        managementIntent.putExtra("index", index);
        managementIntent.putExtra("accelerometer", (Boolean) ((CheckBox) findViewById(R.id.accelerometerBox)).isChecked());
        managementIntent.putExtra("altimeter", (Boolean) ((CheckBox) findViewById(R.id.altimeterBox)).isChecked());
        managementIntent.putExtra("ambient light", (Boolean) ((CheckBox) findViewById(R.id.ambientBox)).isChecked());
        managementIntent.putExtra("barometer", (Boolean) ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        managementIntent.putExtra("gsr", (Boolean) ((CheckBox) findViewById(R.id.gsrBox)).isChecked());
        managementIntent.putExtra("heart rate", (Boolean) ((CheckBox) findViewById(R.id.heartRateBox)).isChecked());

        managementIntent.putExtra("userId", ((EditText) findViewById(R.id.userIdField)).getText().toString());

        managementIntent.putExtra("continueStudy", true);

        startService(managementIntent);
    }

}
