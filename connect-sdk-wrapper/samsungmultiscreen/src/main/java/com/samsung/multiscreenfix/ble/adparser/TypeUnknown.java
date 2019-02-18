//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeUnknown extends AdElement {
    int unknownType;
    byte[] unknownData;

    public TypeUnknown(int type, byte[] data, int pos, int len) {
        this.unknownType = type;
        this.unknownData = new byte[len];
        System.arraycopy(data, pos, this.unknownData, 0, len);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("unknown type: 0x" + Integer.toHexString(this.unknownType) + " ");

        for(int i = 0; i < this.unknownData.length; ++i) {
            if(i > 0) {
                sb.append(",");
            }

            int v = this.unknownData[i] & 255;
            sb.append("0x" + Integer.toHexString(v));
        }

        return new String(sb);
    }

    public int getType() {
        return this.unknownType;
    }

    public byte[] getBytes() {
        return this.unknownData;
    }
}
