package com.northwestern.habits.datagathering;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ManageConnectionActivity extends AppCompatActivity {
    private String address = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_connection);

        // Extract the macAddress for the device
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            address = extras.getString("MAC");
        }

        ((TextView) findViewById(R.id.nameText)).setText(address);
    }

    public void getTypeClicked(View view) {
        TextView typeText = (TextView) findViewById(R.id.typeText);

        // Query device for its type and display to type text
        try {
            typeText.setText(
                    BluetoothConnectionLayer.getAdapter().getRemoteDevice(address).getClass().toString()
            );
        } catch (Exception e) {
            typeText.setText("ERROR: FAILED TO GET THE CLASS OF THE DEVICE");
        }
    }


}
