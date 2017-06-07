package com.northwestern.habits.datagathering.webapi;

import android.app.ProgressDialog;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.northwestern.habits.datagathering.MyReceiver;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.database.DataManagementService;
import com.northwestern.habits.datagathering.database.SQLiteDBManager;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import org.json.JSONObject;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by Y.Misal on 4/26/2017.
 */

public class PhoneJobService extends JobService {

    private static final String TAG = "PhoneJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        //start job
        Log.e(TAG, "Power/WiFi available.");
        Toast.makeText(this, "HAbits: Power/WiFi changed.", Toast.LENGTH_SHORT).show();
        if (MyReceiver.isCharging(this) && MyReceiver.isWifiConnected(this)) {
            // WiFi connected, charging available start backup
            Intent i = new Intent(this, DataManagementService.class);
            i.setAction(DataManagementService.ACTION_BACKUP);
            startService(i);
            // upload csv task
            new ZipCSVsAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            // Wifi disconnected, stop backup if it is running
            Intent i = new Intent(this, DataManagementService.class);
            i.setAction(DataManagementService.ACTION_STOP_BACKUP);
            startService(i);
        }
        return true; // true if we're not done yet
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // true if we'd like to be rescheduled
        return false;
    }

    //upload csv if available to the remote server
    // asynk task
    private class ZipCSVsAsyncTask extends AsyncTask<Void, Void, String> {

        private String url = "";
        private ArrayList<String> mPath = null;
        private ProgressDialog dialog;
        private Context mContext = null;

        public ZipCSVsAsyncTask(Context context) {
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
            String PATH = Environment.getExternalStorageDirectory() + "/Bandv2/"; // + userID + "/" ;
            File folder = new File(PATH);
            Date dt = new Date();
            mPath = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_HHmmss", Locale.US);
            String time1 = sdf.format(dt);
            Log.v(TAG, "time " +time1 );

            if (folder.exists()) {
                File[] listFile = folder.listFiles();
                if (listFile.length > 0) {
                    String[] s = new String[listFile.length];
                    for (int i = 0; i < listFile.length; i++) {
                        s[i] = listFile[i].getPath();
                        File file = new File(s[i]);
                        if (file.getAbsolutePath().contains(".zip")) {
                            ZipFile zf= null;
                            try {
                                zf = new ZipFile(file.getAbsoluteFile());
                                int si =  zf.size();
                                mPath.add(file.getAbsolutePath());
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.v(TAG, "Zip err " + file.getAbsolutePath());
                                deleteDir(file);
                            }
                        } else {
                            if (file.exists()) {
                                String zname = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/"));
                                zname = zname.replace("/", "");
                                zname = zname.replace(" ", "_");
                                String zipname = file.getParent() + "/" + zname + "_" + time1 + ".zip";
                                Log.v(TAG, "Zip location " + zipname);
                                mPath.add(zipname);
                                zipFiles(file, new File(zipname));
                                deleteDir(file);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "files not available for zip.");
                }
            } else {
                Log.e(TAG, "file not exists.");
            }

            String str = "";
            return str;
        }

        @Override
        protected void onPostExecute(String response) {
            String uid = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Preferences.USER_ID, "");
            SQLiteDBManager.getInstance(mContext).addUploadStatus(uid, mPath, 0, "uploadID");
            uploadFileToServer(mContext);
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
                if (directory != null) {
                    if (directory.listFiles() != null) {
                        for (File kid : directory.listFiles()) {
                            if (kid != null) {
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

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
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

    private void uploadFileToServer(Context context) {
        //check if user wants to keep the copy of local files
        final boolean isChecked = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.IS_ALLOW_KEEP, false);
        //getting the actual path of the image
//        String path = getPath(filePath);
        String uid = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, "");
        ArrayList<String> filePath = SQLiteDBManager.getInstance(context).getUploadStatusData(uid);

        //Uploading code
        for (int i = 0; i < filePath.size(); i++) {
            try {
                String uploadId = UUID.randomUUID().toString();
                /*File file =  new File(filePath.get(i));
                if (file.exists()) {
                    String name = filePath.get(i);
                    name = name.replace(".zip", "_up.zip");
                    File file1 = new File(name);
                    file1.mkdirs();
                }*/
                String url = WebAPIManager.URL + "upload";
                Log.e(TAG, "H: " + PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.AUTH, ""));
                Log.e(TAG, "F: " + filePath.get(i) + " ALLOW: " + !isChecked);
                //Creating a multi part request
                new MultipartUploadRequest(context, uploadId, url)
                        .setMethod("POST")
                        .addHeader("Authorization", PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.AUTH, ""))
                        //Adding file
                        .addFileToUpload(filePath.get(i), "fileToUpload")
                        .addParameter("user_id", PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, ""))
                        //Adding text parameter to the request
                        .setNotificationConfig(new UploadNotificationConfig())
                        .setAutoDeleteFilesAfterSuccessfulUpload(!isChecked)
                        .setUsesFixedLengthStreamingMode(true)
                        .setMaxRetries(3)
                        .setDelegate(new UploadStatusDelegate() {
                            @Override
                            public void onProgress(Context context, UploadInfo uploadInfo) {
                                // your code here
                            }

                            @Override
                            public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
                                // your code here
                                String uid = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, "");
                                String uploadId = uploadInfo.getUploadId();
                                ArrayList<String> ufStrings = uploadInfo.getSuccessfullyUploadedFiles();
                                SQLiteDBManager.getInstance(context).updateUploadStatusData(uid, ufStrings, 0);
                            }

                            @Override
                            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                // your code here
                                // if you have mapped your server response to a POJO, you can easily get it:
                                // YourClass obj = new Gson().fromJson(serverResponse.getBodyAsString(), YourClass.class);
                                if (isChecked) {
                                    String uid = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, "");
                                    String uploadId = uploadInfo.getUploadId();
                                    ArrayList<String> ufStrings = uploadInfo.getSuccessfullyUploadedFiles();
                                    SQLiteDBManager.getInstance(context).updateUploadStatusData(uid, ufStrings, 1);
                                } else {
                                    String uid = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, "");
                                    String uploadId = uploadInfo.getUploadId();
                                    ArrayList<String> ufStrings = uploadInfo.getSuccessfullyUploadedFiles();
                                    SQLiteDBManager.getInstance(context).deleteUploadedFile(uid, ufStrings, 1);
                                }
                            }

                            @Override
                            public void onCancelled(Context context, UploadInfo uploadInfo) {
                                // your code here
                                String uid = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.USER_ID, "");
                                String uploadId = uploadInfo.getUploadId();
                                ArrayList<String> ufStrings = uploadInfo.getSuccessfullyUploadedFiles();
                                SQLiteDBManager.getInstance(context).updateUploadStatusData(uid, ufStrings, 0);
                            }
                        })
                        //Starting the upload
                        .startUpload();
            } catch (Exception exc) {
                Toast.makeText(context, exc.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}