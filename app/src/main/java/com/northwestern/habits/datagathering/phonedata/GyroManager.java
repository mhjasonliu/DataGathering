package com.northwestern.habits.datagathering.phonedata;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import com.northwestern.habits.datagathering.DataStorageContract;

/**
 * Created by William on 1/23/2016
 */
public class GyroManager extends DataManager {
    public GyroManager(String sName, SQLiteOpenHelper db, Context context) {
        super(sName, "GyroManager", db, context);

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }


    @Override
    protected void subscribe() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void unSubscribe() {
        mSensorManager.unregisterListener(this, mSensor);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event != null) {

            String T_Gyro = "Gyroscope";

            int studyId, devId, sensId;
            try {
                studyId = getStudyId(studyName, database);
            } catch (Resources.NotFoundException e) {

                // study not found, use lowest available
                studyId = getNewStudy(database);


                // Write the study into database, save the id
                ContentValues values = new ContentValues();
                values.put(DataStorageContract.StudyTable.COLUMN_NAME_STUDY_ID, studyName);
                values.put(DataStorageContract.StudyTable._ID, studyId);
                database.insert(
                        DataStorageContract.StudyTable.TABLE_NAME,
                        null,
                        values
                );
            }

            try {
                devId = getDevId(location, mac, studyId, database);
            } catch (Resources.NotFoundException e) {
                devId = getNewDev(database);

                // Write new Device into database, save the id
                ContentValues values = new ContentValues();
                values.put(DataStorageContract.DeviceTable._ID, devId);
                values.put(DataStorageContract.DeviceTable.COLUMN_NAME_STUDY_ID, studyId);
                values.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_PHONE);
                values.put(DataStorageContract.DeviceTable.COLUMN_NAME_MAC, mac);
                values.put(DataStorageContract.DeviceTable.COLUMN_NAME_LOCATION, location);

                database.insert(
                        DataStorageContract.DeviceTable.TABLE_NAME,
                        null,
                        values
                );
            }

            try {
                sensId = getSensorId(T_Gyro, devId, database);
            } catch (Resources.NotFoundException e) {
                sensId = getNewSensor(database);

                // Write new sensor into database, save id
                ContentValues values = new ContentValues();
                values.put(DataStorageContract.SensorTable._ID, sensId);
                values.put(DataStorageContract.SensorTable.COLUMN_NAME_DEVICE_ID, devId);
                values.put(DataStorageContract.SensorTable.COLUMN_NAME_TYPE, T_Gyro);

                database.insert(
                        DataStorageContract.SensorTable.TABLE_NAME,
                        null,
                        values
                );
            }

            // Add new entry to the gyro table
            Log.v(TAG, "Study name is: " + studyName);
            Log.v(TAG, "Study Id is: " + Integer.toString(studyId));
            Log.v(TAG, "Device ID is: " + Integer.toString(devId));
            Log.v(TAG, "Sensor ID is: " + Integer.toString(sensId));
            Log.v(TAG, "X: " + Double.toString(event.values[0]) +
                    "Y: " + Double.toString(event.values[1]) +
                    "Z: " + Double.toString(event.values[2]));
            Log.v(TAG, getDateTime(event));

            ContentValues values = new ContentValues();
            values.put(DataStorageContract.GyroTable.COLUMN_NAME_DATETIME, getDateTime(event));
            values.put(DataStorageContract.GyroTable.COLUMN_NAME_SENSOR_ID, sensId);
            values.put(DataStorageContract.GyroTable.COLUMN_NAME_X, event.values[0]);
            values.put(DataStorageContract.GyroTable.COLUMN_NAME_Y, event.values[1]);
            values.put(DataStorageContract.GyroTable.COLUMN_NAME_Z, event.values[2]);


            database.insert(DataStorageContract.GyroTable.TABLE_NAME, null, values);
        }

    }
}
