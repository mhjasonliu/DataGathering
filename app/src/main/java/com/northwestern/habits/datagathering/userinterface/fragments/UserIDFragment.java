package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.banddata.BandDataService;

import java.util.ArrayList;
import java.util.List;

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
     * <p>
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
        visibleList.add(rootView.findViewById(R.id.requesting_id_text));

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
                new View.OnClickListener()
                {
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
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnUserIdFragmentInterractionHandler {
        public void onScrollLockRequest(boolean shouldLock);

        public void advanceScrollRequest();

    }

    private void requestUserID(List<View> hideViews, final List<View> enableViews) {

        // set the visibility for progress bar
        for (View view : hideViews) {
            view.setVisibility(View.INVISIBLE);
        }

        rButton.setEnabled(true);


        // Disable swiping
        scrollLockRequest(false);


        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
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
                            InputMethodManager imm = (InputMethodManager) getContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                            dialog.dismiss();
                        } else {
                            getContext().sendBroadcast(
                                    new Intent(BandDataService.ACTION_USER_ID)
                                            .putExtra(BandDataService.USER_ID_EXTRA, value));
                            // Set user id preference in this process
                            PreferenceManager.getDefaultSharedPreferences(context).edit()
                                    .putString(Preferences.USER_ID, value).apply();

                            Toast.makeText(getContext(), "User ID set to " + value,
                                    Toast.LENGTH_SHORT).show();
                            for (View view : enableViews) {
                                view.setEnabled(true);
                            }
                            InputMethodManager imm = (InputMethodManager) getContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                            dialog.dismiss();
                        }

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
