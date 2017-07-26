package com.northwestern.habits.datagathering.DataThreads;

import android.content.Context;

/**
 * Created by Y.Misal on 7/19/2017.
 */

public class SingletonDataTread {

    private static WriteDataMethods mWriteDataMethods = null;
    private static volatile SingletonDataTread instance = null;

    private SingletonDataTread( ) {}

    public static SingletonDataTread getInstance() {
        if (instance == null) {
            synchronized(SingletonDataTread.class) {
                if (instance == null) {
                    instance = new SingletonDataTread();
                }
            }
        }
        return instance;
    }

    public static WriteDataMethods getWriteMethodDT(Context context) {
        if (mWriteDataMethods == null) {
            synchronized(SingletonDataTread.class) {
                if (mWriteDataMethods == null) {
                    mWriteDataMethods = new WriteDataMethods(context);
                }
            }
        }
        return mWriteDataMethods;
    }

}
