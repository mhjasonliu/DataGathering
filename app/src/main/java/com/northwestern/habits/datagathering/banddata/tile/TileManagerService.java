package com.northwestern.habits.datagathering.banddata.tile;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;

import java.util.UUID;

public class TileManagerService extends Service {
    public static final String TILE_ID_EXTRA = "Button_data";
    private static final String TAG = "TileManagerService";
    public TileManagerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        UUID buttonData = (UUID) intent.getExtras().get(TILE_ID_EXTRA);
        new HandleBroadcastTask(getBaseContext(), buttonData).execute();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    class HandleBroadcastTask extends AsyncTask<Void, Void, Void> {
        public HandleBroadcastTask(Context c, UUID e) {
            context = c;
            tID = e;
        }

        private Context context;
        private UUID tID;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                UUID tid = tID;
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
