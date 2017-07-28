package com.northwestern.habits.datagathering.DataThreads;

import android.content.Context;

import com.northwestern.habits.datagathering.SendDataTask;

/**
 * Created by Y.Misal on 7/19/2017.
 */

public class SingletonDataTread {

    private static WriteDataThread mWriteDataThread = null;
    private static GyroWriteDataThread mGyroWriteDataThread = null;
    private static HeartWriteDataThread mHeartWriteDataThread = null;
    private static WriteDataMethods mWriteDataMethods = null;
    private static SendDataTask mSendDataTask = null;
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

    public static WriteDataThread getAccelDT(Context context) {
        if (mWriteDataThread == null) {
            synchronized(SingletonDataTread.class) {
                if (mWriteDataThread == null) {
                    mWriteDataThread = new WriteDataThread(context);
                }
            }
        }
        return mWriteDataThread;
    }

    public static GyroWriteDataThread getGyroDt(Context context) {
        if (mGyroWriteDataThread == null) {
            synchronized(SingletonDataTread.class) {
                if (mGyroWriteDataThread == null) {
                    mGyroWriteDataThread = new GyroWriteDataThread(context);
                }
            }
        }
        return mGyroWriteDataThread;
    }

    public static HeartWriteDataThread getHeartDT(Context context) {
        if (mHeartWriteDataThread == null) {
            synchronized(SingletonDataTread.class) {
                if (mHeartWriteDataThread == null) {
                    mHeartWriteDataThread = new HeartWriteDataThread(context);
                }
            }
        }
        return mHeartWriteDataThread;
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

    public static SendDataTask getSendDataTask(Context context) {
        if (mSendDataTask == null) {
            synchronized(SingletonDataTread.class) {
                if (mSendDataTask == null) {
                    mSendDataTask = new SendDataTask(context);
                }
            }
        }
        return mSendDataTask;
    }

}
