package com.northwestern.habits.datagathering;

import com.microsoft.band.BandInfo;

/**
 * Created by William on 6/20/2016
 */
public class DeviceListItem {


    private DeviceType type;
    private String name;
    private String MAC;

    public DeviceListItem() {
        type = DeviceType.OTHER;
    }

    public DeviceListItem(BandInfo b) {
        name = b.getName();
        MAC = b.getMacAddress();
        type = DeviceType.BAND;
    }

    public enum DeviceType {
        BAND, OTHER, PHONE, NECKLACE
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public DeviceType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getMAC() {
        return MAC;
    }
}
