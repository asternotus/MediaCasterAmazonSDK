//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.Map;

public class ApplicationInfo {
    private static final String PROPERTY_ID = "id";
    private static final String PROPERTY_STATE = "running";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_VERSION = "version";
    private final String id;
    private final boolean running;
    private final String name;
    private final String version;

    static ApplicationInfo create(Map<String, Object> info) {
        if(info == null) {
            throw new NullPointerException("info");
        } else {
            String id = (String)info.get("id");
            boolean running = ((Boolean)info.get("running")).booleanValue();
            String name = (String)info.get("name");
            String version = (String)info.get("version");
            return new ApplicationInfo(id, running, name, version);
        }
    }

    public String getId() {
        return this.id;
    }

    public boolean isRunning() {
        return this.running;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof ApplicationInfo)) {
            return false;
        } else {
            ApplicationInfo other = (ApplicationInfo)o;
            if(!other.canEqual(this)) {
                return false;
            } else {
                String this$id = this.getId();
                String other$id = other.getId();
                if(this$id == null) {
                    if(other$id != null) {
                        return false;
                    }
                } else if(!this$id.equals(other$id)) {
                    return false;
                }

                if(this.isRunning() != other.isRunning()) {
                    return false;
                } else {
                    String this$name = this.getName();
                    String other$name = other.getName();
                    if(this$name == null) {
                        if(other$name != null) {
                            return false;
                        }
                    } else if(!this$name.equals(other$name)) {
                        return false;
                    }

                    String this$version = this.getVersion();
                    String other$version = other.getVersion();
                    if(this$version == null) {
                        if(other$version != null) {
                            return false;
                        }
                    } else if(!this$version.equals(other$version)) {
                        return false;
                    }

                    return true;
                }
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ApplicationInfo;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        String $id = this.getId();
        int result1 = result * 59 + ($id == null?0:$id.hashCode());
        result1 = result1 * 59 + (this.isRunning()?79:97);
        String $name = this.getName();
        result1 = result1 * 59 + ($name == null?0:$name.hashCode());
        String $version = this.getVersion();
        result1 = result1 * 59 + ($version == null?0:$version.hashCode());
        return result1;
    }

    public String toString() {
        return "ApplicationInfo(id=" + this.getId() + ", running=" + this.isRunning() + ", name=" + this.getName() + ", version=" + this.getVersion() + ")";
    }

    private ApplicationInfo(String id, boolean running, String name, String version) {
        if(id == null) {
            throw new NullPointerException("id");
        } else if(name == null) {
            throw new NullPointerException("name");
        } else if(version == null) {
            throw new NullPointerException("version");
        } else {
            this.id = id;
            this.running = running;
            this.name = name;
            this.version = version;
        }
    }
}
