package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by William on 6/20/2016
 */
public class DeviceListAdapter extends ExpandableListAdapter {

    private Context context;
    private boolean useList = true;
    private static final String TAG = "DeviceListAdapter";
    private List<DeviceListItem> devices;

    public DeviceListAdapter(Context context, List<String> headerData, HashMap<String,
            List<String>> childData) {
        super(context, headerData, childData);
        this.context = context;
    }

    public static HashMap<String, List<String>> createChildren(List<DeviceListItem> items) {
        HashMap<String, List<String>> children = new HashMap<>();
        for (DeviceListItem device :
                items) {
            switch (device.getType()) {
                case BAND:
                    children.put(device.getName(), Arrays.asList("BAND"));
                    break;
                case PHONE:
                    children.put(device.getName(), Arrays.asList("PHONE"));
                    break;
                case OTHER:
                    children.put(device.getName(), Arrays.asList("OTHER", "OTHER",
                            "OTHER", "OTHER"));
                    break;
                default:
                    Log.e(TAG, "UNIMPLEMENTED TYPE");
            }
        }
        return children;
    }

    public void setDevices(List<DeviceListItem> items) {
        devices = items;
    }

    private class ViewHolder {
        public TextView nameText;
        public TextView macText;

        private ViewHolder() {
        }
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
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        DeviceListItem device = devices.get(groupPosition);
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
                GridLayout gridLayout = (GridLayout) rootView.findViewById(R.id.band_sensor_grid);
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
                    box.setOnClickListener(sensorBoxListener);
                }

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

                return rootView;
            case OTHER:
            default:
                return super.getChildView(groupPosition, childPosition, isLastChild, null, parent);
        }
    }

    private View.OnClickListener sensorBoxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof CheckBox) {
                SharedPreferences prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Preferences.getSensorKey(((DeviceListItem) v.getTag()).getMAC(),
                                ((CheckBox) v).getText().toString()),
                        ((CheckBox) v).isChecked());
                e.apply();
            }
        }
    };

}
