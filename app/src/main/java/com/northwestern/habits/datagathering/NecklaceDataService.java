package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NecklaceDataService extends Service {
    public NecklaceDataService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
