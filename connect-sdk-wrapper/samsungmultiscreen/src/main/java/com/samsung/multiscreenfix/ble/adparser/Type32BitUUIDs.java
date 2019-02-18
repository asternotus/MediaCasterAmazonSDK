//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class Type32BitUUIDs extends AdElement {
    int type;
    int[] uuids;

    public Type32BitUUIDs(int type, byte[] data, int pos, int len) {
        this.type = type;
        int items = len / 4;
        this.uuids = new int[items];
        int ptr = pos;

        for(int i = 0; i < items; ++i) {
            int v = data[ptr] & 255;
            ++ptr;
            v |= (data[ptr] & 255) << 8;
            ++ptr;
            v |= (data[ptr] & 255) << 16;
            ++ptr;
            v |= (data[ptr] & 255) << 24;
            ++ptr;
            this.uuids[i] = v;
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
        case 21:
            sb.append("Service UUIDs: ");
            break;
        default:
            sb.append("Unknown 32Bit UUIDs type: 0x" + Integer.toHexString(this.type) + ": ");
        }

        for(int i = 0; i < this.uuids.length; ++i) {
            if(i > 0) {
                sb.append(",");
            }

            sb.append("0x" + hex32(this.uuids[i]));
        }

        return new String(sb);
    }

    public int getType() {
        return this.type;
    }

    public int[] getUUIDs() {
        return this.uuids;
    }
}
