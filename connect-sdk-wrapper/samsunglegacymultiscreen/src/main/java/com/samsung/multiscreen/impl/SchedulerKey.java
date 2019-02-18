//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.impl;

public class SchedulerKey {
    private final SchedulerKey.SchedulerKeyType type;
    private final String stringInfo;

    public SchedulerKey(SchedulerKey.SchedulerKeyType type, String stringInfo) {
        this.type = type;
        this.stringInfo = stringInfo;
    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        int result1 = 31 * result + (this.stringInfo == null?0:this.stringInfo.hashCode());
        result1 = 31 * result1 + (this.type == null?0:this.type.hashCode());
        return result1;
    }

    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(obj == null) {
            return false;
        } else if(this.getClass() != obj.getClass()) {
            return false;
        } else {
            SchedulerKey other = (SchedulerKey)obj;
            if(this.stringInfo == null) {
                if(other.stringInfo != null) {
                    return false;
                }
            } else if(!this.stringInfo.equals(other.stringInfo)) {
                return false;
            }

            return this.type == other.type;
        }
    }

    public static enum SchedulerKeyType {
        CONNECTED_CHANNEL_POLL,
        PING_TIMEOUT;

        private SchedulerKeyType() {
        }
    }
}
