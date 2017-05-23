package com.northwestern.habits.datagathering.weardata;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.util.List;

public class WearDataService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {
    private static final String DATA_REQUEST_PATH = "/DataRequest";

    public WearDataService() {
    }

    private static final String TAG = "WearDataService";
    GoogleApiClient googleApiClient;
    Node nodeForTimeEntry;

    public static final String NODE_ID = "Nodeid";

    public static final String ACCEL = "Accelerometer";
    public static final String GYRO = "Gyroscope";
    public static final String HEART = "HeartRate";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = (intent != null) ? intent.getExtras() : null;
        if (extras != null) {
            String nodeID = extras.getString(NODE_ID);
            if (nodeID != null) {
                if (intent.hasExtra(ACCEL)) {
                    if (extras.getBoolean(ACCEL)) {
                        // Request accel
                        Log.v(TAG, "Accel requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (ACCEL + "1").getBytes());
                    } else {
                        // Request stop accel
                        Log.v(TAG, "Accel un-requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (ACCEL + "0").getBytes());
                    }
                }

                if (intent.hasExtra(GYRO)) {
                    if (extras.getBoolean(GYRO)) {
                        // Request accel
                        Log.v(TAG, "GYRO requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (GYRO + "1").getBytes());
                    } else {
                        // Request stop accel
                        Log.v(TAG, "GYRO un-requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (GYRO + "0").getBytes());
                    }
                }

                if (intent.hasExtra(HEART)) {
                    if (extras.getBoolean(HEART)) {
                        // Request accel
                        Log.v(TAG, "HEART requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (HEART + "1").getBytes());
                    } else {
                        // Request stop accel
                        Log.v(TAG, "HEART un-requested for " + nodeID);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
                                DATA_REQUEST_PATH, (HEART + "0").getBytes());
                    }
                }
            } else {
                Log.e(TAG, "Request received without a node id");
            }
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //googleApiClient is defined as a class member variable
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            googleApiClient.connect();
        }
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
        Wearable.ChannelApi.addListener(googleApiClient, this);
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

        if (nodeToUse == null) {
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
                    AccelerometerData accelerometerData = new AccelerometerData(data);
                    if (accelerometerData.dataSeries != null)
                        accelerometerData.sendToCsv(this);
                    else
                        Log.e(TAG, "Data series null");
                } else
                    Log.v(TAG, "Data was null");

            } else {
                Log.v(TAG, "Type other than changed: " + event.toString());
            }

        }
    }

    @Override
    public void onChannelOpened(Channel channel) {
        Log.v(TAG, "Channel event!");

//        File folder = new File(Environment.getExternalStorageDirectory() + "/Bandv2/" + channel.getPath());
//
//        if (!folder.exists()) Log.v(TAG, "mkdirs result: " + folder.mkdirs());

        File file = new File(Environment.getExternalStorageDirectory() + "/Bandv2/" + channel.getPath());
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Log.v(TAG, "File path: " + file.getPath());
        PendingResult<Status> result = channel.receiveFile(googleApiClient, Uri.fromFile(file), false);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                Log.v(TAG, "Status: " + status);
            }
        });
    }
}
