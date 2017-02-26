package com.northwestern.habits.datagathering;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Sends a bunch of bytes on the google API client
 * Created by William on 1/29/2017.
 */
public class SendDataTask extends AsyncTask {
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private String nodeId;

    private static final String TAG = "SendDataTask";

    public SendDataTask(Context context) {
        mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        nodeId = mContext.getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getString(Preferences.KEY_PHONE_ID, null);
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.v(TAG, "nodid " + nodeId);
        if (nodeId == null) return null;

        // Top level file
        File dir = new File(mContext.getExternalFilesDir(null).getPath() + "/WearData/");
        Log.v(TAG, "dir: " + dir.getPath());
        sendChildren(dir);


        return null;
    }

    private void sendChildren(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    sendChildren(file);
                } else {
                    sendFile(file, dir);
                }
            }
        }
    }

    private void sendFile(File file, File directory) {
        Log.v(TAG, "Opening channel...");
        ChannelApi.OpenChannelResult result =
                Wearable.ChannelApi.openChannel(mGoogleApiClient, nodeId,
                        "/WearData/" + file.getPath()).await(5, TimeUnit.SECONDS);

        if (result.getStatus().isSuccess()) {
            Channel channel = result.getChannel();
            Log.v(TAG, "Sending file...");
            channel.sendFile(mGoogleApiClient, Uri.fromFile(file));
            Log.v(TAG, "File sent.");
        } else {
            Log.v(TAG, "Channel failed. " + result.getStatus().toString());
        }
    }
}