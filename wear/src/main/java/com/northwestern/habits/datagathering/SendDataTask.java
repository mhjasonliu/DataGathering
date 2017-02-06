package com.northwestern.habits.datagathering;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Sends a bunch of bytes on the google API client
 * Created by William on 1/29/2017.
 */
public class SendDataTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "SendDataTask";

    private byte[] data;
    private Context mContext;

    public SendDataTask(byte[] data, Context context) {
        this.data = data;
        this.mContext = context;
    }

    private SendDataTask() {
    }

    @Override
    protected Void doInBackground(Void... params) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();

        Log.v(TAG, "Connecting...");
        ConnectionResult connectionResult = googleApiClient.blockingConnect();

        if(!connectionResult.isSuccess()) {
            Log.e(TAG, "Connection failed.");
            return null;
        }

        Log.v(TAG, "First float of data: " + Float.toString(bytes2Float(data)));

//        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/timeEntry");
//
//        DataMap map = new DataMap();
//
//        map.putInt("count", count++);
//
//        ArrayList<DataMap> l = new ArrayList<>();
//        l.add(map);
//
//        putDataMapReq.getDataMap().putDataMapArrayList("accelerometer_entries", l);
//
//        PutDataRequest request = putDataMapReq.asPutDataRequest();
//
//        DataApi.DataItemResult dataItemResultResult =
//                Wearable.DataApi.putDataItem(googleApiClient, request).await();

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/timeEntry");
//
//        ArrayList<DataMap> entriesAsDataMaps = new ArrayList<>();
//            entriesAsDataMaps.add(toDataMap());


//        putDataMapReq.getDataMap().putDataMapArrayList("time_entries", entriesAsDataMaps);

        putDataMapReq.getDataMap().putDataMap("time_entries", toDataMap());

        PutDataRequest request = putDataMapReq.asPutDataRequest();
        Log.v(TAG, "Request: " + request.toString());

        DataApi.DataItemResult dataItemResultResult =
                Wearable.DataApi.putDataItem(googleApiClient, request).await();

        if(dataItemResultResult.getStatus().isSuccess()) {
            //handle success
            Log.v(TAG, "Success!");
        } else {
            //handle failure
            Log.v(TAG, "Failure.");
        }

        return null;
    }
    private static int count = 0;

    private float bytes2Float(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    private DataMap toDataMap() {
        DataMap map = new DataMap();
        map.putInt("count", count++);
        Log.v(TAG, "Count: " + Integer.toString(count));
//        map.putByteArray("data", data);
        return map;
    }
}
