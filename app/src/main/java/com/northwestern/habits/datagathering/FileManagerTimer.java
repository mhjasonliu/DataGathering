package com.northwestern.habits.datagathering;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.TimerTask;

/**
 * Created by William on 6/28/2016.
 */
public class FileManagerTimer extends TimerTask {

    private Context mContext;
    private static final String TAG = "FileManagerTimer";

    public FileManagerTimer(Context c) {
        super();
        mContext = c;
    }

    @Override
    public void run() {
        Log.e(TAG, "Running!");
        // Get the current hour
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        DataGatheringApplication appInstance = DataGatheringApplication.getInstance();

        String pathToPrev = DataGatheringApplication.getDataFilePath(mContext, hour - 1);
        String pathToAttachments = DataGatheringApplication.getAttachmentDirectory(mContext);

        File parentDir = new File(pathToPrev);
        File destDir = new File(pathToAttachments);

        String id = mContext.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE)
                .getString(Preferences.CURRENT_DOCUMENT, "");
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File f : parentDir.listFiles()) {
                // Add attachment to the document
                try {
                    InputStream stream = new FileInputStream(f);
                    Document doc = appInstance.getDatabase().getDocument(id);
                    UnsavedRevision newRev = doc.createRevision();
                    newRev.setAttachment(f.getName(), "csv", stream);
                    newRev.save();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }



                // Move the file to attachments
                try {
                    moveFile(f, destDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void moveFile(File file, File dir) throws IOException {
        File newFile = new File(dir, file.getName());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            outputChannel = new FileOutputStream(newFile).getChannel();
            inputChannel = new FileInputStream(file).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            file.delete();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }

    }
}
