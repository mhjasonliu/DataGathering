package com.northwestern.habits.datagathering;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class ServerCommunicationService extends Service {
    public ServerCommunicationService() {
        super();
    }

    private final String URL = "";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        new SendDbTask().execute();
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

            // Open a connection

            // Query database for a study


            // While there is data in the study, create JSON objects and send over the connection

            // Delete the study

            // Close connection


            System.out.println("executeCommand");
            Runtime runtime = Runtime.getRuntime();
            try
            {
                Log.v(TAG, "Pinging google...");
                Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
                int mExitValue = mIpAddrProcess.waitFor();
                System.out.println(" mExitValue "+mExitValue);
                if(mExitValue==0){
                }else{
                }
            }
            catch (InterruptedException ignore)
            {
                ignore.printStackTrace();
                System.out.println(" Exception:"+ignore);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.out.println(" Exception:"+e);
            }
//
//
//            Log.v(TAG, "Now posting to google...?");
//
//            ArrayList list = new ArrayList(1);
//            list.add(0, new );
//            HttpPostHC4 poster = new
//                    HttpPostHC4("www.google.com");
//            poster.setEntity(new UrlEncodedFormEntityHC4());
//            // 1) Connect via HTTP. 2) Encode data. 3) Send data.
//            try
//            {
//                HttpClient httpclient = new DefaultHttpClient();
//                HttpPost httppost = new
//                        HttpPost("http://www.blah.com/AddAccelerationData.php");
//                httppost.setEntity(new UrlEncodedFormEntity(data));
//                HttpResponse response = httpclient.execute(httppost);
//                Log.i("postData", response.getStatusLine().toString());
//                //Could do something better with response.
//            }
//            catch(Exception e)
//            {
//                Log.e("log_tag", "Error:  "+e.toString());
//            }
            return null;
        }
    }
}
