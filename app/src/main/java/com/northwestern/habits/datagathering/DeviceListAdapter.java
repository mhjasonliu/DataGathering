package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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
                    Intent bandDataIntent = new Intent(context, BandDataService.class);

                    String text = buttonView.getText().toString();
                    switch (text) {
                        case "Accelerometer":
                            bandDataIntent.putExtra(BandDataService.ACCEL_REQ_EXTRA, true);
                            break;
                        case "Altimeter":
                            bandDataIntent.putExtra(BandDataService.ALT_REQ_EXTRA, true);
                            break;
                        case "Ambient Light":
                            bandDataIntent.putExtra(BandDataService.AMBIENT_REQ_EXTRA, true);
                            break;
                        case "Barometer":
                            bandDataIntent.putExtra(BandDataService.BAROMETER_REQ_EXTRA, true);
                            break;
                        case "Calories":
                            bandDataIntent.putExtra(BandDataService.CALORIES_REQ_EXTRA, true);
                            break;
                        case "Contact":
                            bandDataIntent.putExtra(BandDataService.CONTACT_REQ_EXTRA, true);
                            break;
                        case "Distance":
                            bandDataIntent.putExtra(BandDataService.DISTANCE_REQ_EXTRA, true);
                            break;
                        case "GSR":
                            bandDataIntent.putExtra(BandDataService.GSR_REQ_EXTRA, true);
                            break;
                        case "Gyroscope":
                            bandDataIntent.putExtra(BandDataService.GYRO_REQ_EXTRA, true);
                            break;
                        case "Heart Rate":
                            bandDataIntent.putExtra(BandDataService.HEART_RATE_REQ_EXTRA, true);
                            break;
                        case "Pedometer":
                            bandDataIntent.putExtra(BandDataService.PEDOMETER_REQ_EXTRA, true);
                            break;
                        case "Skin Temp.":
                            bandDataIntent.putExtra(BandDataService.SKIN_TEMP_REQ_EXTRA, true);
                            break;
                        case "UV":
                            bandDataIntent.putExtra(BandDataService.UV_REQ_EXTRA, true);
                            break;
                        default:
                            Log.e(TAG, "Button text not recognized");
                    }
                    bandDataIntent.putExtra(BandDataService.STUDY_ID_EXTRA,
                            prefs.getString(Preferences.USER_ID, ""));
                    bandDataIntent.putExtra(BandDataService.CONTINUE_STUDY_EXTRA, true);
                    bandDataIntent.putExtra(BandDataService.STOP_STREAM_EXTRA, !isChecked);
                    bandDataIntent.putExtra(BandDataService.INDEX_EXTRA, 0);
                    bandDataIntent.putExtra(BandDataService.FREQUENCY_EXTRA, "8Hz");
                    bandDataIntent.putExtra(BandDataService.LOCATION_EXTRA, "");

                    context.startService(bandDataIntent);
                    break;
                case PHONE:
                    break;
                case OTHER:
                    break;
                default:
                    throw new UnsupportedOperationException();

            }




        }
    };

}
