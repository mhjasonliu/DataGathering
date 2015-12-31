package com.northwestern.habits.datagathering.banddata;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandInfo;

import java.util.EventListener;
import java.util.HashMap;

/**
 * Created by William on 12/31/2015.
 */
public abstract class DataManager {

    public DataManager(String sName) {
        studyName = sName;
    }

    HashMap<BandInfo, EventListener> listeners = new HashMap<>();
    HashMap<BandInfo, BandClient> clients = new HashMap<>();

    String studyName;

    protected abstract void subscribe(BandInfo info);
    protected abstract void unSubscribe(BandInfo info);



}
