package com.northwestern.habits.datagathering.userinterface;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.northwestern.habits.datagathering.CustomViewPager;
import com.northwestern.habits.datagathering.DataGatheringApplication;
import com.northwestern.habits.datagathering.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AdvancedSettingsActivity extends Activity {

    private static final String TAG = "AdvancedSettings";

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
    protected CustomViewPager mViewPager;


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
            return PlaceholderFragment.newInstance(position + 1, mViewPager);
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

        private CustomViewPager mPager;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, CustomViewPager pager) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            fragment.mPager = pager;
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int viewIndex = getArguments().getInt(ARG_SECTION_NUMBER);
            Log.v(TAG, "Creating view! " + viewIndex);


            View rootView = inflater.inflate(R.layout.fragment_advanced_settings, container, false);

            // Get the views that you want to manipulate from the rootView
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);

            String titleText;
            switch (viewIndex) {
                case 1:
                    // Create fragment to request new subject ID

                    titleText = "Subject ID request fragment (aka section" +
                            Integer.toString(viewIndex) + ")";
                    textView.setText(titleText);

                    rootView = inflater.inflate(R.layout.fragment_subject_id, container, false);
                    final Button requestButton = (Button) rootView.findViewById(R.id.request_id_button);
                    Button continueButton = (Button) rootView.findViewById(R.id.continue_button);

                    final List<View> visibleList = new ArrayList<>();
                    visibleList.add(rootView.findViewById(R.id.request_id_progress));
                    visibleList.add(rootView.findViewById(R.id.requesting_id_text));

                    final List<View> enableList = new ArrayList<>();
                    enableList.add(requestButton);
                    enableList.add(continueButton);
                    final CustomViewPager pager = mPager;

                    requestButton.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Perform the request
                                    new IdRequestTask(visibleList, enableList, pager).execute();

                                    // Make necessary views appear
                                    for (View view : visibleList) {
                                        view.setVisibility(View.VISIBLE);
                                    }

                                    // Make necessary views disappear
                                    for (View view : enableList) {
                                        view.setEnabled(false);
                                    }

                                    // Disable swiping
                                    pager.setPagingEnabled(false);
                                }
                            }
                    );

                    final SharedPreferences prefs =
                            getContext().getSharedPreferences(
                                    DataGatheringApplication.PREFS_NAME, MODE_PRIVATE);
                    continueButton.setOnClickListener(

                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.remove(DataGatheringApplication.PREF_USER_ID);
                                    editor.apply();
                                    pager.setCurrentItem(1, true);
                                }
                            }
                    );


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


        private class IdRequestTask extends AsyncTask<Void,Void,Void> {
            private String mResponse = "";
            private List<View> hideViews;
            List<View> enableViews;
            private CustomViewPager pager;

            public IdRequestTask(List<View> hViews, List<View> eViews, CustomViewPager p) {
                super();
                hideViews = hViews;
                enableViews = eViews;
                pager = p;
            }

            @Override
            protected Void doInBackground(Void[] params) {
                String dataUrl = "https://vfsmpmapps10.fsm.northwestern.edu/php/getUserID.cgi";
                String dataUrlParameters = "";
                URL url;
                HttpURLConnection connection = null;
                try {
                    // Create connection
                    Log.v(TAG, "connecting...");
                    url = new URL(dataUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                    connection.setRequestProperty("Content-Length","" + Integer.toString(dataUrlParameters.getBytes().length));
                    connection.setRequestProperty("Content-Language", "en-US");
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    // Send request
                    Log.v(TAG, "Requesting...");
                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.writeBytes(dataUrlParameters);
                    wr.flush();
                    wr.close();
                    // Get Response
                    Log.v(TAG, "Reading response...");
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                    mResponse = response.toString();
                    Log.v(TAG, mResponse);

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                // set the visibility for progress bar
                for (View view : hideViews) {
                    view.setVisibility(View.INVISIBLE);
                }

                // Disable the buttons
                for (View view : enableViews) {
                    view.setEnabled(true);
                }

                // Disable swiping
                pager.setPagingEnabled(true);


                final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
                if (mResponse.equals("")) {
                    // Handle failure
                    alertBuilder.setTitle("Failed to get ID");
                    alertBuilder.setMessage("Make sure that you are connected to the internet.\n" +
                            "You may need to connect to a Northwestern VPN.");
                    alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                } else {
                    // TODO Handle success
                    alertBuilder.setTitle("Success!");
                    alertBuilder.setMessage("Successfully received an ID for this user. (" +
                            mResponse + ")");
                    SharedPreferences prefs = getContext().getSharedPreferences(
                            DataGatheringApplication.PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(DataGatheringApplication.PREFS_NAME, mResponse);
                    editor.apply();

                    alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                }
                alertBuilder.create().show();


            }
        }
    }
}
