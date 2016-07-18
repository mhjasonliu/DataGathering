package com.northwestern.habits.datagathering.userinterface;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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

import com.couchbase.lite.replicator.Replication;
import com.northwestern.habits.datagathering.CouchBaseData;
import com.northwestern.habits.datagathering.CustomDrawerListener;
import com.northwestern.habits.datagathering.R;

import java.net.MalformedURLException;
import java.net.URL;

public class UserActivity extends AppCompatActivity {

    private Replication push;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    Snackbar.make(view, "Syncing database with back end", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    if (push == null) {
                        URL url = null;
                        try {
                            url = new URL("http://107.170.25.202:4984/db/");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
//                        Database database = CouchBaseData.getDatabaseInstance(UserActivity.this);
                        try {
                            push = CouchBaseData.getReplicationInstance(UserActivity.this);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        push.setContinuous(false);
//                    Authenticator auth = new BasicAuthenticator(username, password);
//                    push.setAuthenticator(auth);

                        push.addChangeListener(new Replication.ChangeListener() {
                            @Override
                            public void changed(Replication.ChangeEvent event) {
                                // will be called back when the push replication status changes
                                String message = "Completed " + event.getCompletedChangeCount()
                                        + " out of " + event.getCompletedChangeCount() + " "
                                        + push.getStatus().toString();
                                Log.v("Replication", message);
                                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();

                                // Check for an error
                                Throwable error = event.getError();
                                if (error != null) {
                                    error.printStackTrace();
                                }
                            }
                        });

                    }
//                    Snackbar.make(view, "Status: " + push.getStatus(), Snackbar.LENGTH_SHORT).show();
                    if (push.getStatus() != Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                        push.start();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Snackbar.make(view, "Status: " + push.getStatus(), Snackbar.LENGTH_SHORT).show();

                    if (storagePermitted(UserActivity.this)) {
                        CouchBaseData.exportToCsv("asdf", getApplicationContext());
                    }
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(new CustomDrawerListener(this, drawer));
        }
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
