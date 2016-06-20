package com.northwestern.habits.datagathering;

import android.content.Context;
import android.util.Log;
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
