//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.Map;

public class Device {
    private static final String DUID_KEY = "duid";
    private static final String MODEL_KEY = "model";
    private static final String DESCRIPTION_KEY = "description";
    private static final String NETWORK_TYPE_KEY = "networkType";
    private static final String SSID_KEY = "ssid";
    private static final String IP_KEY = "ip";
    private static final String FIRMWARE_VERSION_KEY = "firmwareVersion";
    private static final String NAME_KEY = "name";
    private static final String ID_KEY = "id";
    private static final String UDN_KEY = "udn";
    private static final String RESOLUTION_KEY = "resolution";
    private static final String COUNTRY_CODE_KEY = "countryCode";
    private static final String PLATFORM_KEY = "OS";
    private static final String WIFIMAC_KEY = "wifiMac";
    private final String duid;
    private final String model;
    private final String description;
    private final String networkType;
    private final String ssid;
    private final String ip;
    private final String firmwareVersion;
    private final String name;
    private final String id;
    private final String udn;
    private final String resolution;
    private final String countryCode;
    private final String platform;
    private final String wifiMac;

    private Device(Map<String, Object> data) {
        if(data == null) {
            throw new NullPointerException();
        } else {
            this.duid = (String)data.get("duid");
            this.model = (String)data.get("model");
            this.description = (String)data.get("description");
            this.networkType = (String)data.get("networkType");
            this.ssid = (String)data.get("ssid");
            this.ip = (String)data.get("ip");
            this.firmwareVersion = (String)data.get("firmwareVersion");
            this.name = (String)data.get("name");
            this.id = (String)data.get("id");
            this.udn = (String)data.get("udn");
            this.resolution = (String)data.get("resolution");
            this.countryCode = (String)data.get("countryCode");
            this.platform = (String)data.get("OS");
            this.wifiMac = (String)data.get("wifiMac");
        }
    }

    static Device create(Map<String, Object> data) {
        return new Device(data);
    }

    public String toString() {
        return "Device(duid=" + this.getDuid() + ", model=" + this.getModel() + ", description=" + this.getDescription() + ", networkType=" + this.getNetworkType() + ", ssid=" + this.getSsid() + ", ip=" + this.getIp() + ", firmwareVersion=" + this.getFirmwareVersion() + ", name=" + this.getName() + ", id=" + this.getId() + ", udn=" + this.getUdn() + ", resolution=" + this.getResolution() + ", countryCode=" + this.getCountryCode() + ", platform=" + this.getPlatform() + ", wifiMac=" + this.getWifiMac() + ")";
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof Device)) {
            return false;
        } else {
            Device other = (Device)o;
            if(!other.canEqual(this)) {
                return false;
            } else {
                String this$duid = this.getDuid();
                String other$duid = other.getDuid();
                if(this$duid == null) {
                    if(other$duid != null) {
                        return false;
                    }
                } else if(!this$duid.equals(other$duid)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Device;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        String $duid = this.getDuid();
        int result1 = result * 59 + ($duid == null?0:$duid.hashCode());
        return result1;
    }

    public String getDuid() {
        return this.duid;
    }

    public String getModel() {
        return this.model;
    }

    public String getDescription() {
        return this.description;
    }

    public String getNetworkType() {
        return this.networkType;
    }

    public String getSsid() {
        return this.ssid;
    }

    public String getIp() {
        return this.ip;
    }

    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public String getUdn() {
        return this.udn;
    }

    public String getResolution() {
        return this.resolution;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public String getPlatform() {
        return this.platform;
    }

    public String getWifiMac() {
        return this.wifiMac;
    }
}
