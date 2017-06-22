package com.northwestern.habits.datagathering.userinterface.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.northwestern.habits.datagathering.Preferences;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.userinterface.UserActivity;
import com.northwestern.habits.datagathering.webapi.WebAPIManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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
//        new UserIdAsyncTask( getActivity(), "", "o", "n" ).execute();
    }

    private CheckBox oldPasswordBox;
    private CheckBox newPasswordBox;
    private EditText oldPword;
    private EditText newPwd;
    private EditText confirmPwd;
    private Button changePasswordButton;
    private Button finishButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_password, container, false);

        finishButton = (Button) rootView.findViewById(R.id.button_finish_advanced);
        changePasswordButton = (Button) rootView.findViewById(R.id.button_change_password);

        oldPasswordBox = (CheckBox) rootView.findViewById(R.id.old_password_checkbox);
        newPasswordBox = (CheckBox) rootView.findViewById(R.id.comparison_checkbox);

        oldPword = (EditText) rootView.findViewById(R.id.old_password_field);
        newPwd = (EditText) rootView.findViewById(R.id.new_password_field);
        confirmPwd = (EditText) rootView.findViewById(R.id.confirm_password_field);

//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams
//                .SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        finishButton.setOnClickListener(finishClickListener);
        changePasswordButton.setOnClickListener(changePasswordListener);

        oldPasswordBox.setOnCheckedChangeListener(checkedChangeListener);
        newPasswordBox.setOnCheckedChangeListener(checkedChangeListener);

        oldPword.setOnFocusChangeListener(editTextFocusListener);
        newPwd.setOnFocusChangeListener(editTextFocusListener);
        confirmPwd.setOnFocusChangeListener(editTextFocusListener);
        confirmPwd.setOnKeyListener(finalEnterListener);

        // Set up the checkbox to change when correct old password is set
        oldPword.addTextChangedListener(new TextWatcher() {
            private final String pwd = PreferenceManager.getDefaultSharedPreferences(getActivity())
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
                        .equals(PreferenceManager.getDefaultSharedPreferences(getActivity())
                                .getString(Preferences.PASSWORD, "")));
            }
        });

        // store/update user on remote server
//        new UserIdAsyncTask( getActivity(), "", "o", "n" ).execute();

        // Set up the checkbox to change when the new passwords match
        newPwd.addTextChangedListener(newPasswordWatcher);
        confirmPwd.addTextChangedListener(newPasswordWatcher);

        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Preferences.PASSWORD, "")
                .equals("")) {
            oldPasswordBox.setChecked(true);
            oldPword.setEnabled(false);
            finishButton.setEnabled(false);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPasswordFragmentInterractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPasswordFragmentInteractionListener");
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
            InputMethodManager mgr = (InputMethodManager) (v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE));
            mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);

            String flag = "";
            if (oldPword.getText().toString().trim().length() == 0) {
                flag = "login";
            } else {
                flag = "reset";
            }

            String url = WebAPIManager.URL + flag;

            String url1 = "http://192.168.100.166/lumen/public/" + flag;
            Log.e("URL login: ", url);
            String uname = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Preferences.USER_ID, "");
            Object obj = new String[] { uname, newPwd.getText().toString(), oldPword.getText().toString() };
            UserIdAsyncTask userIdAsyncTask = new UserIdAsyncTask( url, flag, obj );
            userIdAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

    private View.OnFocusChangeListener editTextFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            Log.v(TAG, "focus change started");
            InputMethodManager mgr = (InputMethodManager) (v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE));

            if (!hasFocus) {
                // Hide the keyboard
                mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
            } else {
                // Enable the keyboard
                mgr.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
            Log.v(TAG, "focus change ended");
        }
    };

    private View.OnKeyListener finalEnterListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER
                    && changePasswordButton.isEnabled()) {
               changePasswordButton.callOnClick();
            }
            return false;
        }
    };

    private View.OnClickListener finishClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            ActivityManager mngr = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);

            List<ActivityManager.AppTask> taskList = mngr.getAppTasks();

            if (getActivity().isTaskRoot()) {
                // This is the only activity shown, start new UserActivity and finish this one
                Intent i = new Intent(getActivity(), UserActivity.class);
                startActivity(i);
                getActivity().finish();
            } else {
                getActivity().onBackPressed();
            }
        }
    };

    // asynk task
    private class UserIdAsyncTask extends AsyncTask<Void, Void, String> {

        private String url = "";
        private Object mObject = null;
        private String mFlag = "";
        private ProgressDialog dialog;

        public UserIdAsyncTask( String url, String flag, Object obj ) {
            this.url = url;
            this.mObject = obj;
            this.mFlag = flag;
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(), "", "Please wait...");
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String str = "";
            try {
                str = WebAPIManager.httpPOSTRequest(this.url, this.mObject, this.mFlag );
                return str;
            } catch (Exception e) {
                Log.e(UserIDFragment.class.toString(), e.getMessage(), e);
            }
            return str;
        }

        @Override
        protected void onPostExecute(String response) {
            dialog.dismiss();

//            {"status":"success","data":{"user_id":3},"message":"User Login successful"}
//            {"status":"success","data":{"user_id":5},"message":"User Login successful","auth":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9oYWJpdHMub3JnIiwiYXVkIjoiaHR0cDpcL1wvaGFiaXRzLmNvbSIsImlhdCI6MTQ5MjYwMjI3NywidXNlcm5hbWUiOiJ0ZXN0MSJ9.IUdUq1X2gYOnScwMtanDK36SPfQMBWV-Yugwf7cxhMA"}

            /*SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            editor.putString(Preferences.PASSWORD, newPwd.getText().toString());
            editor.apply();

            oldPword.setText("");
            newPwd.setText("");
            confirmPwd.setText("");

            // Enable things that were disabled before
            finishButton.setEnabled(true);
            oldPword.setEnabled(true);

            // Alert the user that password changed
            Toast.makeText(getActivity(), "Password changed", Toast.LENGTH_SHORT).show();*/

           if (response.equalsIgnoreCase("fail") || response.equalsIgnoreCase("failed") || response.equalsIgnoreCase("error")) {
               String msg = "Failed to login the user.";
               if (this.mFlag.equalsIgnoreCase("reset")) {
                   msg = "Failed to reset the password.";
               }
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            } else {
               try {
                   JSONObject jsonObject = new JSONObject(response);
                   if (jsonObject.getString("status").equalsIgnoreCase("error")) {
                       Toast.makeText(getActivity(), "Failed to login the user.", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                   SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                   editor.putString(Preferences.PASSWORD, newPwd.getText().toString());
                   editor.putString(Preferences.AUTH, jsonObject.optString("auth"));
                   editor.putString(Preferences.USER_ID1, jsonObject1.optString("user_id"));
                   editor.apply();

                   oldPword.setText("");
                   newPwd.setText("");
                   confirmPwd.setText("");

                   // Enable things that were disabled before
                   finishButton.setEnabled(true);
                   oldPword.setEnabled(true);

                   // Alert the user that password changed
                   Toast.makeText(getActivity(), "Password changed", Toast.LENGTH_SHORT).show();
               } catch (JSONException e) {
                   e.printStackTrace();
               }
            }
        }
    }
}