package com.northwestern.habits.datagathering;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.microsoft.band.BandInfo;

/**
 * Created by William on 11/16/2015.
 */
public class BandDatabase extends SQLiteOpenHelper {
    public BandDatabase(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    public int addBand (BandInfo band) {
        // TODO implement this method

        // Check to see if band is in the database

        // If it is, return band ID

        // Otherwise, add it and return band ID

        return 0;
    }

    /**
     * Puts the sensor
     * @param bandID
     * @param type
     * @return
     */
    public int addBandSensor ( int bandID, String type ) {
        //TODO implement method

        // Check to make sure ID is in database.

        // If not, throw an error

        // Check to see if the sensor type is in the database
        // If it is, return the id
        // otherwise, add it and return id

        return 0;
    }

    /**
     * Adds an entry for the band and sensor given
     * @param bandID an integer representing the ID of the band as held in the database
     * @param sensorID an integer representing the ID of the sensor as held in the database
     * @param values an array of strings that holds the date and time followed by sensor data values
     */
    public void addBandSensorEntry (int bandID, int sensorID, String[] values) {
        // Todo implement method
        // Make sure bandID and sensorID exist
        // If not, throw an error

        // Get the type of sensor to handle the values
        // Case block for the types of sensors

    }


}
