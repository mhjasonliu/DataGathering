package com.northwestern.habits.datagathering;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class CrashActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CrashActivity ";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.activity_crash);
        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
    }

    @Override
    public void onClick (View v)
    {
        Log.v(TAG, "Clicked!");
        if (v == findViewById(R.id.button)) {
            sendLogFile();
        } else {
            moveTaskToBack(true);
            finish();
        }

    }

    private void sendLogFile ()
    {
        String fullName = extractLogToFile();
        if (fullName == null)
            return;

        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType ("text/plain");
        intent.putExtra (Intent.EXTRA_EMAIL, new String[] {"williamstogin2018@u.northwestern.edu"});
        intent.putExtra (Intent.EXTRA_SUBJECT, "MyApp log file");
        intent.putExtra (Intent.EXTRA_STREAM, Uri.parse("file://" + fullName));
        intent.putExtra (Intent.EXTRA_TEXT, "Log file attached."); // do this so some email clients don't complain about empty body.
        startActivity (intent);
        //moveTaskToBack(true);
        finish();
    }

    private String extractLogToFile()
    {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo (this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        // Make file name - file must be saved to external storage or it wont be readable by
        // the email app.
        String path = Environment.getExternalStorageDirectory() + "/" + "Bandv2/CrashLogs/";
        Calendar c = Calendar.getInstance();
        String fullName = path + "Error_Report" + c.toString() + ".log";

        // Extract to file.
        File file = new File (fullName);
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                this.sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStreamReader reader = null;
        FileWriter writer = null;
        try
        {
            // For Android 4.0 and earlier, you will get all app's log output, so filter it to
            // mostly limit it to your app's output.  In later versions, the filtering isn't needed.
            String cmd = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) ?
                    "logcat -d -v time MyApp:v dalvikvm:v System.err:v *:s" :
                    "logcat -d -v time";

            // get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());

            // write output stream
            writer = new FileWriter(file);
            writer.write ("Android version: " +  Build.VERSION.SDK_INT + "\n");
            writer.write ("Device: " + model + "\n");
            writer.write ("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");

            char[] buffer = new char[10000];
            do
            {
                int n = reader.read (buffer, 0, buffer.length);
                if (n == -1)
                    break;
                writer.write (buffer, 0, n);
            } while (true);

            reader.close();
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            // You might want to write a failure message to the log here.
            Log.e(TAG, "Failed to send log");
            return null;
        }
//        System.exit(1);
        return fullName;
    }
}
