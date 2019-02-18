//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeTXPowerLevel extends AdElement {
    byte txpower;

    public TypeTXPowerLevel(byte[] data, int pos) {
        this.txpower = data[pos];
    }

    public String toString() {
        return "TX Power Level: " + Byte.toString(this.txpower) + " dBm";
    }

    public byte getTXPowerLevel() {
        return this.txpower;
    }
}
