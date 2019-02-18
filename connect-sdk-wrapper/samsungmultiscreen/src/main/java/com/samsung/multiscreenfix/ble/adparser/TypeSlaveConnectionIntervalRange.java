//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeSlaveConnectionIntervalRange extends AdElement {
    int connIntervalMin = '\uffff';
    int connIntervalMax = '\uffff';

    public TypeSlaveConnectionIntervalRange(byte[] data, int pos, int len) {
        if(len >= 4) {
            int v = data[pos] & 255;
            int ptr = pos + 1;
            v |= (data[ptr] & 255) << 8;
            ++ptr;
            this.connIntervalMin = v;
            v = data[ptr] & 255;
            ++ptr;
            v |= (data[ptr] & 255) << 8;
            ++ptr;
            this.connIntervalMax = v;
        }

    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Slave Connection Interval Range: ");
        sb.append("conn_interval_min: ");
        if(this.connIntervalMin == '\uffff') {
            sb.append("none");
        } else {
            sb.append(Float.toString((float)this.connIntervalMin * 1.25F) + " msec");
        }

        sb.append(",conn_interval_max: ");
        if(this.connIntervalMax == '\uffff') {
            sb.append("none");
        } else {
            sb.append(Float.toString((float)this.connIntervalMax * 1.25F) + " msec");
        }

        return new String(sb);
    }

    public int getConnIntervalMin() {
        return this.connIntervalMin;
    }

    public int getConnIntervalMax() {
        return this.connIntervalMax;
    }
}
