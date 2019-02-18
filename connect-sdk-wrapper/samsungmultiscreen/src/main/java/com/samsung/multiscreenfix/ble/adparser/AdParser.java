//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

import java.util.ArrayList;

public class AdParser {
    public AdParser() {
    }

    public static ArrayList<AdElement> parseAdData(byte[] data) {
        int pos = 0;
        ArrayList out = new ArrayList();

        int bpos;
        int blen;
        for(int dlen = data.length; pos + 1 < dlen; pos = bpos + blen + 1) {
            bpos = pos;
            blen = data[pos] & 255;
            if(blen == 0 || pos + blen > dlen) {
                break;
            }

            ++pos;
            int type = data[pos] & 255;
            ++pos;
            int len = blen - 1;
            Object e;
            switch(type) {
            case 1:
                e = new TypeFlags(data, pos);
                break;
            case 2:
            case 3:
            case 20:
                e = new Type16BitUUIDs(type, data, pos, len);
                break;
            case 4:
            case 5:
                e = new Type32BitUUIDs(type, data, pos, len);
                break;
            case 6:
            case 7:
            case 21:
                e = new Type128BitUUIDs(type, data, pos, len);
                break;
            case 8:
            case 9:
                e = new TypeString(type, data, pos, len);
                break;
            case 10:
                e = new TypeTXPowerLevel(data, pos);
                break;
            case 13:
            case 14:
            case 15:
            case 16:
                e = new TypeByteDump(type, data, pos, len);
                break;
            case 17:
                e = new TypeSecOOBFlags(data, pos);
                break;
            case 18:
                e = new TypeSlaveConnectionIntervalRange(data, pos, len);
                break;
            case 22:
                e = new TypeServiceData(data, pos, len);
                break;
            case 255:
                e = new TypeManufacturerData(data, pos, len);
                break;
            default:
                e = new TypeUnknown(type, data, pos, len);
            }

            out.add(e);
        }

        return out;
    }
}
