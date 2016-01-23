package com.northwestern.habits.datagathering;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.northwestern.habits.datagathering.phonedata.PhoneDataService;

public class PhoneDataActivity extends AppCompatActivity {

    public static final String STUDY_NAME_EXTRA = "studyname";

    private String studyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_data);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            studyName = extras.getString(STUDY_NAME_EXTRA);

        }
    }


    public void startStreamPressed(View view) {
        Intent phoneIntent = new Intent(this, PhoneDataService.class);
        phoneIntent.putExtra(PhoneDataService.STUDY_ID_EXTRA, studyName);
        phoneIntent.putExtra(PhoneDataService.CONTINUE_STUDY_EXTRA, true);

        startService(phoneIntent);
    }


    public void stopStreamPressed(View view) {

    }



}
