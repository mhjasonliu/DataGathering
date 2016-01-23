package com.northwestern.habits.datagathering.phonedata;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PhoneDataService extends Service {
    public PhoneDataService() {



    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
