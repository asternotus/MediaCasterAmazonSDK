package com.connectsdk.service.capability;

import com.connectsdk.service.capability.listeners.ResponseListener;

public interface Installer extends CapabilityMethods {
    public final static String Any = "Installer.Any";

    public final static String Application_Install = "Installer.App.Install";

    public final static String[] Capabilities = {
            Application_Install
    };

    public Installer getInstaller();

    public void installApplication(String appId, ResponseListener<Boolean> listener);

    public void checkIfAppIsInstalled(String appId, ResponseListener<Boolean> listener);

    public CapabilityPriorityLevel getInstallerCapabilityLevel();

}
