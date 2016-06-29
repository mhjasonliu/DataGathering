package com.northwestern.habits.datagathering;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.northwestern.habits.datagathering.banddata.BandDataService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by William on 6/20/2016
 */
public class DeviceListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<DeviceListItem> _listDataHeader;
    private HashMap<DeviceListItem, List<String>> _listDataChild;
    private static final String TAG = "DeviceListAdapter";

    public DeviceListAdapter(Context context, List<DeviceListItem> headerData, HashMap<DeviceListItem,
            List<String>> childData) {
        this.context = context;
        this._listDataHeader = headerData;
        this._listDataChild = childData;

        Intent intent = new Intent(context, BandDataService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static HashMap<DeviceListItem, List<String>> createChildren(List<DeviceListItem> items) {
        HashMap<DeviceListItem, List<String>> children = new HashMap<>();
        for (DeviceListItem device :
                items) {
            switch (device.getType()) {
                case BAND:
                    children.put(device, Arrays.asList("BAND"));
                    break;
                case PHONE:
                    children.put(device, Arrays.asList("PHONE"));
                    break;
                case OTHER:
                    children.put(device, Arrays.asList("OTHER", "OTHER",
                            "OTHER", "OTHER"));
                    break;
                default:
                    Log.e(TAG, "UNIMPLEMENTED TYPE");
            }
        }
        return children;
    }

    public void unselectAllsensors() {
        int groupCount = getGroupCount();
        View childView;
        GridLayout sensorGrid;
        for (int i = 0; i < groupCount; i++) {
            DeviceListItem.DeviceType type = _listDataHeader.get(i).getType();
            if (type != null && type != DeviceListItem.DeviceType.OTHER) {
                childView = getChildView(i, 0, false, null, null);

                sensorGrid = (GridLayout) childView.findViewById(R.id.sensor_grid);
                int childViews = sensorGrid.getChildCount();
                for (int j = 0; j < childViews; j++) {
                    ((CheckBox) sensorGrid.getChildAt(j)).setChecked(false);
                }
            }
        }
        notifyDataSetChanged();
    }

    private AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.v(TAG, "Item selected");
            switch (parent.getId()) {
                case R.id.locationSpinner:
                    //TODO
                    break;
                case R.id.frequencySpinner:
                    Log.v(TAG, "Frequency change to " + parent.getItemAtPosition(position));
                    //TODO
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    public int getGroupCount() {
        return _listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return _listDataChild.get(_listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return _listDataHeader.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return _listDataChild.get(_listDataHeader.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // Todo possibly fix this?
        String headerTitle = ((DeviceListItem) getGroup(groupPosition)).getName();
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);



        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        DeviceListItem device = _listDataHeader.get(groupPosition);
        DeviceListItem.DeviceType type = device.getType();

        LayoutInflater infalInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView;
        switch (type) {
            case BAND:
                rootView = infalInflater.inflate(R.layout.item_band_sensors, null);
                // Prepare the location spinner
                Spinner locationSpinner = (Spinner) rootView.findViewById(R.id.locationSpinner);
                ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(context,
                        R.array.locations_array, android.R.layout.simple_spinner_item);
                // Specify the layout to use when the list of choices appears
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // Apply the locationAdapter to the spinner
                locationSpinner.setAdapter(locationAdapter);
                locationSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);

                // Prepare the frequency spinner
                Spinner frequencySpinner = (Spinner) rootView.findViewById(R.id.frequencySpinner);
                ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(context,
                        R.array.frequency_array, android.R.layout.simple_spinner_item);
                // Specify the layout to use when the list of choices appears
                frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // Apply the frequencyAdapter to the spinner
                frequencySpinner.setAdapter(frequencyAdapter);
                frequencySpinner.setOnItemSelectedListener(spinnerItemSelectedListener);

                // Set the checked values and the onclicked listener
                setSensorsInGrid(rootView, device);
                return rootView;

            case PHONE:
                rootView = infalInflater.inflate(R.layout.item_phone_sensors, null);

                // Enable checkboxes as the phone allows

                SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                rootView.findViewById(R.id.accelBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
                rootView.findViewById(R.id.tempBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null);
                rootView.findViewById(R.id.gravBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null);
                rootView.findViewById(R.id.gyroBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
                rootView.findViewById(R.id.lightBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_LIGHT) != null);
                rootView.findViewById(R.id.linAccBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null);
                rootView.findViewById(R.id.magFieldBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);
                rootView.findViewById(R.id.barometerBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null);
                rootView.findViewById(R.id.proxBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null);
                rootView.findViewById(R.id.humidBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null);
                rootView.findViewById(R.id.rotationBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);
                setSensorsInGrid(rootView, device);

                return rootView;
            case OTHER:
            default:
                // TODO make this code not be from the old ExpandableListAdapter class
                final String childText = (String) getChild(groupPosition, childPosition);

                if (convertView == null) {
                    convertView = infalInflater.inflate(R.layout.list_item, null);
                }

                TextView txtListChild = (TextView) convertView
                        .findViewById(R.id.lblListItem);

                txtListChild.setText(childText);
                return convertView;
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private void setSensorsInGrid(View v, DeviceListItem device) {
        GridLayout gridLayout = (GridLayout) v.findViewById(R.id.sensor_grid);
        int childCount = gridLayout.getChildCount();
        CheckBox box;
        SharedPreferences preferences = context.getSharedPreferences(Preferences.NAME,
                Context.MODE_PRIVATE);
        for (int i = 0; i < childCount; i++) {
            box = (CheckBox) gridLayout.getChildAt(i);
            box.setChecked(preferences.getBoolean(
                    Preferences.getSensorKey(device.getMAC(), box.getText().toString()
                    ), false));
            box.setTag(device);
            box.setOnCheckedChangeListener(sensorBoxListener);
        }
    }
    boolean isBound = false;
    Messenger mMessenger;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            // Create the Messenger object
            mMessenger = new Messenger(service);

            // Create a Message
            // Note the usage of MSG_SAY_HELLO as the what value
//            Message msg = Message.obtain(null, MessengerService.MSG_SAY_HELLO, 0, 0);

            // Create a bundle with the data
            Bundle bundle = new Bundle();
            bundle.putString("hello", "world");

            // Set the bundle data to the Message
//            msg.setData(bundle);

            // Send the Message to the Service (in another process)
//            try {
//                mMessenger.send(msg);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // unbind or process might have crashes
            mMessenger = null;
            isBound = false;
        }
    };


    private CompoundButton.OnCheckedChangeListener sensorBoxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // FIXME: 6/28/2016 hack to connect to the old band streaming service
                SharedPreferences prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Preferences.getSensorKey(((DeviceListItem) buttonView.getTag()).getMAC(),
                                buttonView.getText().toString()),
                        isChecked);
                e.apply();
            DeviceListItem device = (DeviceListItem) buttonView.getTag();
            switch (device.getType()) {
                case BAND:
                    Message bandMessage = Message.obtain(null, BandDataService.MSG_STREAM, 0, 0);
                    Bundle requestBundle = new Bundle();
                    Intent bandDataIntent = new Intent(context, BandDataService.class);

                    String text = buttonView.getText().toString();
                    switch (text) {
                        case "Accelerometer":
                            // TODO: 6/29/2016 Request a frequency
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.ACCEL_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.ACCEL_REQ_EXTRA, true);
                            break;
                        case "Altimeter":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.ALT_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.ALT_REQ_EXTRA, true);
                            break;
                        case "Ambient Light":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.AMBIENT_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.AMBIENT_REQ_EXTRA, true);
                            break;
                        case "Barometer":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.BAROMETER_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.BAROMETER_REQ_EXTRA, true);
                            break;
                        case "Calories":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.CALORIES_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.CALORIES_REQ_EXTRA, true);
                            break;
                        case "Contact":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.CONTACT_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.CONTACT_REQ_EXTRA, true);
                            break;
                        case "Distance":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.DISTANCE_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.DISTANCE_REQ_EXTRA, true);
                            break;
                        case "GSR":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.GSR_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.GSR_REQ_EXTRA, true);
                            break;
                        case "Gyroscope":
                            // TODO: 6/29/2016 Request a frequency
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.GYRO_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.GYRO_REQ_EXTRA, true);
                            break;
                        case "Heart Rate":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.HEART_RATE_REQ_EXTRA);
                            // TODO: 6/29/2016 Request permission
                            bandDataIntent.putExtra(BandDataService.HEART_RATE_REQ_EXTRA, true);
                            break;
                        case "Pedometer":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.PEDOMETER_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.PEDOMETER_REQ_EXTRA, true);
                            break;
                        case "Skin Temp.":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.SKIN_TEMP_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.SKIN_TEMP_REQ_EXTRA, true);
                            break;
                        case "UV":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.UV_REQ_EXTRA);
                            bandDataIntent.putExtra(BandDataService.UV_REQ_EXTRA, true);
                            break;
                        default:
                            Log.e(TAG, "Button text not recognized");
                    }
                    requestBundle.putBoolean(BandDataService.STOP_STREAM_EXTRA, !isChecked);
//                    bandDataIntent.putExtra(BandDataService.STUDY_ID_EXTRA,
//                            prefs.getString(Preferences.USER_ID, ""));
//                    bandDataIntent.putExtra(BandDataService.CONTINUE_STUDY_EXTRA, true);
//                    bandDataIntent.putExtra(BandDataService.STOP_STREAM_EXTRA, !isChecked);
//                    bandDataIntent.putExtra(BandDataService.INDEX_EXTRA, 0);
//                    bandDataIntent.putExtra(BandDataService.FREQUENCY_EXTRA, "8Hz");
//                    bandDataIntent.putExtra(BandDataService.LOCATION_EXTRA, "");

//                    context.startService(bandDataIntent);
                    requestBundle.putString(BandDataService.MAC_EXTRA, device.getMAC());
                    bandMessage.setData(requestBundle);
                    try {
                        mMessenger.send(bandMessage);
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                    break;
                case PHONE:
                    // TODO implement phone sensor streaming
                    break;
                case OTHER:
                    break;
                default:
                    throw new UnsupportedOperationException();

            }




        }
    };

}
