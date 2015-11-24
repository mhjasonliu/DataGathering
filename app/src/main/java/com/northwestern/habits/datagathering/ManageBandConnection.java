package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.SampleRate;

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
        managementIntent.putExtra("index", index);
        managementIntent.putExtra("accelerometer", (Boolean) ((CheckBox) findViewById(R.id.accelerometerBox)).isChecked());
        managementIntent.putExtra("altimeter", (Boolean) ((CheckBox) findViewById(R.id.altimeterBox)).isChecked());
        managementIntent.putExtra("ambient light", (Boolean) ((CheckBox) findViewById(R.id.ambientBox)).isChecked());
        managementIntent.putExtra("barometer", (Boolean) ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        managementIntent.putExtra("gsr", (Boolean) ((CheckBox) findViewById(R.id.gsrBox)).isChecked());
        managementIntent.putExtra("heart rate", (Boolean) ((CheckBox) findViewById(R.id.heartRateBox)).isChecked());

        managementIntent.putExtra("userId", ((EditText) findViewById(R.id.userIdField)).getText().toString());

        startService(managementIntent);
    }

}
