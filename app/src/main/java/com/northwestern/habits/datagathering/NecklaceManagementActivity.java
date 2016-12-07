package com.northwestern.habits.datagathering;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

import com.northwestern.habits.datagathering.necklacedata.NecklaceDataService;

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

//    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            Log.i("onConnectionStateChange", "Status: " + status);
//            switch (newState) {
//                case BluetoothProfile.STATE_CONNECTED:
//                    Log.i("gattCallback", "STATE_CONNECTED");
//                    gatt.discoverServices();
//                    break;
//                case BluetoothProfile.STATE_DISCONNECTED:
//                    Log.e("gattCallback", "STATE_DISCONNECTED");
//                    // TODO provide user some warning
//                    break;
//                default:
//                    Log.e("gattCallback", "STATE_OTHER");
//            }
//
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            List<BluetoothGattService> services = gatt.getServices();
//            Log.i("onServicesDiscovered", services.toString());
//            gatt.readCharacteristic(services.get(1).getCharacteristics().get
//                    (0));
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic
//                                                 characteristic, int status) {
//            Log.i("onCharacteristicRead", characteristic.toString());
//            gatt.disconnect();
//        }
//    };

    public void onStartNecklaceStream(View view) {
        Intent necklaceIntent = new Intent(NecklaceManagementActivity.this, NecklaceDataService.class);

        necklaceIntent.putExtra(NecklaceDataService.START_STREAM_EXTRA, true);
        necklaceIntent.putExtra(NecklaceDataService.AUDIO_EXTRA,
                ((CheckBox) findViewById(R.id.audioBox)).isChecked());
        necklaceIntent.putExtra(NecklaceDataService.PIEZO_EXTRA,
                ((CheckBox) findViewById(R.id.piezoBox)).isChecked());
        necklaceIntent.putExtra(NecklaceDataService.DEVICE_EXTRA, device);

        startService(necklaceIntent);

    }

    public void onStopNecklaceStream(View view) {
        Intent necklaceIntent = new Intent(NecklaceManagementActivity.this, NecklaceDataService.class);

        necklaceIntent.putExtra(NecklaceDataService.START_STREAM_EXTRA, false);
        necklaceIntent.putExtra(NecklaceDataService.AUDIO_EXTRA,
                ((CheckBox) findViewById(R.id.audioBox)).isChecked());
        necklaceIntent.putExtra(NecklaceDataService.PIEZO_EXTRA,
                ((CheckBox) findViewById(R.id.piezoBox)).isChecked());
        necklaceIntent.putExtra(NecklaceDataService.DEVICE_EXTRA, device);

        startService(necklaceIntent);
    }

}
