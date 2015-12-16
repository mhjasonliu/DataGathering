package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

        if (btEnabled()) {
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
                    startActivity(devManagementIntent);
                }
            });
            b.setNegativeButton("CANCEL", null);
            b.create().show();
        }
    }

    public void manageStudyClicked(View view) {

        if (btEnabled()) {
            Intent devManagementIntent = new Intent(
                    getApplicationContext(), DeviceManagementActivity.class);
            devManagementIntent.putExtra("continueStudy", true);
            startActivity(devManagementIntent);
        }
    }

    public void endStudyClicked(View view) {
        // Start Service with end study intent
        Intent endStudyIntent = new Intent(this,
                BandDataService.class);
        endStudyIntent.putExtra("continueStudy", false);
        startService(endStudyIntent);
    }

    private boolean btEnabled() {
        Log.v(TAG, "Continue Study button clicked");

        // Check for bluetooth connection
        BluetoothAdapter adapter = BluetoothConnectionLayer.getAdapter();
        Log.v(TAG, "Checked bluetooth adapter: ");
        if (adapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("No Bluetooth")
                    .setMessage("There is no bluetooth adapter for this device. To manage bluetooth" +
                            "device connections, please run this app on a device with bluetooth support.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            Log.v(TAG, "No adapter found");
        } else {
            Log.v(TAG, "About to check enabled");
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
                // Recall device management
                startStudyClicked(findViewById(R.id.manageDevicesButton));
            }
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

/*
    public void onEnterDatabase(View view) {
        // Gets the data repository in write mode
        db = mDbHelper.getWritableDatabase();

// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(DataStorageContract.UserTable.COLUMN_NAME_ENTRY_ID, entryId);
        entryId++;
        values.put(UserTable.COLUMN_NAME_TITLE, "This is an entry!");
        //values.put(UserTable.COLUMN_NAME_CONTENT, "More content, YaY!");

// Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                DataStorageContract.UserTable.TABLE_NAME,
                null,
                values);
    }
*/


    public void onReadDatabase(View view) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Log.v(TAG, "displaying database");
        //new DataStorageContract.DisplayDatabaseTask().execute(db);
    }


}
