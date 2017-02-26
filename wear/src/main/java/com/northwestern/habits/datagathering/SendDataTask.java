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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Sends a bunch of bytes on the google API client
 * Created by William on 1/29/2017.
 */
public class SendDataTask extends AsyncTask {
    DataAccumulator mAccumulator;
    Context mContext;

    private static final String TAG = "SendDataTask";

    public SendDataTask(DataAccumulator accumulator, Context context) {
        mAccumulator = accumulator;
        mContext = context;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String nodeId = mContext.getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getString(Preferences.KEY_PHONE_ID, null);
        Log.v(TAG, "nodid " + nodeId);
        if (nodeId == null) return null;

        File folder = new File(mContext.getExternalFilesDir(null) + "/Bandv2");
        if (!folder.exists()) Log.v(TAG, "Result of mkdirs: " + folder.mkdirs());

        File testFile = new File(folder.getPath() + "/test.txt");

        try {
            testFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(testFile);
            outputStream.write("hello!\n".getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        Log.v(TAG, "Opening channel...");
        ChannelApi.OpenChannelResult result =
                Wearable.ChannelApi.openChannel(googleApiClient, nodeId, "/WearData/asdf").await(5, TimeUnit.SECONDS);

        if (result.getStatus().isSuccess()) {
            Channel channel = result.getChannel();
            Log.v(TAG, "Sending file...");
            channel.sendFile(googleApiClient, Uri.fromFile(testFile));
            Log.v(TAG, "File sent.");
        } else {
            Log.v(TAG, "Channel failed. " + result.getStatus().toString());
        }

        return null;
    }
}