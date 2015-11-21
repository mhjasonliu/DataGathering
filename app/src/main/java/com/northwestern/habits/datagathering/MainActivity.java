package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.microsoft.band.BandException;
import com.northwestern.habits.datagathering.DataStorageContract.FeedEntry;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Created main activity");

        Log.v(TAG, "Creating database");
        mDbHelper = new DataStorageContract.FeedReaderDbHelper(getApplicationContext());
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
    DataStorageContract.FeedReaderDbHelper mDbHelper;
    private Cursor cursor;

    public void onDeleteDatabase( View view ) {
        db = mDbHelper.getReadableDatabase();

        mDbHelper.onUpgrade(db, db.getVersion(), db.getVersion()+1);
    }

    public void onEnterDatabase(View view) {
        // Gets the data repository in write mode
        db = mDbHelper.getWritableDatabase();

// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_ENTRY_ID, entryId);
        entryId++;
        values.put(FeedEntry.COLUMN_NAME_TITLE, "This is an entry!");
        //values.put(FeedEntry.COLUMN_NAME_CONTENT, "More content, YaY!");

// Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                FeedEntry.TABLE_NAME,
                null,
                values);
    }

    public void onReadDatabase(View view) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

// Define a projection that specifies which columns from the database
// you will actually use after this query.
        String[] projection = {
                FeedEntry._ID,
                FeedEntry.COLUMN_NAME_ENTRY_ID,
                FeedEntry.COLUMN_NAME_TITLE,
                //FeedEntry.COLUMN_NAME_CONTENT
        };

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                FeedEntry.COLUMN_NAME_ENTRY_ID + " DESC";

        cursor = db.query(
                FeedEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        Log.v(TAG, "About to log the entry database stuff");
        new DataStorageContract.DisplayDatabaseTask().execute(cursor);
    }


}
