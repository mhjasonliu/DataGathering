package com.northwestern.habits.datagathering.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Y.Misal on 6/7/2017.
 */

public class SQLiteDBManager  extends SQLiteOpenHelper {

    public static final String TAG = "SQLiteDBManager";
    public static final int DATABASE_VERSION = 1;   // Database Version
    public static final String DATABASE_NAME = "habitsDB";  // Database Name
    public static final String UPLOAD_STATUS_TABLE = "uploadStatus";  // Database Name

    private static final String CREATE_TABLE_UPLOAD_STATUS = "CREATE TABLE IF NOT EXISTS " + UPLOAD_STATUS_TABLE
            + "(" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "user_id TEXT, " + "file_name TEXT, " + "upload_status INTEGER " + ")";

    public SQLiteDBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static SQLiteDBManager mSQLiteDBManager = null;

    //Create a static method to get instance.
    public static SQLiteDBManager getInstance(Context context){
        if(mSQLiteDBManager == null){
            mSQLiteDBManager = new SQLiteDBManager(context);
        }
        return mSQLiteDBManager;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_UPLOAD_STATUS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void addUploadStatus(String uid, ArrayList<String> filename, int upload_status, String uploadID) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (String f : filename) {
            ContentValues values = new ContentValues();
            values.put("user_id", uid);
            values.put("file_name", f);
            values.put("upload_status", upload_status);
            db.insertWithOnConflict(UPLOAD_STATUS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            Log.e(TAG, "insert  :" + f);
//            Long id = db.insert(UPLOAD_STATUS_TABLE, null, values);
        }
        db.close();
    }

    //update reminder time in user table
    public void updateUploadStatusData(String user_id, ArrayList<String> filename, int upload_status) {

        SQLiteDatabase db    = this.getWritableDatabase();
        for (String f : filename) {
            ContentValues values = new ContentValues();
            values.put("upload_status", upload_status);
            db.update(UPLOAD_STATUS_TABLE, values, "user_id='" + user_id + "'" + " AND file_name='" + f + "'", null);
            Log.e(TAG, "update  :" + f);
        }
        db.close();
    }

    public void deleteUploadedFile(String user_id, ArrayList<String> filename, int upload_status) {

        SQLiteDatabase db    = this.getWritableDatabase();
        for (String f : filename) {
            ContentValues values = new ContentValues();
            values.put("file_name", f);
            values.put("upload_status", upload_status);
            db.delete(UPLOAD_STATUS_TABLE, "file_name=?", new String[]{f});
//            db.dele(UPLOAD_STATUS_TABLE, values, "user_id= '" + user_id + "'" + " AND file_name=" + filename , null);
            Log.e(TAG, "delete :" + f);
        }
        db.close();
    }

    //get data from table
    public ArrayList<String> getUploadStatusData(String uid) {
        ArrayList<String> stringArrayList = new ArrayList();
        // Select All Query
        String selectQuery = "SELECT * FROM " + UPLOAD_STATUS_TABLE + " WHERE upload_status=0";
        SQLiteDatabase db  = this.getWritableDatabase();
        Cursor cursor      = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    Log.i( "_id =", "" + cursor.getInt(0) + " Path: " + cursor.getString(2) );
                    stringArrayList.add(cursor.getString(2));
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        db.close(); //
        return stringArrayList;
    }

}
