//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeByteDump extends AdElement {
    byte[] b;
    int type;

    public TypeByteDump(int type, byte[] data, int pos, int len) {
        this.type = type;
        this.b = new byte[len];
        System.arraycopy(data, pos, this.b, 0, len);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        switch(this.type) {
        case 13:
            sb.append("Class of device: ");
            break;
        case 14:
            sb.append("Simple Pairing Hash C: ");
            break;
        case 15:
            sb.append("Simple Pairing Randomizer R: ");
            break;
        case 16:
            sb.append("TK Value: ");
        }

        for(int i = 0; i < this.b.length; ++i) {
            if(i > 0) {
                sb.append(",");
            }

            int v = this.b[i] & 255;
            sb.append(hex8(v));
        }

        return new String(sb);
    }

    public int getType() {
        return this.type;
    }

    public byte[] getBytes() {
        return this.b;
    }
}
