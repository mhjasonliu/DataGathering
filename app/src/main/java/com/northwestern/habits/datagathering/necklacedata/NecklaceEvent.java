package com.northwestern.habits.datagathering.necklacedata;

/**
 * Created by William on 1/24/2016
 */
public class NecklaceEvent {

    private byte byte0;
    private byte byte1;
    private byte byte2;
    private byte byte3;
    private byte byte4;
    private byte byte5;
    private byte byte6;


    NecklaceEvent (byte[] bytes) {
        byte0 = bytes[0];
        byte1 = bytes[1];
        byte2 = bytes[2];
        byte3 = bytes[3];
        byte4 = bytes[4];
        byte5 = bytes[5];
        byte6 = bytes[6];
    }


    public byte getByte0() {return byte0;}
    public byte getByte1() {return byte1;}
    public byte getByte2() {return byte2;}
    public byte getByte3() {return byte3;}
    public byte getByte4() {return byte4;}
    public byte getByte5() {return byte5;}
    public byte getByte6() {return byte6;}


}
