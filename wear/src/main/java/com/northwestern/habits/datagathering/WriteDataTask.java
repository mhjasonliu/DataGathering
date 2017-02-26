package com.northwestern.habits.datagathering;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;

/**
 * Created by William on 2/25/2017.
 */

public class WriteDataTask extends AsyncTask {
    DataAccumulator mAccumulator;
    Context mContext;
    public WriteDataTask(DataAccumulator accumulator, Context context) {
        mAccumulator = accumulator;
        mContext = context;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String nodeId = mContext.getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getString(Preferences.KEY_PHONE_ID, null);
        if (nodeId == null) return null;

        File f = new File("/");

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API).build();

        ChannelApi.OpenChannelResult result =
                Wearable.ChannelApi.openChannel(googleApiClient, nodeId, "/dataTransfer/asdf").await();

        Channel channel = result.getChannel();
        channel.sendFile(googleApiClient, Uri.fromFile(f));

        return null;
    }
}
