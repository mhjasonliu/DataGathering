package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.northwestern.habits.datagathering.DataStorageContract.UserTable;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Created main activity");

        Log.v(TAG, "Creating database");
        mDbHelper = new DataStorageContract.BluetoothDbHelper(getApplicationContext());
    }



    /******************************* BUTTON HANDLERS *******************************/
    private final String TAG = "Main activity"; //For logs
    private final int REQUEST_ENABLE_BT = 1;

    public void manageDevicesClicked(View view) {
        Log.v(TAG, "Device Management button clicked");

        // Check for bluetooth connection
        BluetoothAdapter adapter = BluetoothConnectionLayer.getAdapter();
        Log.v(TAG, "Checked the adapter: ");
        if (adapter == null) {
            //TODO popup warning and cancel the activity
            Log.v(TAG, "No adapter found");
        } else {
            Log.v(TAG, "About to check enabled");
            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Log.v(TAG, "About to check enabled again");
            if (adapter.isEnabled()) {
                BluetoothConnectionLayer.refreshPairedDevices();
                Log.v(TAG, "Refreshed paired devices.");
                BluetoothConnectionLayer.refreshNearbyDevices();
                Intent devManagementIntent = new Intent(this, DeviceManagementActivity.class);
                startActivity(devManagementIntent);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Recall device management
                manageDevicesClicked(findViewById(R.id.manageDevicesButton));
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
        new DataStorageContract.DisplayDatabaseTask().execute(db);
    }


}
