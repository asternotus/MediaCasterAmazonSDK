//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeFlags extends AdElement {
    public static int FLAGS_LE_LIMITED_DISCOVERABLE_MODE = 1;
    public static int FLAGS_LE_GENERAL_DISCOVERABLE_MODE = 2;
    public static int FLAGS_BR_EDR_NOT_SUPPORTED = 4;
    public static int FLAGS_SIMULTANEOUS_LE_CONTROLLER = 8;
    public static int FLAGS_SIMULTANEOUS_LE_HOST = 16;
    int flags;

    public TypeFlags(byte[] data, int pos) {
        this.flags = data[pos] & 255;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("flags:");
        if((this.flags & FLAGS_LE_LIMITED_DISCOVERABLE_MODE) != 0) {
            sb.append("LE Limited Discoverable Mode");
        }

        if((this.flags & FLAGS_LE_GENERAL_DISCOVERABLE_MODE) != 0) {
            if(sb.length() > 6) {
                sb.append(",");
            }

            sb.append("LE General Discoverable Mode");
        }

        if((this.flags & FLAGS_BR_EDR_NOT_SUPPORTED) != 0) {
            if(sb.length() > 6) {
                sb.append(",");
            }

            sb.append("BR/EDR Not Supported");
        }

        if((this.flags & FLAGS_SIMULTANEOUS_LE_CONTROLLER) != 0) {
            if(sb.length() > 6) {
                sb.append(",");
            }

            sb.append("Simultaneous LE and BR/EDR to Same Device Capable (Controller)");
        }

        if((this.flags & FLAGS_SIMULTANEOUS_LE_HOST) != 0) {
            if(sb.length() > 6) {
                sb.append(",");
            }

            sb.append("Simultaneous LE and BR/EDR to Same Device Capable (Host)");
        }

        return new String(sb);
    }

    public int getFlags() {
        return this.flags;
    }
}
