package com.northwestern.habits.datagathering;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {


    private final String TAG = "Main activity"; //For logs


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Creating database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());
    }



    /******************************* BUTTON HANDLERS *******************************/
    private final int REQUEST_ENABLE_BT = 1;

    public void startStudyClicked(View view) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Please enter a unique study name (note, this will end the current study)");
        final EditText input = new EditText(this);
        b.setView(input);
        b.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent devManagementIntent = new Intent(
                        getApplicationContext(), DeviceManagementActivity.class);
                devManagementIntent.putExtra("name", input.getText().toString());
                devManagementIntent.putExtra("continueStudy", false);
                startDevMgmtActivity(devManagementIntent);
            }
        });
        b.setNegativeButton("CANCEL", null);
        b.create().show();
    }

    public void manageStudyClicked(View view) {
        startDevMgmtActivity(null);
    }

    public void endStudyClicked(View view) {
        // Start Service with end study intent
        Intent endStudyIntent = new Intent(this,
                BandDataService.class);
        endStudyIntent.putExtra("continueStudy", false);
        startService(endStudyIntent);
    }


    private void startDevMgmtActivity( Intent intent ) {

        if (intent != null ) {
            Log.e(TAG, "StartDevMgmtActivity not implemented for that button.");
        } else {
            if (btEnabled()) {
                locEnabled();
            }
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
                startDevMgmtActivity(null);
            }
        }
    }


    private final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 0;
    private void locEnabled( ) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
        } else {
            onRequestPermissionsResult(MY_PERMISSIONS_REQUEST_COARSE_LOCATION,
                    new String[]{}, new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.v(TAG, "Got permission result");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent devManagementIntent = new Intent(
                            getApplicationContext(), DeviceManagementActivity.class);
                    devManagementIntent.putExtra(DeviceManagementActivity.CONT_STUDY_EXTRA, true);
                    devManagementIntent.putExtra(DeviceManagementActivity.BT_LE_EXTRA, true);
                    startActivity(devManagementIntent);

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
    private int entryId = 0;
    DataStorageContract.BluetoothDbHelper mDbHelper;
    private Cursor cursor;

    public void onDeleteDatabase( View view ) {
        db = mDbHelper.getWritableDatabase();

        mDbHelper.onUpgrade(db, db.getVersion(), db.getVersion()+1);
    }


    public void onReadDatabase(View view) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Log.v(TAG, "displaying database");
        //new DataStorageContract.DisplayDatabaseTask().execute(db);
    }


}
