package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.northwestern.habits.datagathering.CustomListeners.WriteDataThread;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Sends a bunch of bytes on the google API client
 * Created by William on 1/29/2017.
 */
public class SendDataTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private String nodeId;

    public static boolean isZippingData = false;
    public static boolean isSendingData = false;

    private static final String TAG = "SendDataTask";
    private static final long SLEEP_TIME = 3000;

    public SendDataTask(Context context) {
        mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        nodeId = mContext.getSharedPreferences(Preferences.PREFERENCE_NAME, 0)
                .getString(Preferences.KEY_PHONE_ID, null);
        Log.e(TAG, "SendTask");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!isZippingData) { // Do not risk trying to send deleted files and vice versa
            isZippingData = true;
            createZipFromData();
            isZippingData = false;
        }
        Log.e(TAG, "sending files count ");
        if (mGoogleApiClient.isConnected()) {
            Log.e(TAG, "mGoogleApiClient.isConnected()");
            List<Node> nodeList = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
            for (Node node : nodeList) {
                if (node.isNearby()) {
                    if (!isSendingData) { // Do not risk trying to send deleted files and vice versa
                        isSendingData = true;
                        Log.e(TAG, "nodeid " + nodeId);
                        if (nodeId == null) return null;
                        // Top level file
                        for (int i = 0; i < mPath.size(); i++) {
                            File zipFile = new File(mPath.get(i));
                            Log.e(TAG, "zipfile path " + zipFile.getAbsolutePath());
                            sendZipFile(zipFile);
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "mGoogleApiClient.isConnected() false");
        }
        return null;
    }

    private ArrayList<String> mPath = null;
    private void createZipFromData() {
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission denied");
        }
        File folder = new File(mContext.getExternalFilesDir(null).getPath());
        Log.w(TAG, "Creating zip " + folder.getAbsolutePath());
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
                            Log.e(TAG, "zipfile added to " + mPath);
                        } catch (IOException e) {
                            Log.v(TAG, "Zip err " + file.getAbsolutePath());
                            writeError(e, mContext);
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
                isSendingData = false;
            } else {
                Log.e(TAG, "files not available for zip.");
            }
        } else {
            Log.e(TAG, "file not exists.");
        }
    }

    // For to Delete the directory inside list of files and inner Directory
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                Log.v(TAG, "delete zip " + children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    //create zip file and write all the content from directory into
    private void zipFiles(File directory, File zipfile) {
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
            Log.v(TAG, "zip finished");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            writeError(e, mContext);
        } catch (IOException e) {
            writeError(e, mContext);
            e.printStackTrace();
        } finally {
            try {
                res.close();
            } catch (IOException e) {
                writeError(e, mContext);
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

    // send zip files to connected device
    private void sendZipFile(final File file) {
        if (file.exists()) {
            String path = file.getPath().substring(mContext.getExternalFilesDir(null).getPath().length() + 1);
            Log.v(TAG, "***************** *********** Opening channel... " + path);
            ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(mGoogleApiClient, nodeId, path)
                    .await();//(22, TimeUnit.SECONDS);
            if (result.getStatus().isSuccess()) {
                //WriteDataThread.writeLogs( "Channel opened for file transmission" + "_" + System.currentTimeMillis(), mContext );
                final Channel channel = result.getChannel();
                if (result.getStatus().isSuccess()) {
                    channel.addListener(mGoogleApiClient, new MyChannelListener());
//                    Toast.makeText(mContext, "File transfer start.", Toast.LENGTH_SHORT).show();
                    com.google.android.gms.common.api.Status status = channel.sendFile(mGoogleApiClient, Uri.fromFile(file))
                            .await();//(12, TimeUnit.SECONDS);
                    if (status.getStatusCode() == 0) {
                        Log.v(TAG, "Sending csv " + file.getName() + " from ");
                        Log.v(TAG, " for path " + file.getAbsolutePath());
                        /*SystemClock.sleep(SLEEP_TIME);
                        if (file.delete()) { Log.v(TAG, "File successfully deleted");
                            WriteDataThread.writeLogs( "File transferred successfully" + "_" + System.currentTimeMillis(), mContext );
                        }*/
                        isSendingData = false;
                    }
                } else {
                    Log.v(TAG, "Channel failed. " + result.getStatus().toString());
                }
            }
        }
    }

    private class MyChannelListener implements ChannelApi.ChannelListener {

        @Override
        public void onChannelOpened(Channel channel) {}

        @Override
        public void onChannelClosed(Channel channel, int closeReason,
                                    int appSpecificErrorCode) {
            Log.e(TAG, "Wear+onChannelClosed " + channel.getPath() + " closeReason=" + closeReason + " appSpecificErrorCode=" + appSpecificErrorCode);
        }

        @Override
        public void onInputClosed(Channel channel, int closeReason,
                                  int appSpecificErrorCode) {
            // File transfer is finished
            Log.e(TAG, "Wear+onInputClosed " + channel.getPath() + " closeReason=" + closeReason + " appSpecificErrorCode=" + appSpecificErrorCode);
        }

        @Override
        public void onOutputClosed(Channel channel, int closeReason,
                                   int appSpecificErrorCode) {
            Log.e(TAG, "Wear+onOutputClosed " + channel.getPath() + " closeReason=" + closeReason + " appSpecificErrorCode=" + appSpecificErrorCode);
            switch (closeReason) {
                case CLOSE_REASON_NORMAL:
                    SystemClock.sleep(SLEEP_TIME);
                    File folder = new File(mContext.getExternalFilesDir(null).getPath()+"/"+channel.getPath());
                    Log.e(TAG, "File path: " + folder.getAbsolutePath());
                    if (folder.exists()) {
                        folder.delete();
//                        Toast.makeText(mContext, "File sent.", Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "File successfully deleted " + folder.getAbsolutePath());
                        //WriteDataThread.writeLogs( "File transferred successfully" + "_" + System.currentTimeMillis(), mContext );
                    } else {
                        Log.v(TAG, "File not exists " + folder.getAbsolutePath());
                    }
                    break;
            }
        }
    }

    private void writeError(Throwable e, Context context) {
        Log.e(TAG, "WRITING ERROR TO DISK: \n");
        e.printStackTrace();

        String PATH = context.getExternalFilesDir(null) + "/WearData/ERRORS/";
        File folder = new File(PATH);
        if (!folder.exists()) folder.mkdirs();

        Calendar c = Calendar.getInstance();
        File errorReport = new File(folder.getPath()
                + "/Exception_"
                + c.get(Calendar.HOUR_OF_DAY)
                + c.get(Calendar.MINUTE)
                + c.get(Calendar.SECOND)
                + ".txt");

        FileWriter writer = null;
        try {
            writer = new FileWriter(errorReport, true);
            writer.write("\n\n-----------------BEGINNING OF EXCEPTION-----------------\n\n");
            writer.write(e.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}