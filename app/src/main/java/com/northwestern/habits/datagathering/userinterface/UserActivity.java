package com.northwestern.habits.datagathering.userinterface;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.northwestern.habits.datagathering.CustomDrawerListener;
import com.northwestern.habits.datagathering.DataManagementService;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.banddata.BandDataService;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(new CustomDrawerListener(this, drawer));
        }


        // Deal with the buttons
        final Button eatingButton = (Button) findViewById(R.id.button_eating);
        final Button drinkButton = (Button) findViewById(R.id.button_drinking);
        final Button nothingButton = (Button) findViewById(R.id.button_nothing);
        Button swallowButton = (Button) findViewById(R.id.button_swallow);

        assert eatingButton != null;
        assert drinkButton != null;
        assert nothingButton != null;
        assert swallowButton != null;

        // Set onclick listeners
        eatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable this button
                v.setEnabled(false);

                // Enable other buttons
                drinkButton.setEnabled(true);
                nothingButton.setEnabled(true);

                sendLabelBroadcast(DataManagementService.L_EATING);
            }
        });
        drinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable this button
                v.setEnabled(false);

                // Enable other buttons
                eatingButton.setEnabled(true);
                nothingButton.setEnabled(true);

                sendLabelBroadcast(DataManagementService.L_DRINKING);
            }
        });
        nothingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable this button
                v.setEnabled(false);

                // Enable other buttons
                drinkButton.setEnabled(true);
                eatingButton.setEnabled(true);

                sendLabelBroadcast(DataManagementService.L_NOTHING);
            }
        });

        swallowButton.setOnClickListener(new View.OnClickListener() {
            private boolean isSwallowing = false;
            private int previousState = 0;

            Drawable originalBackground;

            @Override
            public void onClick(View v) {
                if (isSwallowing) {
                    // Return background to original state
                    v.setBackground(originalBackground);

                    // Reenable the buttons
                    eatingButton.setEnabled(previousState != DataManagementService.L_EATING);
                    drinkButton.setEnabled(previousState != DataManagementService.L_DRINKING);
                    nothingButton.setEnabled(previousState != DataManagementService.L_NOTHING);

                    // Send new label and go to not swallowing
                    sendLabelBroadcast(previousState);
                    isSwallowing = false;
                } else {
                    // Switch the background, saving the original
                    originalBackground = v.getBackground();
                    Drawable newBackground = originalBackground.getConstantState().newDrawable();
                    newBackground.setColorFilter(Color.CYAN, PorterDuff.Mode.DARKEN);
                    v.setBackground(newBackground);

                    // Remember the last label
                    if (!eatingButton.isEnabled()) {previousState = DataManagementService.L_EATING;}
                    else if (!drinkButton.isEnabled()) {previousState = DataManagementService.L_DRINKING;}
                    else if (!nothingButton.isEnabled()) {previousState = DataManagementService.L_NOTHING;}

                    // Disable the other buttons
                    eatingButton.setEnabled(false);
                    drinkButton.setEnabled(false);
                    nothingButton.setEnabled(false);

                    // Broadcast the new label and save the state
                    sendLabelBroadcast(DataManagementService.L_SWALLOW);
                    isSwallowing = true;
                }

            }
        });


        // Set enabled status
        int label = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(Preferences.LABEL, DataManagementService.L_NOTHING);
        switch (label) {
            case DataManagementService.L_EATING:
                eatingButton.callOnClick();
                break;
            case DataManagementService.L_DRINKING:
                eatingButton.callOnClick();
                break;
            case DataManagementService.L_NOTHING:
                eatingButton.callOnClick();
                break;
            case DataManagementService.L_SWALLOW:
                eatingButton.callOnClick();
                break;
            default:
                Log.e(TAG, "Unrecognized label stored in preferences");
        }
    }

    private void sendLabelBroadcast(int label) {
        // Store label in preferences
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putInt(DataManagementService.USER_ID, label);
        Intent i = new Intent(BandDataService.ACTION_LABEL);
        i.putExtra(BandDataService.LABEL_EXTRA, label);
        this.sendBroadcast(i);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private static boolean storagePermitted(Activity activity) {

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&

                ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

            return true;

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

        return false;

    }

}
