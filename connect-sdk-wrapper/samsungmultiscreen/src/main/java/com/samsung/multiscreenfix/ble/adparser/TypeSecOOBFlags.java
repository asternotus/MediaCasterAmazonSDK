//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeSecOOBFlags extends AdElement {
    public static int FLAGS_OOB_DATA_PRESENT = 1;
    public static int FLAGS_LE_SUPPORTED_HOST = 2;
    public static int FLAGS_SIMULTANEOUS_LE_BR_EDR = 4;
    public static int FLAGS_RANDOM_ADDRESS = 8;
    int flags;

    public TypeSecOOBFlags(byte[] data, int pos) {
        this.flags = data[pos] & 255;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("OOB Flags: ");
        if((this.flags & FLAGS_OOB_DATA_PRESENT) != 0) {
            sb.append("OOB data present");
        }

        if((this.flags & FLAGS_LE_SUPPORTED_HOST) != 0) {
            if(sb.length() > 10) {
                sb.append(",");
            }

            sb.append("LE supported (Host)");
        }

        if((this.flags & FLAGS_SIMULTANEOUS_LE_BR_EDR) != 0) {
            if(sb.length() > 10) {
                sb.append(",");
            }

            sb.append("Simultaneous LE and BR/EDR to Same Device Capable (Host)");
        }

        if(sb.length() > 10) {
            sb.append(",");
        }

        if((this.flags & FLAGS_RANDOM_ADDRESS) != 0) {
            sb.append("Random Address");
        } else {
            sb.append("Public Address");
        }

        return new String(sb);
    }

    public int getFlags() {
        return this.flags;
    }
}
