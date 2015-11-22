package com.northwestern.habits.datagathering;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

import java.security.PublicKey;

/**
 * Created by William on 11/12/2015.
 */
public final class DataStorageContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DataStorageContract() {}

    /* Inner class that defines the table contents */
    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_CONTENT = "content";
        public static final String COLUMN_NAME_UPDATED = "updated";
        private static final String TEXT_TYPE = " TEXT";
        private static final String COMMA_SEP = ",";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_ENTRY_ID + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_CONTENT + TEXT_TYPE +
                " )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
    }

    public static class FeedReaderDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "FeedReader.db";

        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(FeedEntry.SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(FeedEntry.SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
    public static class DisplayDatabaseTask extends AsyncTask<Cursor, Void, Void> {
        @Override
        protected Void doInBackground(Cursor... params) {
            Cursor cursor = params[0];
            // Log the info
            cursor.moveToFirst();
            int id = cursor.getColumnIndexOrThrow(FeedEntry._ID);
            int entryTitle = cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE);

            while( !cursor.isAfterLast() ) {
                Log.i("", "Item ID is: " + cursor.getString(id));
                Log.i("", "Item title is: " + cursor.getString(entryTitle));
                cursor.moveToNext();
            }
            return null;
        }
    }
}
