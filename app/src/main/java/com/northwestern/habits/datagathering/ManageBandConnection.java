package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;


public class ManageBandConnection extends AppCompatActivity implements HeartRateConsentListener,
        AdapterView.OnItemSelectedListener {

    private final String TAG = "ManageBandConnection";

    protected static final String INDEX_EXTRA = "index";
    protected static final String STUDY_NAME_EXTRA = "studyName";

    private Spinner locationSpinner;

    private int index;
    private String studyName;
    private String location = "inner-left";
    private boolean waitForHeartRate = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_band_connection);


        // Extract the Band for the device
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            index = extras.getInt(INDEX_EXTRA);
            studyName = extras.getString(STUDY_NAME_EXTRA);
            Log.v(TAG, "Loaded band info");
        }


        // Prepare the spinner
        locationSpinner = (Spinner) findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.locations_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        locationSpinner.setAdapter(adapter);

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
        intent.putExtra(BandDataService.ACCEL_REQ_EXTRA,
                (Boolean) ((CheckBox) findViewById(R.id.accelerometerBox)).isChecked());
        intent.putExtra(BandDataService.ALT_REQ_EXTRA,
                (Boolean) ((CheckBox) findViewById(R.id.altimeterBox)).isChecked());
        intent.putExtra(BandDataService.AMBIENT_REQ_EXTRA,
                (Boolean) ((CheckBox) findViewById(R.id.ambientBox)).isChecked());
        intent.putExtra(BandDataService.BAROMETER_REQ_EXTRA,
                (Boolean) ((CheckBox) findViewById(R.id.barometerBox)).isChecked());
        intent.putExtra(BandDataService.GSR_REQ_EXTRA,
                (Boolean) ((CheckBox) findViewById(R.id.gsrBox)).isChecked());
        intent.putExtra(BandDataService.LOCATION_EXTRA,
                location);
        intent.putExtra(BandDataService.STUDY_ID_EXTRA, studyName);

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

        return intent;
    }

    @Override
    public void userAccepted(boolean b) {
        Log.v(TAG, "User accepted heart rate request");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        location = (String) parent.getItemAtPosition(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
