package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class ServerCommunicationService extends Service {
    public ServerCommunicationService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        return Service.START_STICKY;
    }


    private final String TAG = "Server communication";
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Service was bound when it shouldn't be.");
        return null;
    }


    private class SendDbTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {



            return null;
        }
    }
}
