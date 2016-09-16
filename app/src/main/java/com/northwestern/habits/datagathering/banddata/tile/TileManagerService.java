package com.northwestern.habits.datagathering.banddata.tile;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.tiles.TileEvent;

import java.util.UUID;

public class TileManagerService extends Service {
    public static final String BUTTON_DATA_EXTRA = "Button_data";
    private static final String TAG = "TileManagerService";
    public TileManagerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Started");
        TileEvent buttonData = (TileEvent) intent.getExtras().get(BUTTON_DATA_EXTRA);
        new HandleBroadcastTask(getBaseContext(), buttonData).execute();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    class HandleBroadcastTask extends AsyncTask<Void, Void, Void> {
        public HandleBroadcastTask(Context c, TileEvent e) {
            context = c;
            buttonData = e;
        }

        private Context context;
        private TileEvent buttonData;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                UUID tid = buttonData.getTileID();
                BandInfo i = TileManager.infoFromUUID(tid);
                if (i != null) {
                    TileManager.updatePages(TileManager.getConnectedBandClient(i, context), tid, context);
                }
            } catch (InterruptedException | BandException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}