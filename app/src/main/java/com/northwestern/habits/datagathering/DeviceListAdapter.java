package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void setMessenger(Messenger m) {
        messenger = m;
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
                    View box = sensorGrid.getChildAt(j);
                    if (box instanceof CheckBox)
                        ((CheckBox) sensorGrid.getChildAt(j)).setChecked(false);
                }
            }
        }
        notifyDataSetChanged();
    }

    private AdapterView.OnItemSelectedListener accSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//            sendFrequencyMesasge(parent, view, BandDataService.ACCEL_REQ_EXTRA);
            view.setEnabled(false);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };
    private AdapterView.OnItemSelectedListener gyroSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            DeviceListItem d = (DeviceListItem) parent.getTag();
//
//            String preferenceKey = Preferences.getFrequencyKey(d.getMAC(), Preferences.GYRO);
//            String savedFrequency = p.getString(preferenceKey, "");
            String selectedFrequency = ((TextView) view).getText().toString();
//
//            if (!Objects.equals(savedFrequency, "")) {
//                listenForSelected = !Objects.equals(savedFrequency, selectedFrequency);
//            }
//
//
//            if (listenForSelected) {
                ((Spinner) ((View) parent.getParent())
                        .findViewById(R.id.acc_frequency_spinner)).setSelection(position);
                sendFrequencyMesasge(parent, view, BandDataService.GYRO_REQ_EXTRA);
                SharedPreferences.Editor e = p.edit();
//                Log.e(TAG, ((TextView) view).getText().toString());
                e.putString(Preferences.getFrequencyKey(d.getMAC(), Preferences.GYRO), selectedFrequency);
                e.apply();
