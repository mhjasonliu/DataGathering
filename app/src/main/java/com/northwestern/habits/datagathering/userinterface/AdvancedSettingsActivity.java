package com.northwestern.habits.datagathering.userinterface;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.northwestern.habits.datagathering.DataGatheringApplication;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.userinterface.fragments.PasswordFragment;
import com.northwestern.habits.datagathering.userinterface.fragments.UserIDFragment;

public class AdvancedSettingsActivity extends Activity
    implements UserIDFragment.OnUserIdFragmentScrollLockHandler {

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

        // Check for no User ID
        if (getApplicationContext().getSharedPreferences(DataGatheringApplication.PREFS_NAME,0).contains(DataGatheringApplication.PREF_USER_ID)) {
            Log.e(TAG, "No user ID");
//            UserIDFragment.newInstance().IdRequestTask()
        }
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
    public void advanceScroll() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
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
                case 2: // Create fragment to set
                    return PasswordFragment.newInstance();
            }
            return PlaceholderFragment.newInstance(position + 1);
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
}
