package com.northwestern.habits.datagathering;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {


    private final String TAG = "Main activity"; //For logs

    private boolean waitingToContinue = false;
    private Intent newStudyIntent;

    private Button newStudyButton;
    private Button editStudyButton;
    private Button endStudyButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newStudyButton = (Button) findViewById(R.id.startStudyButton);
        editStudyButton = (Button) findViewById(R.id.manageStudyButton);
        endStudyButton = (Button) findViewById(R.id.endStudyButton);

        Log.v(TAG, "Creating database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        Window window = getWindow();
// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
// finally change the color
        window.setStatusBarColor(getResources().getColor(R.color.NorthwesternPurple));
    }



    /******************************* BUTTON HANDLERS *******************************/
    private final int REQUEST_ENABLE_BT = 1;

    public void startStudyClicked(View view) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Please enter a unique study name \n(note: this will end the current study)");
        final EditText input = new EditText(this);
        b.setView(input);
        b.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                waitingToContinue = true;

                // Kill the old study
                endStudyClicked(endStudyButton);

                editStudyButton.setEnabled(true);
                endStudyButton.setEnabled(true);

                newStudyIntent = new Intent(
                        getApplicationContext(), DeviceManagementActivity.class);
                newStudyIntent.putExtra(DeviceManagementActivity.STUDY_NAME_EXTRA,
                        input.getText().toString());
                newStudyIntent.putExtra(DeviceManagementActivity.CONT_STUDY_EXTRA, false);
                startDevMgmtActivity();
            }
        });
        b.setNegativeButton("CANCEL", null);
        b.create().show();
    }

    public void manageStudyClicked(View view) {
        startDevMgmtActivity();
    }

    public void endStudyClicked(View view) {
        // TODO Add warning to confirm with user

        // Start Service with end study intent
        Intent endStudyIntent = new Intent(this,
                BandDataService.class);
        endStudyIntent.putExtra(DeviceManagementActivity.CONT_STUDY_EXTRA, false);
        startService(endStudyIntent);

        endStudyButton.setEnabled(false);
        editStudyButton.setEnabled(false);
    }


    private void startDevMgmtActivity() {
        if (btEnabled()) {
            locEnabled();
        }
    }




    private boolean btEnabled() {
        // Check for bluetooth connection
        BluetoothAdapter adapter = BluetoothConnectionLayer.getAdapter();
        if (adapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("No Bluetooth")
                    .setMessage("There is no bluetooth adapter for this device. To manage bluetooth" +
                            "device connections, please run this app on a device with bluetooth support.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            Log.v(TAG, "No adapter found");
        } else {
            Log.v(TAG, "About to check bluetooth enabled");
            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Log.v(TAG, "About to check enabled again");
            if (adapter.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Recall startMgmtActivity
                startDevMgmtActivity();
            }
        }
    }


    private final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 0;
    private void locEnabled( ) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Coarse location permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
        } else {
            // Permission was previously granted
            onRequestPermissionsResult(MY_PERMISSIONS_REQUEST_COARSE_LOCATION,
                    new String[]{}, new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.v(TAG, "Got permission result");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (waitingToContinue) {
                        newStudyIntent.putExtra(DeviceManagementActivity.BT_LE_EXTRA, true);
                    } else {
                        newStudyIntent = new Intent(
                                getApplicationContext(), DeviceManagementActivity.class);
                        newStudyIntent.putExtra(DeviceManagementActivity.CONT_STUDY_EXTRA, true);
                        newStudyIntent.putExtra(DeviceManagementActivity.BT_LE_EXTRA, true);
                    }
                    startActivity(newStudyIntent);

                } else {
                    // Start a device management activity without bluetooth le

                    Intent devManagementIntent = new Intent(
                            getApplicationContext(), DeviceManagementActivity.class);
                    devManagementIntent.putExtra(DeviceManagementActivity.CONT_STUDY_EXTRA, true);
                    devManagementIntent.putExtra(DeviceManagementActivity.BT_LE_EXTRA, false);
                    startActivity(devManagementIntent);

                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /* ******************************** DATABASE TESTING ********************************* */
    public SQLiteDatabase db;
    DataStorageContract.BluetoothDbHelper mDbHelper;

    public void onDeleteDatabase( View view ) {
        db = mDbHelper.getWritableDatabase();

        mDbHelper.onUpgrade(db, db.getVersion(), db.getVersion() + 1);
    }




}
