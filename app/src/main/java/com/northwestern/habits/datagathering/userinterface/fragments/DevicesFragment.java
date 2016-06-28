package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;
import com.northwestern.habits.datagathering.DeviceListAdapter;
import com.northwestern.habits.datagathering.DeviceListItem;
import com.northwestern.habits.datagathering.R;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnDevicesFragmentInterractionListener}
 * interface.
 */
public class DevicesFragment extends Fragment {

    private static final String TAG = "DevicesFragment";

    private OnDevicesFragmentInterractionListener mListener;
    private List<DeviceListItem> devices;

    /**
     * The fragment's ListView/GridView.
     */
    private ExpandableListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private DeviceListAdapter mAdapter;

    // TODO: Rename and change types of parameters
    public static DevicesFragment newInstance() {
        DevicesFragment fragment = new DevicesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DevicesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devices = new LinkedList<>();

        // Get a list of the other bluetooth devices
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {

            // Add this phone to the list of devices
            DeviceListItem phone = new DeviceListItem();
            phone.setName("This phone");
            phone.setMAC(adapter.getAddress());
            phone.setType(DeviceListItem.DeviceType.PHONE);
            devices.add(phone);
        }


        // Get all connected bands and add them to devices list
        BandInfo[] bands = BandClientManager.getInstance().getPairedBands();
        for (BandInfo band :
                bands) {
            devices.add(new DeviceListItem(band));
        }

        // Create an adapter with a list of devices
        HashMap<DeviceListItem, List<String>> children = DeviceListAdapter.createChildren(devices);
        mAdapter = new DeviceListAdapter(getContext(), devices, children);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices, container, false);

        // Set the adapter
        mListView = (ExpandableListView) rootView.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        rootView.findViewById(R.id.unselect_sensors_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.unselectAllsensors();
            }
        });

        rootView.findViewById(R.id.skip_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.advanceScrollRequest();
            }
        });

        mListener.onRequestDeviceRegistration(devices);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDevicesFragmentInterractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDevicesFragmentInterractionListener {
        public void onRequestDeviceRegistration(List<DeviceListItem> deviceItems);
        public void advanceScrollRequest();
    }
}
