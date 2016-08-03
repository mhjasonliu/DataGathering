package com.northwestern.habits.datagathering.userinterface;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandInfo;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.northwestern.habits.datagathering.DeviceListItem;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.userinterface.fragments.DevicesFragment;
import com.northwestern.habits.datagathering.userinterface.fragments.PasswordFragment;
import com.northwestern.habits.datagathering.userinterface.fragments.UserIDFragment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdvancedSettingsActivity extends Activity
        implements UserIDFragment.OnUserIdFragmentInterractionHandler,
        PasswordFragment.OnPasswordFragmentInterractionListener,
        DevicesFragment.OnDevicesFragmentInterractionListener,
        HeartRateConsentListener {

    private static final String TAG = "AdvancedSettings";
    private List<DeviceListItem> devices;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The CustomViewPager that will host the section contents.
     */
    public CustomViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(pageChangeListener);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_advanced_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollLockRequest(boolean shouldLock) {
        mViewPager.setPagingEnabled(!shouldLock);
    }

    @Override
    public void advanceScrollRequest() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
    }

    public void retreatScroll() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }

    @Override
    public void onRequestDeviceRegistration(List<DeviceListItem> deviceItems) {
        devices = deviceItems;
        BandInfo[] bands = BandClientManager.getInstance().getPairedBands();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor e = prefs.edit();
        Set<String> previouslyRegistered = prefs.getStringSet(Preferences.REGISTERED_DEVICES, new HashSet<String>());
        String myAddress = BluetoothAdapter.getDefaultAdapter().getAddress();

        final Set<String> currentlyRegistered = new HashSet<>(previouslyRegistered);
        currentlyRegistered.add(Preferences.getDeviceKey(myAddress));
        previouslyRegistered.remove(Preferences.getDeviceKey(myAddress));

        for (BandInfo band : bands) {
            currentlyRegistered.add(Preferences.getDeviceKey(band.getMacAddress()));
            previouslyRegistered.remove(Preferences.getDeviceKey(band.getMacAddress()));

            // Request heart rate for that device
            com.microsoft.band.sensors.BandSensorManager manager =
                    BandClientManager.getInstance().create(this, band).getSensorManager();

            if (manager.getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                // Request heart rate consent
                Log.v(TAG, "Requesting heart rate consent");
                // Get permission
                manager.requestHeartRateConsent(this, this);
            }
        }

        e.putStringSet(Preferences.REGISTERED_DEVICES, currentlyRegistered);
        e.apply();

        for (final String oldMac: previouslyRegistered) {
            AlertDialog.Builder b =
            new AlertDialog.Builder(this).setTitle("WARNING").setMessage("A device with the MAc " +
                    "address " + oldMac + " was previously paired to this phoen, but it is no longer." +
                    " Please either re-pair the device or forget the device");
            b.setNegativeButton("Forget", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentlyRegistered.remove(oldMac);
                    e.putStringSet(Preferences.REGISTERED_DEVICES, currentlyRegistered);
                    e.apply();
                }
            });

            b.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });

            b.create().show();
        }
    }

    @Override
    public void userAccepted(boolean b) {
        // Do something maybe?
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0: // Create fragment to request new subject ID
                    return UserIDFragment.newInstance();
                case 1: // Create fragment to set devices/sensors
                    return DevicesFragment.newInstance();
                case 2: // Create fragment to set
                    return PasswordFragment.newInstance();
                default:
                    return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int viewIndex = getArguments().getInt(ARG_SECTION_NUMBER);


            View rootView = inflater.inflate(R.layout.fragment_advanced_settings, container, false);

            // Get the views that you want to manipulate from the rootView
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);

            String titleText;
            switch (viewIndex) {
                case 1:
                    // Create fragment to request new subject ID

                    rootView = UserIDFragment.newInstance().onCreateView(inflater,
                            container, savedInstanceState);

                    break;
                case 2:
                    // Create fragment to add sensors


                    titleText = "Sensor management fragment (aka section" +
                            Integer.toString(viewIndex) + ")";
                    textView.setText(titleText);
                    break;
                case 3:
                    // Create fragment to set password


                    titleText = "Password fragment (aka section" +
                            Integer.toString(viewIndex) + ")";
                    textView.setText(titleText);
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }


            return rootView;
        }
    }


    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            // Update selection
            Drawable selected = getDrawable(R.drawable.circle_filled);
            Drawable notSelected = getDrawable(R.drawable.circle_empty);

            LinearLayout ll = (LinearLayout) findViewById(R.id.indicator_layout);
            for (int i = 0; i < ll.getChildCount(); i++) {
                if (i == position) {
                    ll.getChildAt(i).setBackground(selected);
                } else {
                    ll.getChildAt(i).setBackground(notSelected);
                }
            }

            // Registering devices with the backend
