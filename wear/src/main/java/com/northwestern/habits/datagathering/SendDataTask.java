package com.northwestern.habits.datagathering;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sends a bunch of bytes on the google API client
 * Created by William on 1/29/2017.
 */
public class SendDataTask extends AsyncTask<Void, Void, Void> {
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private String nodeId;
    private DataAccumulator mAccumulator;
    private String mType;

    public static boolean isSendingData = false;

    private static final String TAG = "SendDataTask";

    public SendDataTask(Context context) {
        mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        nodeId = mContext.getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getString(Preferences.KEY_PHONE_ID, null);
        Log.e(TAG, "SendTask");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mGoogleApiClient.isConnected()) {
            Log.e(TAG, "mGoogleApiClient.isConnected()");
            List<Node> nodeList = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
            for (Node node : nodeList) {
                if (node.isNearby()) {
                    if (!isSendingData) { // Do not risk trying to send deleted files and vice versa
                        isSendingData = true;
                        Log.e(TAG, "nodeid " + nodeId);
                        if (nodeId == null) return null;

                        // Top level file
                        File dir = new File(mContext.getExternalFilesDir(null).getPath() + "/WearData/");
                        sendChildren(dir);

                        isSendingData = false;
                    }
                }
            }
        } else {
            Log.e(TAG, "mGoogleApiClient.isConnected() false");
        }
        return null;
    }

    private synchronized void sendChildren(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isDirectory()) {
                    sendChildren(file);
                } else {
                    sendFile(file, dir);
                }
            }
        }
    }

    private synchronized void sendFile(File file, File directory) {

        // Delete only if the file name is not the current hour:minute
        Calendar c = Calendar.getInstance();
        String currentcsv = Integer.toString(c.get(Calendar.HOUR_OF_DAY))
                + "_" + Integer.toString(c.get(Calendar.MINUTE)) + ".csv";
        if (!file.getName().equals(currentcsv)) {
            Log.v(TAG, "***************** *********** Opening channel...");
            ChannelApi.OpenChannelResult result =
                    Wearable.ChannelApi.openChannel(mGoogleApiClient, nodeId,
                            file.getPath().substring(
                                    mContext.getExternalFilesDir(null).getPath().length() + 1)
                    ).await(10, TimeUnit.SECONDS);

            if (result.getStatus().isSuccess()) {
                Channel channel = result.getChannel();
                channel.sendFile(mGoogleApiClient, Uri.fromFile(file)).await(1, TimeUnit.SECONDS);
                Log.v(TAG, "Sending csv " + file.getName() + " from " + directory.getName());
                Log.v(TAG, " for path " + file.getAbsolutePath());
                if (file.delete()) Log.v(TAG, "File successfully deleted");
            } else {
                Log.v(TAG, "Channel failed. " + result.getStatus().toString());
//                new WriteDataTask(mContext, mAccumulator, mType).execute();
            }
        } else {
            Log.v(TAG, "Current csv " + currentcsv + " equals " + file.getName());
            Log.v(TAG, " for path " + file.getAbsolutePath());
        }
    }
}