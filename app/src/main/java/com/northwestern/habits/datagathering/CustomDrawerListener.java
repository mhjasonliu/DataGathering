package com.northwestern.habits.datagathering;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

import com.northwestern.habits.datagathering.userinterface.AdvancedSettingsActivity;

/**
 * Created by William on 6/14/2016.
 */
public class CustomDrawerListener
        implements NavigationView.OnNavigationItemSelectedListener {
    private Context mContext;
    private DrawerLayout mDrawer;

    public CustomDrawerListener(Context c, DrawerLayout v) {
        mContext = c;
        mDrawer = v;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.advanced_options) {
            String password = mContext.getSharedPreferences(Preferences.NAME, 0)
                    .getString(Preferences.PASSWORD, "");
            promptAdminPassword();
        } else if (id == R.id.activity_history) {

        } else if (id == R.id.eating_probability) {

        } else if (id == R.id.settings) {

        } else if (id == R.id.feedback) {

        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void promptAdminPassword() {
//        startActivityForResult();

//        Intent intent = new Intent(mContext, AdvancedSettingsActivity.class);
//        mContext.startActivity(intent);

        new CustomAlertDialog(mContext).show();
    }


    private static class CustomAlertDialog extends AlertDialog {
        String password;

        private void startAdvancedSettings(Context context) {
            Intent i = new Intent(context, AdvancedSettingsActivity.class);
            context.startActivity(i);
        }

        protected CustomAlertDialog(final Context context) {
            super(context);
            final EditText input = new EditText(context);
            password = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE)
                    .getString(Preferences.PASSWORD, "");

            boolean passwordExists = true;
            // No password, just let them in
//            if (password.equals("")) {
//                startAdvancedSettings(context);
//                passwordExists = false;
//            }

            if (passwordExists) {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());

                setView(input);
                setTitle("Password required for Advanced Settings");
                this.setButton(BUTTON_POSITIVE, "Enter", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.v("a", "Password: '" + password + "', given: '" + input.getText() + "'");

                        // Check password and possibly start new activity
                        if ((input.getText().toString()).equals(password)) {
                            // Correct password, let them in
                            startAdvancedSettings(context);
                        } else {
                            // Let them know that their password was wrong
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setCancelable(true);
                            builder.setTitle("Wrong password");

                            builder.setPositiveButton("Retry", new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancel();
                                    new CustomAlertDialog(context).show();
                                }
                            });
                            builder.setNegativeButton("Cancel", new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancel();
                                }
                            });
                            builder.create().show();
                        }
                    }
                });

                this.setButton(BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
            } else {
                cancel();
            }
        }
    }
}
