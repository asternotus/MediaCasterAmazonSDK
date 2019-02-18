//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.ssdp;

import java.net.URI;
import java.util.Scanner;

public class SSDPSearchResult {
    private String uniqueServiceName;
    private String uuid;
    private String searchTarget;
    private String location;
    private String deviceUri;
    private String host;
    private int port;
    private String server;

    public SSDPSearchResult() {
    }

    public String toString() {
        String s = "[SSDPSearchResult]";
        s = s + "\nuniqueServiceName: " + this.uniqueServiceName;
        s = s + "\nuuid: " + this.uuid;
        s = s + "\nsearchTarget: " + this.searchTarget;
        s = s + "\nlocation: " + this.location;
        s = s + "\ndeviceUri: " + this.deviceUri;
        s = s + "\nhost: " + this.host + ", port: " + this.port;
        s = s + "\nserver: " + this.server;
        return s;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o != null && this.getClass() == o.getClass()) {
            SSDPSearchResult that = (SSDPSearchResult)o;
            if(this.port != that.port) {
                return false;
            } else {
                label96: {
                    if(this.deviceUri != null) {
                        if(this.deviceUri.equals(that.deviceUri)) {
                            break label96;
                        }
                    } else if(that.deviceUri == null) {
                        break label96;
                    }

                    return false;
                }

                if(this.host != null) {
                    if(!this.host.equals(that.host)) {
                        return false;
                    }
                } else if(that.host != null) {
                    return false;
                }

                if(this.location != null) {
                    if(!this.location.equals(that.location)) {
                        return false;
                    }
                } else if(that.location != null) {
                    return false;
                }

                label75: {
                    if(this.searchTarget != null) {
                        if(this.searchTarget.equals(that.searchTarget)) {
                            break label75;
                        }
                    } else if(that.searchTarget == null) {
                        break label75;
                    }

                    return false;
                }

                if(this.server != null) {
                    if(!this.server.equals(that.server)) {
                        return false;
                    }
                } else if(that.server != null) {
                    return false;
                }

                if(this.uniqueServiceName != null) {
                    if(!this.uniqueServiceName.equals(that.uniqueServiceName)) {
                        return false;
                    }
                } else if(that.uniqueServiceName != null) {
                    return false;
                }

                if(this.uuid != null) {
                    if(!this.uuid.equals(that.uuid)) {
                        return false;
                    }
                } else if(that.uuid != null) {
                    return false;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.uniqueServiceName != null?this.uniqueServiceName.hashCode():0;
        result = 31 * result + (this.uuid != null?this.uuid.hashCode():0);
        result = 31 * result + (this.searchTarget != null?this.searchTarget.hashCode():0);
        result = 31 * result + (this.location != null?this.location.hashCode():0);
        result = 31 * result + (this.deviceUri != null?this.deviceUri.hashCode():0);
        result = 31 * result + (this.host != null?this.host.hashCode():0);
        result = 31 * result + this.port;
        result = 31 * result + (this.server != null?this.server.hashCode():0);
        return result;
    }

    public String getUniqueServiceName() {
        return this.uniqueServiceName;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getSearchTarget() {
        return this.searchTarget;
    }

    public String getLocation() {
        return this.location;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getDeviceUri() {
        return this.deviceUri;
    }

    public String getServer() {
        return this.server;
    }

    protected static SSDPSearchResult createResult(String data) {
        SSDPSearchResult result = null;

        try {
            result = new SSDPSearchResult();
            result.uniqueServiceName = getHeader(data, "USN");
            result.searchTarget = getHeader(data, "ST");
            result.location = getHeader(data, "LOCATION");
            String[] e = result.uniqueServiceName.split("::");
            String uuidPart = e[0];
            String[] uuidSplit = uuidPart.split("uuid:");
            result.uuid = uuidSplit[1];
            URI uri = URI.create(result.location);
            result.host = uri.getHost();
            result.port = uri.getPort();
            result.deviceUri = uri.getScheme() + "://" + result.host + ":" + result.port;
            result.server = getHeader(data, "SERVER");
        } catch (Exception var6) {
            ;
        }

        return result;
    }

    private static String getHeader(String data, String headerName) {
        Scanner s = new Scanner(data);
        s.nextLine();

        String line;
        int index;
        String header;
        do {
            if(!s.hasNextLine()) {
                return null;
            }

            line = s.nextLine();
            index = line.indexOf(58);
            if(index < 0) {
                return null;
            }

            header = line.substring(0, index);
        } while(!headerName.equalsIgnoreCase(header.trim()));

        return line.substring(index + 1).trim();
    }
}
