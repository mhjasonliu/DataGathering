package com.northwestern.habits.datagathering;

/**
 * Created by William on 6/14/2016.
 */
public class Preferences {

    public static final String NAME = "my_prefs";

    // Preference fields
    public static final String USER_ID = "userID";
    public static final String PASSWORD = "password";

    /** List of mac addresses intended to be used as entries in shared preferences */
    public static final String REGISTERED_DEVICES = "registered_devices";


    public static String getDeviceKeyFromMac(String mac) {
        return mac + "_ID";
    }

    public static String getSensorKey(String mac, String sensor) {
        return  mac + "_" + sensor; }

}