//            if (position == 1) {
//                SharedPreferences prefs = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
//                Set<String> macs = prefs.getStringSet(Preferences.REGISTERED_DEVICES, new HashSet<String>());
//
//                // Register devices if necessary
//                LinkedList<String> toBeRegistered = new LinkedList<>();
//                for (DeviceListItem item :
//                        devices) {
//                    if (!macs.contains(item.getMAC())) {
//                        toBeRegistered.add(item.getMAC());
//                    }
//                }
//
//                if (toBeRegistered.size() > 0) {
////                    new DeviceRegistrationTask().execute(toBeRegistered);
//                }
//
//            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            AdvancedSettingsActivity outerThis = AdvancedSettingsActivity.this;
            // If we are scrolling, we don't want any keyboards up
            InputMethodManager mgr = (InputMethodManager) (outerThis
                    .getSystemService(Context.INPUT_METHOD_SERVICE));
            mgr.hideSoftInputFromWindow(outerThis.getWindow().getDecorView().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    };


    private class DeviceRegistrationTask extends AsyncTask<List<String>, Void, HashMap<String, String>> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Alert user that devices are being registered
//            ProgressDialog.Builder builder = new ProgressDialog.Builder(AdvancedSettingsActivity.this);
//            builder.setTitle("Registering Devices");
//            builder.setMessage("Please wait while your devices are being registered with " +
//                    "the back end server.");
//            builder.setCancelable(false);
//            ProgressDialog dialog = builder.create();
//
//            builder.create().show();

            progressDialog = new ProgressDialog(AdvancedSettingsActivity.this);

            //set the icon, title and progress style..

            progressDialog.setIcon(android.R.drawable.ic_popup_sync);

            progressDialog.setTitle("Registering Devices");

            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

            progressDialog.show();
        }

        @Override
        protected HashMap<String, String> doInBackground(List<String>[] params) {
            List<String> macs = params[0];
            HashMap<String, String> ids = new HashMap<>();

            SharedPreferences.Editor e =
                    PreferenceManager.getDefaultSharedPreferences(AdvancedSettingsActivity.this).edit();
            HttpURLConnection connection = null;

            try {
                for (String mac :
                        macs) {
                    ids.put(mac, "");
                    String dataUrl = "https://vfsmpmapps10.fsm.northwestern.edu/php/getDeviceID.cgi";
                    String dataUrlParameters = "MacAddress=" + mac;
                    URL url;
                    // Create connection
//                Log.v(TAG, "connecting...");
                    url = new URL(dataUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Content-Length", "" + Integer.toString(dataUrlParameters.getBytes().length));
                    connection.setRequestProperty("Content-Language", "en-US");
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    // Send request
//                Log.v(TAG, "Requesting...");
                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.writeBytes(dataUrlParameters);
                    wr.flush();
                    wr.close();
                    // Get Response
//                Log.v(TAG, "Reading response...");
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                    String mResponse;
                    mResponse = response.toString();
                    Log.v(TAG, mResponse);
                    e.putString(Preferences.getDeviceKey(mac), mResponse);
                    ids.put(mac, mResponse);
                }
            } catch (Exception ex) {

                ex.printStackTrace();

            } finally {
                e.apply();
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return ids;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> responses) {
            super.onPostExecute(responses);
            AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedSettingsActivity.this);
            builder.setCancelable(false);

            progressDialog.cancel();
            if (responses.containsValue("")) {
                // Post failure: ask for internet and kick the user back to the previous page
                builder.setTitle("Failure");
                builder.setMessage("Failed to register the devices. Please make sure that you" +
                        " are connected to the internet.");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AdvancedSettingsActivity.this.retreatScroll();
                    }
                });
            } else {
                // Post success
                builder.setTitle("Success!");
                builder.setMessage("Successfully received IDs for the devices. (" +
                        responses.keySet() + ") (" + responses.values() + ")");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                // Add added devices to the list of mac addresses
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AdvancedSettingsActivity.this);
                Set<String> devices = new HashSet<>(prefs.getStringSet(Preferences.REGISTERED_DEVICES, new HashSet<String>()));
                devices.addAll(responses.keySet());
                SharedPreferences.Editor e = prefs.edit();
                e.putStringSet(Preferences.REGISTERED_DEVICES, devices);
                e.apply();
            }
            builder.create().show();
        }
    }
}
