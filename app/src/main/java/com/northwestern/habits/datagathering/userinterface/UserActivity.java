package com.northwestern.habits.datagathering.userinterface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
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
import android.widget.TextView;

import com.northwestern.habits.datagathering.CustomDrawerListener;
import com.northwestern.habits.datagathering.MyReceiver;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.banddata.BandDataService;
import com.northwestern.habits.datagathering.database.DataManagementService;

import java.util.concurrent.TimeUnit;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private DbUpdateReceiver updateReceiver = new DbUpdateReceiver();

    private Notification eatingNotification = null;

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

        if (!updateReceiver.registered) {
            registerReceiver(updateReceiver, new IntentFilter(DbUpdateReceiver.ACTION_DB_STATUS));
            updateReceiver.registered = true;
        }

        TextView t = ((TextView) findViewById(R.id.db_status_Text));
        if (t != null) {
            if (MyReceiver.isCharging(this) && MyReceiver.isWifiConnected(this)) {
                t.setText(DbUpdateReceiver.STATUS_SYNCING);
            } else {
                t.setText(DbUpdateReceiver.STATUS_UNKNOWN);
            }
        }

        // Deal with the buttons
        final Button eatingButton = (Button) findViewById(R.id.button_eating);
        final Button drinkButton = (Button) findViewById(R.id.button_drinking);
        final Button nothingButton = (Button) findViewById(R.id.button_nothing);
        final Button swallowButton = (Button) findViewById(R.id.button_swallow);

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

                if (mDelayedNotification == null) {
                    mDelayedNotification = new DelayedNotificationThread();
                    mDelayedNotification.start();
                }
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

                if (mDelayedNotification == null) {
                    mDelayedNotification = new DelayedNotificationThread();
                    mDelayedNotification.start();
                }
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

                if (mDelayedNotification != null) {
                    mDelayedNotification.interrupt();
                }

                // Dismiss the notification if it exists
                NotificationManager notificationManager = (NotificationManager) getBaseContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(LABEL_NOTIFICATION_ID);
            }
        });

        swallowButton.setOnClickListener(new View.OnClickListener() {
            private boolean isSwallowing = false;
            private int previousState = DataManagementService.L_EATING;

            Drawable originalBackground;

            @Override
            public void onClick(final View v) {
                // Reenable the buttons after a delay
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Return background to original state
                        v.setBackground(originalBackground);

                        eatingButton.setEnabled(previousState != DataManagementService.L_EATING);
                        drinkButton.setEnabled(previousState != DataManagementService.L_DRINKING);
                        nothingButton.setEnabled(previousState != DataManagementService.L_NOTHING);
                        swallowButton.setEnabled(true);

                        // Send new label and go to not swallowing
                        sendLabelBroadcast(previousState);
                        isSwallowing = false;
                    }
                }, 2000);

                // Switch the background, saving the original
                originalBackground = v.getBackground();
                Drawable newBackground = originalBackground.getConstantState().newDrawable();
                newBackground.setColorFilter(Color.CYAN, PorterDuff.Mode.DARKEN);
                v.setBackground(newBackground);

                // Remember the last label
                if (!eatingButton.isEnabled()) {
                    previousState = DataManagementService.L_EATING;
                } else if (!drinkButton.isEnabled()) {
                    previousState = DataManagementService.L_DRINKING;
                } else if (!nothingButton.isEnabled()) {
                    previousState = DataManagementService.L_NOTHING;
                }

                // Disable the other buttons
                eatingButton.setEnabled(false);
                drinkButton.setEnabled(false);
                nothingButton.setEnabled(false);

                // Broadcast the new label and save the state
                sendLabelBroadcast(DataManagementService.L_SWALLOW);
                isSwallowing = true;
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
                drinkButton.callOnClick();
                break;
            case DataManagementService.L_NOTHING:
                nothingButton.callOnClick();
                break;
            case DataManagementService.L_SWALLOW:
                swallowButton.callOnClick();
                break;
            default:
                Log.e(TAG, "Unrecognized label stored in preferences");
        }
    }

    private void sendLabelBroadcast(int label) {
        // Store label in preferences
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putInt(Preferences.LABEL, label).apply();
        Intent i = new Intent(BandDataService.ACTION_LABEL);
        i.putExtra(BandDataService.LABEL_EXTRA, label);
        this.sendBroadcast(i);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public class DbUpdateReceiver extends BroadcastReceiver {
        public boolean registered = false;
        public static final String ACTION_DB_STATUS = "db status";
        public static final String STATUS_EXTRA = "status";

        /**
         * String to be passed as extra indicating that replications are not running
         */
        public static final String STATUS_UNKNOWN = "Unknown... Plug in and connect to wifi";
        /**
         * String to be passed as extra indicating that:
         * - A sync is in progress
         * OR
         * - the last sync pushed data
         */
        public static final String STATUS_SYNCING = "Syncing...";
        /**
         * String to be passed as extra indicating that The last sync ended made no update
         */
        public static final String STATUS_SYNCED = "Sync complete";
        /**
         * String to be passed as extra indicating that the replication encountered an error during
         * the last sync
         */
        public static final String STATUS_DB_ERROR = "SERVER ISSUE DETECTED";

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView statusText = ((TextView) UserActivity.this.findViewById(R.id.db_status_Text));
            if (statusText != null) {
                statusText.setText(
                        intent.getStringExtra(STATUS_EXTRA)
                );
            }
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

    @Override
    protected void onDestroy() {
        if (updateReceiver.registered) {
            unregisterReceiver(updateReceiver);
            updateReceiver.registered = false;
        }
        super.onDestroy();
    }

    private DelayedNotificationThread mDelayedNotification = null;
    private final int LABEL_NOTIFICATION_ID = 123;

    private class DelayedNotificationThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(TimeUnit.HOURS.toMillis(1));
                if (!Thread.interrupted()) {
                    Uri alarmsound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Notification.Builder mBuilder =
                            new Notification.Builder(UserActivity.this)
                                    .setSmallIcon(android.R.drawable.button_onoff_indicator_on)
                                    .setContentTitle("Forgot to turn off your label?")
                                    .setStyle(new Notification.BigTextStyle().bigText(
                                            "An hour ago you labeled that you were " +
                                                    "eating or drinking. Please make sure you mark " +
                                                    "when you are done!"))
                                    .setVibrate(new long[]{0, 500})
                                    .setSound(alarmsound);
                    Intent resultIntent = new Intent(UserActivity.this, UserActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(UserActivity.this);
                    // Adds the back stack for the Intent (but not the Intent itself)
                    stackBuilder.addParentStack(UserActivity.class);
                    // Adds the Intent that starts the Activity to the top of the stack
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    // mId allows you to update the notification later on.
                    mNotificationManager.notify(LABEL_NOTIFICATION_ID, mBuilder.build());
                }
            } catch (InterruptedException ignored) {
            }
            mDelayedNotification = null;
            this.interrupt();
        }
    }
}
