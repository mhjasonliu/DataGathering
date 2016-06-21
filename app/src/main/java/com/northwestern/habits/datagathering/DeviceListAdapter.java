package com.northwestern.habits.datagathering;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
                    children.put(device.getName(), Arrays.asList("BAND", "BAND", "BAND"));
                    break;
                case OTHER:
                    children.put(device.getName(), Arrays.asList("OTHER","OTHER",
                            "OTHER","OTHER"));
                    break;
                default:
                    Log.e(TAG, "UNIMPLEMENTED TYPE");
            }
        }
        return children;
    }

    public void setDevices(List<DeviceListItem> items) {devices = items;}

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

        DeviceListItem.DeviceType type = devices.get(groupPosition).getType();

        switch (type) {
            case BAND:
                LayoutInflater infalInflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View rootView = infalInflater.inflate(R.layout.item_band_sensors, null);
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
                return rootView;
            case OTHER:
            default:
                return super.getChildView(groupPosition, childPosition, isLastChild, null, parent);
        }
    }

    //    public View getView(int position, View convertView, ViewGroup parent) {
//        ViewHolder holder = null;
//        DeviceListItem item = (DeviceListItem)getChild()getItem(position);
//        View viewToUse = null;
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//
//
//        if (convertView == null) {
//            if(useList){
//                viewToUse = inflater.inflate(R.layout.device_list_item, null);
//            } else {
//                viewToUse = inflater.inflate(R.layout.device_list_item, null);
//            }
//
//            holder = new ViewHolder();
//            holder.nameText = (TextView)viewToUse.findViewById(R.id.title_text);
//            holder.macText = (TextView)viewToUse.findViewById(R.id.mac_address);
//            viewToUse.setTag(holder);
//        } else {
//            viewToUse = convertView;
//            holder = (ViewHolder) viewToUse.getTag();
//        }
//
//        holder.nameText.setText(item.getName());
//        Log.v(TAG, "Name is " + item.getName());
//        holder.macText.setText(item.getMAC());
//        return viewToUse;
//    }

}