//            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };

    private void sendFrequencyMesasge(AdapterView<?> parent, View view, String requestType) {

        // Get the device associated
        DeviceListItem device = (DeviceListItem) parent.getTag();
        String mac = device.getMAC();

        // Send a request to change the frequency
        Message m = new Message();
        m.what = BandDataService.MSG_FREQUENCY;
        Bundle b = new Bundle();
        b.putString(BandDataService.REQUEST_EXTRA, requestType);
        b.putString(BandDataService.FREQUENCY_EXTRA, ((TextView) view).getText().toString());
        b.putString(BandDataService.MAC_EXTRA, device.getMAC());
        m.setData(b);
        try {
            messenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

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
        Log.v(TAG, "getting child views");
        DeviceListItem device = _listDataHeader.get(groupPosition);
        DeviceListItem.DeviceType type = device.getType();

        LayoutInflater infalInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView;
        switch (type) {
            case BAND:
                rootView = infalInflater.inflate(R.layout.item_band_sensors, null);

                // Prepare the accelerometer frequency spinner
                Spinner frequencySpinner = (Spinner) rootView.findViewById(R.id.acc_frequency_spinner);
                ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(context,
                        R.array.frequency_array, R.layout.disabled_text_view);
                // Specify the layout to use when the list of choices appears
                frequencyAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                // Apply the frequencyAdapter to the spinner
                frequencySpinner.setAdapter(frequencyAdapter);
                frequencySpinner.setOnItemSelectedListener(accSpinnerListener);
                frequencySpinner.setEnabled(false);


                // Repeat for gyro
                frequencySpinner = (Spinner) rootView.findViewById(R.id.gyro_frequency_spinner);
                // Apply the frequencyAdapter to the spinner
                frequencySpinner.setAdapter(frequencyAdapter);
                frequencySpinner.setOnItemSelectedListener(gyroSpinnerListener);

                // Set the checked values and the onclicked listener
                setSensorsInGrid(rootView, device);

                // Gray out the accelerometer box and set the listener appropriately
                CheckBox box = (CheckBox) rootView.findViewById(R.id.accelerometerBox);
                box.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Alert the user that accelerometer and gyro stream together
                        new AlertDialog.Builder(context).setTitle("Accelerometer Streaming")
                                .setMessage("Streaming the accelerometer alone is currently " +
                                        "not supported. To stream accelerometer, start " +
                                        "streaming gyroscope and the accelerometer will start " +
                                        "streaming at the same frequency as the gyroscope.")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create().show();
                        ((CheckBox) v).setChecked(!((CheckBox) v).isChecked()); // Keep it how it was
                    }
                });
                box.setEnabled(true);

                return rootView;

            case PHONE:
                rootView = infalInflater.inflate(R.layout.item_phone_sensors, null);

                // Enable checkboxes as the phone allows

                SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                rootView.findViewById(R.id.accelBox).setEnabled(
                        manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
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
        View box;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 0; i < childCount; i++) {
            box = gridLayout.getChildAt(i);
            if (box instanceof CheckBox) {
                ((CheckBox) box).setChecked(preferences.getBoolean(
                        Preferences.getSensorKey(device.getMAC(),
                                ((CheckBox) box).getText().toString()
                        ), false));
                ((CheckBox) box).setOnCheckedChangeListener(sensorBoxListener);
            } else if (box instanceof Spinner) {
                String type = Preferences.GYRO;
//                if (box.getId() == R.id.acc_frequency_spinner) {
//                    type = Preferences.ACCEL;
                /*} else */
                if (box.getId() == R.id.gyro_frequency_spinner) {
                    type = Preferences.GYRO;
                }
                String frequency = preferences.getString(Preferences.getFrequencyKey(device.getMAC(),
                        type), "8Hz");
                int index;
                switch (frequency) {
                    case "8Hz":
                        index = 0;
                        break;
                    case "31Hz":
                        index = 1;
                        break;
                    case "62Hz":
                        index = 2;
                        break;
                    default:
                        Log.e(TAG, "Default case: frequency = " + frequency);
                        index = 0;
                }
                listenForSelected = false;
                ((Spinner) box).setSelection(index);
                listenForSelected = true;
            }
            box.setTag(device);
        }
    }

    private boolean listenForSelected;

    private Messenger messenger;


    private CompoundButton.OnCheckedChangeListener sensorBoxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean(Preferences.getSensorKey(((DeviceListItem) buttonView.getTag()).getMAC(),
                    buttonView.getText().toString()), isChecked);
            String mac = ((DeviceListItem) buttonView.getTag()).getMAC();
            String devKey = Preferences.getDeviceKey(mac);
            Set<String> sensors = prefs.getStringSet(devKey, new HashSet<String>());

            e.apply();
            DeviceListItem device = (DeviceListItem) buttonView.getTag();
            switch (device.getType()) {
                case BAND:
                    Message bandMessage = Message.obtain(null, BandDataService.MSG_STREAM, 0, 0);
                    Bundle requestBundle = new Bundle();

                    String text = buttonView.getText().toString();
                    switch (text) {
                        case "Accelerometer":
                            Log.e(TAG, "Accelerometer check changed when it shouldn't be!");

//                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
//                                    BandDataService.ACCEL_REQ_EXTRA);
                            if (isChecked) {
//                                sensors.add(Preferences.ACCEL);
                            } else {
//                                sensors.remove(Preferences.ACCEL);
                            }
                            break;
                        case "Altimeter":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.ALT_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.ALT);
                            } else {
                                sensors.remove(Preferences.ALT);
                            }
                            break;
                        case "Ambient Light":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.AMBIENT_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.AMBIENT);
                            } else {
                                sensors.remove(Preferences.AMBIENT);
                            }
                            break;
                        case "Barometer":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.BAROMETER_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.BAROMETER);
                            } else {
                                sensors.remove(Preferences.BAROMETER);
                            }
                            break;
                        case "Calories":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.CALORIES_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.CALORIES);
                            } else {
                                sensors.remove(Preferences.CALORIES);
                            }
                            break;
                        case "Contact":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.CONTACT_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.CONTACT);
                            } else {
                                sensors.remove(Preferences.CONTACT);
                            }
                            break;
                        case "Distance":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.DISTANCE_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.DISTANCE);
                            } else {
                                sensors.remove(Preferences.DISTANCE);
                            }
                            break;
                        case "GSR":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.GSR_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.GSR);
                            } else {
                                sensors.remove(Preferences.GSR);
                            }
                            break;
                        case "Gyroscope":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.GYRO_REQ_EXTRA);
                            ((CompoundButton) ((View) buttonView.getParent())
                                    .findViewById(R.id.accelerometerBox)).setChecked(isChecked);
                            if (isChecked) {
                                sensors.add(Preferences.GYRO);
                            } else {
                                sensors.remove(Preferences.GYRO);
                            }
                            break;
                        case "Heart Rate":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.HEART_RATE_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.HEART);
                            } else {
                                sensors.remove(Preferences.HEART);
                            }
                            break;
                        case "Pedometer":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.PEDOMETER_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.PEDOMETER);
                            } else {
                                sensors.remove(Preferences.PEDOMETER);
                            }
                            break;
                        case "Skin Temp.":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.SKIN_TEMP_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.SKIN_TEMP);
                            } else {
                                sensors.remove(Preferences.SKIN_TEMP);
                            }
                            break;
                        case "UV":
                            requestBundle.putString(BandDataService.REQUEST_EXTRA,
                                    BandDataService.UV_REQ_EXTRA);
                            if (isChecked) {
                                sensors.add(Preferences.UV);
                            } else {
                                sensors.remove(Preferences.UV);
                            }
                            break;
                        default:
                            Log.e(TAG, "Button text not recognized");
                    }
                    e.putStringSet(devKey, sensors);
                    Log.v(TAG, "New sensor set: " + sensors);
                    e.apply();

                    requestBundle.putBoolean(BandDataService.STOP_STREAM_EXTRA, !isChecked);
                    requestBundle.putString(BandDataService.MAC_EXTRA, device.getMAC());
                    bandMessage.setData(requestBundle);
                    try {
                        messenger.send(bandMessage);
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
