package com.northwestern.habits.datagathering;

import android.*;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.LabelManager;
import com.northwestern.habits.datagathering.userinterface.SplashActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by William on 7/11/2016
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";
    public static final String ACTION_LABEL = "com.northwestern.habits.datagathering.action.LABEL";
    public static final String LABEL_EXTRA = "Label";
    public static final String USER_ID_EXTRA = "UserID";
    public static final String TIMESTAMP_EXTRA = "timestamp";

    /**
     * Returns whether or not the wifi is accessable
     * @param c context from which to access the wifi service
     * @return boolean
     */
    public static boolean isWifiConnected(Context c) {
        SupplicantState supState;
        WifiManager wifiManager = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        supState = wifiInfo.getSupplicantState();
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
            && supState == SupplicantState.COMPLETED;
    }

    /**
     * Returns whether or not the phone is charging
     * @param context from which to access the battery manager
     * @return boolean
     */
    public static boolean isCharging(Context context) {
        // Check for charging
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert i != null;
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    Log.v(TAG, "Power connected action received.");

                    // Power connected, start backup if wifi connected
                    if (isWifiConnected(context)) {
                        // start database backup
                        Intent i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_BACKUP);
                        context.startService(i);
                    }
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.v(TAG, "Power disconnected action received.");

                    // Power disconnected. stop backup if it is running
                    Intent i = new Intent(context, DataManagementService.class);
                    i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                    context.startService(i);
                break;
                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    Toast.makeText(context, "HAbits:NETWORK CHANGED1", Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Wifi action received");
                    if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                            false)) {
                        // Wifi is connected, start backup if power connected
                        if (isCharging(context)) {
                            i = new Intent(context, DataManagementService.class);
                            i.setAction(DataManagementService.ACTION_BACKUP);
                            context.startService(i);
                        }
                    } else {
                        // Wifi disconnected, stop backup if it is running
                        i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);
                    }
                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    Toast.makeText(context, "HAbits:NETWORK CHANGED2", Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Wifi action received");
                    if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                            false)) {
                        // Wifi is connected, start backup if power connected
                        if (isCharging(context)) {
                            i = new Intent(context, DataManagementService.class);
                            i.setAction(DataManagementService.ACTION_BACKUP);
                            context.startService(i);
                        }
                    } else {
                        // Wifi disconnected, stop backup if it is running
                        i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Log.v(TAG, "Wifi action received");
                    // Wifi is connected, start backup if power connected
                    if (isCharging(context) && isWifiConnected(context)) {
                        Toast.makeText(context, "HAbits:NETWORK CHANGED3", Toast.LENGTH_SHORT).show();
                        /*i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_BACKUP);
                        context.startService(i);*/
                        // upload csv task
                        new UploadCSVsAsyncTask(context).execute();
                    } else {
                        // Wifi disconnected, stop backup if it is running
                        i = new Intent(context, DataManagementService.class);
                        i.setAction(DataManagementService.ACTION_STOP_BACKUP);
                        context.startService(i);
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    SplashActivity.onStartup(context);
                    break;
                case ACTION_LABEL:
                    // Hand it off to the LabelManager
                    int labelExtra = intent.getIntExtra(LABEL_EXTRA, 0);
                    long timestamp = intent.getLongExtra(TIMESTAMP_EXTRA, 0);
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit().putInt(Preferences.LABEL, labelExtra)
                            .putBoolean(Preferences.IS_EATING,
                                    labelExtra == DataManagementService.L_EATING)
                            .apply();
                    String userID = PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .getString(Preferences.USER_ID, "");
                    LabelManager.addLabelChange(userID, context, Integer.toString(labelExtra), timestamp);
                    break;
                default:
                    Log.e(TAG, "Unknown type sent to receiver: " + intent.getAction());
            }
        }
    }

    //upload csv if available to the remote server
    // asynk task
    private class UploadCSVsAsyncTask extends AsyncTask<Void, Void, String> {

        private String url = "";
        private Object mObject = null;
        private String mFlag = "";
        private ProgressDialog dialog;
        private Context mContext = null;

        public UploadCSVsAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
//            dialog = ProgressDialog.show( this.mContext, "", "Please wait..." );
//            dialog.setCanceledOnTouchOutside(false);
//            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "permission denied");
            }
            String userID = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(Preferences.USER_ID, "");
            String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/" +
                    userID + "/" ;
            File folder = new File(PATH);
            Date dt = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_HH:mm:ss", Locale.US);
            String time1 = sdf.format(dt);
            Log.v(TAG, "time " +time1 );

            if (folder.exists()) {
                File[] listFile = folder.listFiles();
                if (listFile.length > 0) {
                    String[] s = new String[listFile.length];
                    for (int i = 0; i < listFile.length; i++) {
                        s[i] = listFile[i].getPath();
                        File file = new File(s[i]);
                        if (file.exists()) {
                            String zname = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/"));
                            zname = zname.replace("/", "");
                            String zipname = file.getParent() + "/" + zname + "_" + time1 + ".zip";
                            Log.v(TAG, "Zip location " + zipname);
                            zipFiles(file, new File(zipname));
                            deleteDir(file);
                        }
                    }
                }
            }

            /*
//            working code
             try {
                  String z = "/storage/emulated/0/devdeeds/test.zip";
                  FileOutputStream fos = new FileOutputStream( z );
                  ZipOutputStream zos = new ZipOutputStream(fos);

                            String f1 = "/storage/emulated/0/devdeeds/File1.PNG";
                            String f2 = "/storage/emulated/0/devdeeds/File2.PNG";
                            String f3 = "/storage/emulated/0/devdeeds/File3.PNG";

                            addToZipFile(f1, zos);
                            addToZipFile(f2, zos);
                            addToZipFile(f3, zos);

                            zos.close();
                            fos.close();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
             */



            String str = "";
            /*try {
                str = WebAPIManager.httpPOSTRequest(this.url, this.mObject, this.mFlag );
                return str;
            } catch (Exception e) {
                Log.e(UserIDFragment.class.toString(), e.getMessage(), e);
            }*/
            return str;
        }

        @Override
        protected void onPostExecute(String response) {
        }
    }

    // For to Delete the directory inside list of files and inner Directory
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    private void deleteFiles(File dir) {
        Log.v(TAG, "deleting " + dir.getAbsolutePath());
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }
//        file.deleteOnExit();
    }

    public static void zipFiles(File directory, File zipfile) {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        Closeable res = null;
        try {
            OutputStream out = new FileOutputStream(zipfile);
            res = out;
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                res.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    public static void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException {

        System.out.println("Writing '" + fileName + "' to zip file");

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    public static String zip(File[] files1, String zipFile) throws IOException {
        String[] files = new String[files1.length];
        for (int i = 0; i < files1.length; i++) {
            files[i] = files1[i].getPath();
        }
        Log.v(TAG, "directory " + files1 + " & zip name: " + zipFile);
        final int BUFFER_SIZE = 2048;
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];

            for (int i = 0; i < files1.length; i++) {
                File f = files1[i];
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                }
                finally {
                    origin.close();
                }
            }
        }
        finally {
            out.close();
        }
        return zipFile;
    }

    public void zipDir(String dir2zip, String zipFileName) {
        try {
            FileOutputStream dest = new FileOutputStream(zipFileName + ".zip");
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            //create a new File object based on the directory we
            //have to zip File
            File zipDir = new File(dir2zip);

            //get a listing of the directory content
//            String[] dirList = zipDir.list();
            File[] dirList1 = zipDir.listFiles();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;

            //loop through dirList, and zip the files
            for(int i=0; i<dirList1.length; i++) {
                File f = dirList1[i];
                if(f.isDirectory()) {
                    //if the File object is a directory, call this
                    //function again to add its content recursively
                    String filePath = f.getPath();
                    zipDir(filePath, zipFileName);
                    //loop again
                    continue;
                }
                //if we reached here, the File object f was not a directory
                //create a FileInputStream on top of f
                FileInputStream fis = new FileInputStream(f);
                //create a new zip entry
                ZipEntry anEntry = new ZipEntry(f.getPath());
                //place the zip entry in the ZipOutputStream object
                zos.putNextEntry(anEntry);
                //now write the content of the file to the ZipOutputStream
                while((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                //close the Stream
                fis.close();
            }

//            zos.close();
        }
        catch(Exception e) {
            //handle exception
            e.printStackTrace();
        }
    }

    public static String zip1(String[] _files, String zipFileName) {
        int BUFFER = 2048;
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte data[] = new byte[BUFFER];

            for (int i = 0; i < _files.length; i++) {
                Log.v("Compress", "Adding: " + _files[i]);
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return zipFileName;
    }
}
