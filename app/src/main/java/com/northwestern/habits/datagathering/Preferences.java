package com.northwestern.habits.datagathering;

import com.microsoft.band.sensors.SampleRate;

/**
 * Created by William on 6/14/2016
 */
public class Preferences {

    // Preference fields
    public static final String USER_ID = "userID";
    public static final String USER_ID1 = "user_id";
    public static final String PASSWORD = "password";
    public static final String AUTH = "api_auth";
    public static final String IS_ALLOW_KEEP = "allow_keep_files";

    @Deprecated
    public static final String CURRENT_DOCUMENT = "documentID";
    public static final String LABEL = "";

    /** List of mac addresses intended to be used as entries in shared preferences */
    public static final String REGISTERED_DEVICES = "registered_devices";


    public static String getDeviceKey(String mac) {
        return mac;
    }

    public static String getSensorKey(String mac, String sensor) {
        return  mac + "_" + sensor; }

    public static String getFrequencyKey(String mac, String sensor) {
        return mac + "_" + sensor + "_frequency";
    }


    public static String sampleRateToString(SampleRate rate) {
        switch (rate) {
            case MS128:
                return "8Hz";
            case MS32:
                return "31Hz";
            case MS16:
                return "62Hz";
            default:
                return "true";
        }
    }

    protected static SampleRate stringToRate(String s) {
        switch (s) {
            case "8Hz":
                return SampleRate.MS128;
            case "31Hz":
                return SampleRate.MS32;
            case "62Hz":
                return SampleRate.MS16;
            default:
                return SampleRate.MS128;
        }
    }

    public static final String ACCEL = "accelerometer";
    public static final String ALT = "altimeter";
    public static final String AMBIENT = "ambient";
    public static final String BAROMETER = "barometer";
    public static final String CALORIES = "calories";
    public static final String CONTACT = "contact";
    public static final String DISTANCE = "distance";
    public static final String GSR = "gsr";
    public static final String GYRO = "gyro";
    public static final String HEART = "heartRate";
    public static final String PEDOMETER = "pedometer";
    public static final String SKIN_TEMP = "skinTemperature";
    public static final String UV = "ultraViolet";

    public static final String IS_EATING = "is_eating";

}
