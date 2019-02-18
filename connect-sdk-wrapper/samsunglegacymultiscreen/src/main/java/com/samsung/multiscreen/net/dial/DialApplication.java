//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.dial;

import java.util.HashMap;
import java.util.Map;

public class DialApplication {
    private String name;
    private String state;
    private String relLink;
    private String hrefLink;
    private boolean stopAllowed;
    Map<String, String> options = new HashMap();

    public DialApplication() {
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[DialApplication]").append(" name: ").append(this.getName()).append(", state: ").append(this.getState()).append(", relLink: ").append(this.getRelLink()).append(", hrefLink: ").append(this.getHrefLink()).append(", stopAllowed:").append(this.isStopAllowed());
        return builder.toString();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return this.state;
    }

    protected void setState(String state) {
        this.state = state;
    }

    public void setStopAllowed(boolean allowed) {
        this.stopAllowed = allowed;
    }

    public boolean isStopAllowed() {
        return this.stopAllowed;
    }

    public String getRelLink() {
        return this.relLink;
    }

    public void setRelLink(String relLink) {
        this.relLink = relLink;
    }

    public String getHrefLink() {
        return this.hrefLink;
    }

    public void setHrefLink(String hrefLink) {
        this.hrefLink = hrefLink;
    }

    public void setOption(String name, String value) {
        this.options.put(name, value);
    }

    public Map<String, String> getOptions() {
        return this.options;
    }
}
