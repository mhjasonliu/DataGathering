package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.banddata.BandDataService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnUserIdFragmentInterractionHandler} interface
 * to handle interaction events.
 * Use the {@link UserIDFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserIDFragment extends Fragment {

    private static final String TAG = "UserIDFragment";
    private Button rButton;
    private Context context;

    private OnUserIdFragmentInterractionHandler mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * <p/>
     * //     * @param param1 Parameter 1.
     * //     * @param param2 Parameter 2.
     *
     * @return A new instance of fragment UserIDFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserIDFragment newInstance() {
        UserIDFragment fragment = new UserIDFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public UserIDFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = container.getContext();

        // Create fragment to request new user ID
        View rootView = inflater.inflate(R.layout.fragment_user_id, container, false);
        rButton = (Button) rootView.findViewById(R.id.request_id_button);
        final Button requestButton = rButton;
        Button skipButton = (Button) rootView.findViewById(R.id.skip_button);

        final List<View> visibleList = new ArrayList<>();
        visibleList.add(rootView.findViewById(R.id.request_id_progress));

        final List<View> enableList = new ArrayList<>();
        enableList.add(requestButton);
        enableList.add(skipButton);


        requestButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestUserID(visibleList, enableList);
                    }
                }
        );

        skipButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        advanceScroll();
                    }
                }
        );

        // If there is no user ID, disable the continue button and freeze the scrolling
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notContainsID = !prefs.contains(Preferences.USER_ID);
        if (notContainsID) {
            skipButton.setEnabled(false);
            scrollLockRequest(true);
        } else {
            ((TextView) rootView.findViewById(R.id.text_user_id)).append(prefs.getString(Preferences.USER_ID, ""));
            rootView.findViewById(R.id.text_user_id).setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    public void scrollLockRequest(boolean shouldLock) {
        if (mListener != null) {
            mListener.onScrollLockRequest(shouldLock);
        }
    }

    public void advanceScroll() {
        if (mListener != null) {
            mListener.advanceScrollRequest();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnUserIdFragmentInterractionHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnUserIdFragmentInterractionHandler");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        context = null;
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
    public interface OnUserIdFragmentInterractionHandler {
        public void onScrollLockRequest(boolean shouldLock);

        public void advanceScrollRequest();

    }

    private class IdVerificationTask extends AsyncTask<Void, Void, Void> {

        List<View> hideViews, enableViews;
        String id;
        boolean success = false;
        String message = "";
        boolean skipButtonPreviousEnabled;

        public IdVerificationTask(List<View> h, List<View> e, String id) {
            hideViews = h;
            enableViews = e;
            this.id = id;
        }

        @Override
        protected void onPreExecute() {
            TextView sButton = ((TextView) UserIDFragment.this.getView().findViewById(R.id.skip_button));
            skipButtonPreviousEnabled = sButton.isEnabled();
            sButton.setEnabled(false);
            // Hide views
            for (View v : hideViews) {
                v.setVisibility(View.VISIBLE);
            }

            rButton.setEnabled(false);

            // Disable swiping
            scrollLockRequest(true);

            ((TextView) getView().findViewById(R.id.text_user_id)).setText("Requesting id...");
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String hostname = "107.170.25.202";
            int port = 3000;

            Socket socket = null;
//                PrintWriter writer = null;
            BufferedReader reader = null;
            BufferedWriter wr = null;


            try {
                String httpParams = URLEncoder.encode("user", "UTF-8")
                        + "=" + URLEncoder.encode(id, "UTF-8");
                Log.e(TAG, "Writing the socket");
                socket = new Socket(hostname, port);
                socket.isConnected();
//                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
                wr.write("POST /users HTTP/1.0\r\n");
                wr.write("Content-Length: " + httpParams.length() + "\r\n");
                wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
                wr.write("\r\n");
                wr.write("user=" + id);
                wr.flush();
                Log.v(TAG, "Socket written");

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                boolean firstLine = true;
                int code = -1;
                for (String line; (line = reader.readLine()) != null; ) {
                    if (line.isEmpty())
                        break; // Stop when headers are completed. We're not interested in all the HTML.
                    System.out.println(line);
                    if (firstLine) {
                        firstLine = false;
                        code = Integer.valueOf(line.substring(9, 12));
                        message = line.substring(13, line.length());
                    }
                }

                Log.v(TAG, "Code " + Integer.toString(code));
                success = code == 200;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                }
                if (wr != null) {
                    try {
                        wr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) try {
                    socket.close();
                } catch (IOException logOrIgnore) {
                }
            }


            if (success) {
                getContext().sendBroadcast(
                        new Intent(BandDataService.ACTION_USER_ID)
                                .putExtra(BandDataService.USER_ID_EXTRA, id));
                // Set user id preference in this process
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString(Preferences.USER_ID, id).apply();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Hide views
            for (View v : hideViews) {
                if (v != null) v.setVisibility(View.INVISIBLE);
            }

            final TextView v = ((TextView) UserIDFragment.this.getView().findViewById(R.id.text_user_id));
            final Button skipbutton = (Button) UserIDFragment.this.getView().findViewById(R.id.skip_button);
            v.setVisibility(View.VISIBLE);

            rButton.setEnabled(true);
            if (success) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Toast.makeText(getContext(), "User ID set to " + id,
                        Toast.LENGTH_SHORT).show();
                v.setText("User Id is: " + id);
                v.setVisibility(View.VISIBLE);
                skipbutton.setEnabled(true);

                // Unlock
                scrollLockRequest(false);
            } else {
                if (Objects.equals(message, "User name already exists")) {
                    // Launch a warning dialog that allows them to continue
                    Log.v(TAG, "duplicate id detected");
                    new AlertDialog.Builder(getContext()).setTitle("WARNING")
                            .setMessage(message)
                            .setPositiveButton("Continue with " + id, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    scrollLockRequest(false);
                                    getContext().sendBroadcast(
                                            new Intent(BandDataService.ACTION_USER_ID)
                                                    .putExtra(BandDataService.USER_ID_EXTRA, id));
                                    // Set user id preference in this process
                                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                                            .putString(Preferences.USER_ID, id).apply();

                                    Toast.makeText(getContext(), "User ID set to " + id,
                                            Toast.LENGTH_SHORT).show();
                                    v.setText("User Id is: " + id);
                                    v.setVisibility(View.VISIBLE);
                                    skipbutton.setEnabled(true);

                                    // Unlock
                                    scrollLockRequest(false);
                                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    v.setText(message + id);
                                    skipbutton.setEnabled(skipButtonPreviousEnabled);
                                    scrollLockRequest(skipButtonPreviousEnabled);
                                }
                            }).create().show();
                } else {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    v.setText(message);
                    skipbutton.setEnabled(skipButtonPreviousEnabled);
                    scrollLockRequest(skipButtonPreviousEnabled);
                }
            }
        }
    }

    private void requestUserID(final List<View> hideViews, final List<View> enableViews) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getContext());
        final EditText input = new EditText(getContext());
        builder
                .setTitle("User ID")
                .setMessage("Gvie the user a unique ID\n" +
                        "WARNING: the app is currently not configured to check if the ID is " +
                        "unique... please be creative.")
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String value = input.getText().toString();
                        if (input.getText().toString().trim().length() == 0) {
                            Toast.makeText(getContext(), "Empty User ID not acceptable", Toast.LENGTH_SHORT).show();
                        } else {
                            // Verify with server
                            new IdVerificationTask(hideViews, enableViews, value).execute();

                        }
                        // Close keyboard and dialog
                        InputMethodManager imm = (InputMethodManager) getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialog.dismiss();

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialog.dismiss();
                    }

                });

        builder.show();
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}
