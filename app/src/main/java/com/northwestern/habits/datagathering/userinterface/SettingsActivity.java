package com.northwestern.habits.datagathering.userinterface;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;

/**
 * Created by Y.Misal on 29/5/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private Toolbar mToolBar = null;
    private CheckBox mCheckBox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_menu);

        mToolBar  = (Toolbar)findViewById(R.id.toolbar);
        //--------------Set toolbar title---------------//

        mToolBar.setTitle("Settings");
        setSupportActionBar(mToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mCheckBox = (CheckBox) findViewById(R.id.checkBox);

        boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Preferences.IS_ALLOW_KEEP, false);
        mCheckBox.setChecked(isChecked);

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Set Allow or not to keep the files preference in this process
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                        .putBoolean(Preferences.IS_ALLOW_KEEP, isChecked).apply();
                Log.e(TAG, " ALLOW: " + isChecked);
            }
        });
    }

}
