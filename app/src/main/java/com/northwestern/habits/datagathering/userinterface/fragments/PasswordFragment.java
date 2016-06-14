package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.northwestern.habits.datagathering.DataGatheringApplication;
import com.northwestern.habits.datagathering.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnPasswordFragmentInterractionListener} interface
 * to handle interaction events.
 * Use the {@link PasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PasswordFragment extends Fragment {

    private OnPasswordFragmentInterractionListener mListener;
    private static final String TAG = "PasswordFragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PasswordFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PasswordFragment newInstance() {
        PasswordFragment fragment = new PasswordFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_password, container, false);

        EditText oldPword = (EditText) rootView.findViewById(R.id.old_password_field);
        EditText newPwd = (EditText) rootView.findViewById(R.id.new_password_field);
        EditText confirmPwd = (EditText) rootView.findViewById(R.id.confirm_password_field);

        // TODO Set up the checkbox to change when correct old password is set
        oldPword.addTextChangedListener(new TextWatcher() {
            private final String pwd = getContext().getSharedPreferences(
                    DataGatheringApplication.PREFS_NAME,0).getString(
                    DataGatheringApplication.PREF_USER_ID, "");

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Compare to the password
                if (s.toString().equals(pwd) ) {
                    Log.v(TAG, "Equal!");
                } else {
                    Log.v(TAG, "Not equal: '" + s.toString() + "' != '" + pwd + "'");
                }
            }
        });

        // TODO Set up the checkbox to change when the new passwords match
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPasswordFragmentInterractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPasswordFragmentInterractionListener");
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
    public interface OnPasswordFragmentInterractionListener {
    }

}
