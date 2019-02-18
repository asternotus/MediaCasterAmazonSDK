package com.connectsdk.service.capability;

/**
 * Created by Dmitry on 16.02.17.
 */

public interface WakeControl extends CapabilityMethods{

    public final static String Wake_Device = "WakeControl.Wake";

    public final static String[] Capabilities = {
            Wake_Device
    };

    public boolean isDeviceOnline();

}
