package com.northwestern.habits.datagathering;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class WearDataService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {
    public WearDataService() {
    }

    private static final String TAG = "WearDataService";
    GoogleApiClient googleApiClient;
    Node nodeForTimeEntry;

    @Override
    public void onCreate() {
        super.onCreate();
        //googleApiClient is defined as a class member variable
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        Log.v(TAG, "Connecting...");
        googleApiClient.connect();
        Log.v(TAG, "Passed connect");
    }

    @Override
    public void onConnectedNodes(List<Node> var1) {
        Log.v(TAG, "Connected nodes: " + var1.size());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Connected.");
        Wearable.CapabilityApi.getCapability(googleApiClient, "fetch_timeentry_data_capability", CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        //we’ll define this method later
                        determineNodeForDataRetrieval(result.getCapability().getNodes());
                    }
                });

        Wearable.CapabilityApi.addCapabilityListener(googleApiClient, this, "fetch_timeentry_data_capability");
        Wearable.DataApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.CapabilityApi.removeCapabilityListener(googleApiClient, this, "fetch_timeentry_data_capability");
        nodeForTimeEntry = null;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.v(TAG, "Capability changed");
        determineNodeForDataRetrieval(capabilityInfo.getNodes());
    }
    private void determineNodeForDataRetrieval(Iterable<Node> nodes) {
        Log.v(TAG, "determining nodes");
        Node nodeToUse = null;
        for (Node node : nodes) {
            Log.v(TAG, "NODE FOUND");
            if (node.isNearby()) {
                nodeToUse = node;
                break;
            }
            nodeToUse = node;
        }

        if(nodeToUse == null) {
            Log.v(TAG, "Nodenull");
            nodeForTimeEntry = null;
        } else {
            nodeForTimeEntry = nodeToUse;
            Log.v(TAG, "Node not null");
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.e(TAG, "DATA CHANGED");
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.v(TAG, "Event got.");

                Log.v(TAG, event.toString());
                DataItem item = event.getDataItem();
                Log.v(TAG, item.toString());

                DataMap dataMap = DataMap.fromByteArray(item.getData()).getDataMap("time_entries");
                Log.v(TAG, dataMap.toString());

                byte[] data = dataMap.getByteArray("data");
                int count = dataMap.getInt("count");

                Log.v(TAG, "Count is " + Integer.toString(count));

                if (data != null) {
                    Log.v(TAG, Float.toString(bytes2Float(data)));

                    int i = 0;
                    while (i < data.length) {
                        // x
                        // y
                        // z

                    }

                } else
                    Log.v(TAG, "Data was null");

            } else {
                Log.v(TAG, "Type other than changed: " + event.toString());
            }

        }
    }


    private float bytes2Float(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
    }
}
