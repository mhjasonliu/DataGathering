package com.northwestern.habits.datagathering.userinterface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.northwestern.habits.datagathering.Preferences;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If there is a password set, open to UserActivity, else AdvancedSettings
        SharedPreferences prefs = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);

        Intent i;
        if (prefs.getString(Preferences.PASSWORD, "").equals("")) {
            i = new Intent(this, AdvancedSettingsActivity.class);
        } else {
            i = new Intent (this, UserActivity.class);
        }
        startActivity(i);
    }

}
