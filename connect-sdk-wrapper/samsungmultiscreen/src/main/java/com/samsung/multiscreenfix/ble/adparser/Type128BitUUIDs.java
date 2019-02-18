//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

import java.util.UUID;

public class Type128BitUUIDs extends AdElement {
    int type;
    UUID[] uuids;

    public Type128BitUUIDs(int type, byte[] data, int pos, int len) {
        this.type = type;
        int items = len / 16;
        this.uuids = new UUID[items];
        int ptr = pos;

        for(int i = 0; i < items; ++i) {
            long vl = (long)data[ptr] & 255L;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 8;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 16;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 24;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 32;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 40;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 48;
            ++ptr;
            vl |= ((long)data[ptr] & 255L) << 56;
            ++ptr;
            long vh = (long)data[ptr] & 255L;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 8;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 16;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 24;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 32;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 40;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 48;
            ++ptr;
            vh |= ((long)data[ptr] & 255L) << 56;
            ++ptr;
            this.uuids[i] = new UUID(vh, vl);
        }

    }

    public String toString() {
        StringBuffer sb = new StringBuffer("flags: ");
        switch(this.type) {
        case 4:
            sb.append("More 32-bit UUIDs: ");
            break;
        case 5:
            sb.append("Complete list of 32-bit UUIDs: ");
            break;
        default:
            sb.append("Unknown 32Bit UUIDs type: 0x" + Integer.toHexString(this.type) + ": ");
        }

        for(int i = 0; i < this.uuids.length; ++i) {
            if(i > 0) {
                sb.append(",");
            }

            sb.append(this.uuids[i].toString());
        }

        return new String(sb);
    }

    public int getType() {
        return this.type;
    }

    public UUID[] getUUIDs() {
        return this.uuids;
    }
}
