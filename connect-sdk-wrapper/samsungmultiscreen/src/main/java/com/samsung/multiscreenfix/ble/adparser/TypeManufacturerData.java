//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeManufacturerData extends AdElement {
    int manufacturer;
    byte[] b;

    public TypeManufacturerData(byte[] data, int pos, int len) {
        int v = data[pos] & 255;
        int ptr = pos + 1;
        v |= (data[ptr] & 255) << 8;
        ++ptr;
        this.manufacturer = v;
        int blen = len - 2;
        this.b = new byte[blen];
        System.arraycopy(data, ptr, this.b, 0, blen);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Manufacturer data (manufacturer: " + hex16(this.manufacturer) + "): ");

        for(int i = 0; i < this.b.length; ++i) {
            if(i > 0) {
                sb.append(",");
            }

            int v = this.b[i] & 255;
            sb.append(hex8(v));
        }

        return new String(sb);
    }

    public String getManufacturer() {
        StringBuffer sb = new StringBuffer(hex16(this.manufacturer));
        return new String(sb);
    }

    public byte[] getBytes() {
        return this.b;
    }
}
