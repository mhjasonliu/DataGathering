package com.northwestern.habits.datagathering;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by William on 6/20/2016
 */
public class DeviceListAdapter extends ArrayAdapter<DeviceListItem> {

    private Context context;
    private boolean useList = true;
    private static final String TAG = "DeviceListAdapter";

    public DeviceListAdapter(Context context, int resource, DeviceListItem[] objects) {
        super(context, resource, objects);
        this.context = context;

    }

    private class ViewHolder {
        public TextView nameText;
        public TextView macText;
        private ViewHolder() {
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        DeviceListItem item = (DeviceListItem)getItem(position);
        View viewToUse = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);


        if (convertView == null) {
            if(useList){
                viewToUse = inflater.inflate(R.layout.device_list_item, null);
            } else {
                viewToUse = inflater.inflate(R.layout.device_list_item, null);
            }

            holder = new ViewHolder();
            holder.nameText = (TextView)viewToUse.findViewById(R.id.title_text);
            holder.macText = (TextView)viewToUse.findViewById(R.id.mac_address);
            viewToUse.setTag(holder);
        } else {
            viewToUse = convertView;
            holder = (ViewHolder) viewToUse.getTag();
        }

        holder.nameText.setText(item.getName());
        Log.v(TAG, "Name is " + item.getName());
        holder.macText.setText(item.getMAC());
        return viewToUse;
    }

}
