//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.ble.adparser;

public class TypeString extends AdElement {
    int type;
    String s;

    public TypeString(int type, byte[] data, int pos, int len) {
        this.type = type;
        byte[] sb = new byte[len];
        System.arraycopy(data, pos, sb, 0, len);
        this.s = new String(sb);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        switch(this.type) {
        case 8:
            sb.append("Short local name: ");
            break;
        case 9:
            sb.append("Local name: ");
            break;
        default:
            sb.append("Unknown string type: 0x" + Integer.toString(this.type) + ": ");
        }

        sb.append("\'");
        sb.append(this.s);
        sb.append("\'");
        return new String(sb);
    }

    public int getType() {
        return this.type;
    }

    public String getString() {
        return this.s;
    }
}
