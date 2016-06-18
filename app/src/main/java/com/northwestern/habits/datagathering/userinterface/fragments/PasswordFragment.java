package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.userinterface.AdvancedSettingsActivity;

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

    private CheckBox oldPasswordBox;
    private CheckBox newPasswordBox;
    private EditText oldPword;
    private EditText newPwd;
    private EditText confirmPwd;
    private Button changePasswordButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_password, container, false);

        oldPasswordBox = (CheckBox) rootView.findViewById(R.id.old_password_checkbox);
        newPasswordBox = (CheckBox) rootView.findViewById(R.id.comparison_checkbox);
        oldPasswordBox.setOnCheckedChangeListener(checkedChangeListener);
        newPasswordBox.setOnCheckedChangeListener(checkedChangeListener);

        oldPword = (EditText) rootView.findViewById(R.id.old_password_field);
        newPwd = (EditText) rootView.findViewById(R.id.new_password_field);
        confirmPwd = (EditText) rootView.findViewById(R.id.confirm_password_field);

        oldPword.setOnFocusChangeListener(hideKeyboardListener);
        newPwd.setOnFocusChangeListener(hideKeyboardListener);
        confirmPwd.setOnFocusChangeListener(hideKeyboardListener);

        changePasswordButton = (Button) rootView.findViewById(R.id.button_change_password);
        changePasswordButton.setOnClickListener(changePasswordListener);

        // Set up the checkbox to change when correct old password is set
        oldPword.addTextChangedListener(new TextWatcher() {
            private final String pwd = getContext().getSharedPreferences(Preferences.NAME, 0)
                    .getString(Preferences.PASSWORD, "");

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                oldPasswordBox.setChecked(s.toString()
                        .equals(getContext().getSharedPreferences(Preferences.NAME, 0)
                                .getString(Preferences.PASSWORD, "")));
            }
        });


        // Set up the checkbox to change when the new passwords match
        newPwd.addTextChangedListener(newPasswordWatcher);
        confirmPwd.addTextChangedListener(newPasswordWatcher);

        ((AdvancedSettingsActivity)getActivity()).mViewPager.addOnPageChangeListener(
                new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {

                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        View v = rootView;
                        InputMethodManager mgr = (InputMethodManager) (getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE));
                        mgr.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
        );

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


    /* *************************** LISTENERS/HANDLERS *************************************** */

    private TextWatcher newPasswordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            newPasswordBox.setChecked(comparePasswords());
        }
    };

    private boolean comparePasswords() {
        return newPwd.getText().toString().equals(confirmPwd.getText().toString())
                && !newPwd.getText().toString().equals("");
    }

    private CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            changePasswordButton.setEnabled(areBoxesChecked());
        }
    };

    private boolean areBoxesChecked() {
        return newPasswordBox.isChecked() && oldPasswordBox.isChecked();
    }

    private View.OnClickListener changePasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPreferences.Editor editor = getContext().getSharedPreferences(Preferences.NAME, 0).edit();
            editor.putString(Preferences.PASSWORD, newPwd.getText().toString());
            editor.apply();

            oldPword.setText("");
            newPwd.setText("");
            confirmPwd.setText("");
        }
    };

    private View.OnFocusChangeListener hideKeyboardListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            InputMethodManager mgr = (InputMethodManager) (v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE));
            if (!hasFocus) {
                mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
            } else {
                mgr.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    };


}
