package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class NecklaceManagementActivity extends AppCompatActivity {

//    public final String AUDIO_EXTRA = "audio";
//    public final String PIEZO_EXTRA = "piezo";
    public static final String NAME_EXTRA = "name";
    public static final String MAC_EXTRA = "mac";
    public static final String DEV_EXTRA = "device";

    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_necklace_management);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            device = (BluetoothDevice) extras.get(DEV_EXTRA);
        }
    }
}
