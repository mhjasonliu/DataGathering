package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link UserIDFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link UserIDFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserIDFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String TAG = "UserIDFragment";

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserIDFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserIDFragment newInstance(String param1, String param2) {
        UserIDFragment fragment = new UserIDFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public UserIDFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Create fragment to request new user ID
        View rootView = inflater.inflate(R.layout.fragment_subject_id, container, false);
        final Button requestButton = (Button) rootView.findViewById(R.id.request_id_button);
        Button continueButton = (Button) rootView.findViewById(R.id.continue_button);

        final List<View> visibleList = new ArrayList<>();
        visibleList.add(rootView.findViewById(R.id.request_id_progress));
        visibleList.add(rootView.findViewById(R.id.requesting_id_text));

        final List<View> enableList = new ArrayList<>();
        enableList.add(requestButton);
        enableList.add(continueButton);
        final CustomViewPager pager = new CustomViewPager(getContext());

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
                        DataGatheringApplication.PREFS_NAME, Context.MODE_PRIVATE);
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

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
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
                        DataGatheringApplication.PREFS_NAME, Context.MODE_PRIVATE);
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
