package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;


public class ManageBandConnection extends AppCompatActivity implements HeartRateConsentListener {

    private final String TAG = "ManageBandConnection";

    protected static final String INDEX_EXTRA = "index";

    private int index;
    private boolean waitForHeartRate = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_band_connection);

        // Extract the Band for the device
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            index = extras.getInt(INDEX_EXTRA);
            Log.v(TAG, "Loaded band info");
        }

    }

    public void onStreamClick(View view) {
        // Create the intent
        Intent managementIntent = makeIntent();

        managementIntent.putExtra(BandDataService.STOP_STREAM_EXTRA, false);
        managementIntent.putExtra(BandDataService.CONTINUE_STUDY_EXTRA, true);

        if (!waitForHeartRate)
            startService(managementIntent);
    }

    public void onStopStreamClick (View viewl) {
        // Create the intent
        Intent managementIntent = makeIntent();

        managementIntent.putExtra(BandDataService.STOP_STREAM_EXTRA, true);
        managementIntent.putExtra("continueStudy", true);

        startService(managementIntent);
    }

    private Intent makeIntent() {

        Intent intent = new Intent(this, BandDataService.class);
        intent.putExtra(BandDataService.INDEX_EXTRA, index);
        intent.putExtra(BandDataService.ACCEL_REQ_EXTRA, (Boolean) ((CheckBox) findViewById(R.id.accelerometerBox)).isChecked());
        intent.putExtra(BandDataService.ALT_REQ_EXTRA, (Boolean) ((CheckBox) findViewById(R.id.altimeterBox)).isChecked());
        intent.putExtra(BandDataService.AMBIENT_REQ_EXTRA, (Boolean) ((CheckBox) findViewById(R.id.ambientBox)).isChecked());
        intent.putExtra(BandDataService.BAROMETER_REQ_EXTRA, (Boolean) ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        intent.putExtra(BandDataService.GSR_REQ_EXTRA, (Boolean) ((CheckBox) findViewById(R.id.gsrBox)).isChecked());

        com.microsoft.band.sensors.BandSensorManager manager =
                BandClientManager.getInstance().create(this,
                        BandClientManager.getInstance().getPairedBands()[index]).getSensorManager();

        if (((CheckBox) findViewById(R.id.heartRateBox)).isChecked()
                && manager.getCurrentHeartRateConsent() != UserConsent.GRANTED) {
            // Prevent calling before heart rate request
            waitForHeartRate = true;

            Log.v(TAG, "Waiting for heart rate request");
            // Get permission
            manager.requestHeartRateConsent(this, this);


        } else {
            intent.putExtra(BandDataService.HEART_RATE_REQ_EXTRA, true);
            waitForHeartRate = false;
        }

        intent.putExtra(BandDataService.STUDY_ID_EXTRA, ((EditText) findViewById(R.id.studyIdField)).getText().toString());
        return intent;
    }

    @Override
    public void userAccepted(boolean b) {
        Log.v(TAG, "User accepted heart rate request");
    }
}
