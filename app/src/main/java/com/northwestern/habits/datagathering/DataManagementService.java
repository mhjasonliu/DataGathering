package com.northwestern.habits.datagathering;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purpose of this service is to:
 * 1) manage connections and 2) fulfill streaming requests
 *
 * 1) Manage connections
 *      Accomplished by maintaining a
 */
public class DataManagementService extends Service {

    public interface DataManagementFunctions {
        void placeHolder(String text);
    }

    private Map<Activity, DataManagementFunctions> clients = new ConcurrentHashMap<Activity, DataManagementFunctions>();

    public class LocalBinder extends Binder {

        // Registers a Activity to receive updates
        public void registerActivity(Activity activity, DataManagementFunctions callback) {
            clients.put(activity, callback);
        }

        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }
    }

    public DataManagementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
