//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests.impl;

import java.net.URI;

public class DeviceURIResult {
    private URI serviceURI;
    private URI applicationURI;

    public DeviceURIResult(URI serviceURI, URI applicationURI) {
        this.serviceURI = serviceURI;
        this.applicationURI = applicationURI;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[DeviceURIResult] serviceURI: ").append(this.serviceURI).append(" applicationURI: ").append(this.applicationURI);
        return builder.toString();
    }

    public URI getServiceURI() {
        return this.serviceURI;
    }

    public void setServiceURI(URI serviceURI) {
        this.serviceURI = serviceURI;
    }

    public URI getApplicationURI() {
        return this.applicationURI;
    }

    public void setApplicationURI(URI applicationURI) {
        this.applicationURI = applicationURI;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o != null && this.getClass() == o.getClass()) {
            DeviceURIResult that = (DeviceURIResult)o;
            if(this.applicationURI != null) {
                if(!this.applicationURI.equals(that.applicationURI)) {
                    return false;
                }
            } else if(that.applicationURI != null) {
                return false;
            }

            if(this.serviceURI != null) {
                if(this.serviceURI.equals(that.serviceURI)) {
                    return true;
                }
            } else if(that.serviceURI == null) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.serviceURI != null?this.serviceURI.hashCode():0;
        result = 31 * result + (this.applicationURI != null?this.applicationURI.hashCode():0);
        return result;
    }
}
