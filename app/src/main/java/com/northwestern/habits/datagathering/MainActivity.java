package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Created main activity");
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


    // TODO add a way to update the list of info
    /********************* STRUCTURES FOR BLUETOOTH DEVICES ************************/



}
