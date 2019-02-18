package com.connectsdk.discovery.provider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.discovery.provider.samsung.convergence.ConvergenceService;
import com.connectsdk.service.SamsungConvergenceService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.convergence.NSConnection;
import com.connectsdk.service.convergence.NSDevice;
import com.connectsdk.service.convergence.NSDiscovery;
import com.connectsdk.service.convergence.NSListener;
import com.mega.cast.utils.log.SmartLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungConvergenceDiscoveryProvider implements DiscoveryProvider {

    private static final String LOG_TAG = SamsungConvergenceDiscoveryProvider.class.getSimpleName();

    private final Context context;
    private SSDPDiscoveryProvider ssdpDiscoveryProvider;

    private String[] modelScreenTypes = {"Q", "U", "P", "L", "H", "K"};
    private String[] modelRegionTypes = {"N", "E", "A"};
    private String[] modelYears = {
            "Q" /*= 2017 QLED*/,
            "M" /*= 2017 HD*/,
            "K" /*= 2016*/,
            "L" /*= 2015*/,
            "J" /*= 2015*/,
            "H" /*= 2014*/,
            "F" /*= 2013*/,
            "E" /*= 2012*/,
            "D" /*= 2011*/,
            "C" /*= 2010*/,
            "B" /*= 2009*/,
            "A" /*= 2008*/
    };

    private String[] modelYearsConvergence = {
            "F" /*= 2013*/,
            "E" /*= 2012*/,
            "D" /*= 2011*/,
            "C" /*= 2010*/,
    };

    Map<String, String> deviceYearsMap = new HashMap<>();
    Map<String, ServiceDescription> serviceMap = new HashMap<>();

    private NSConnection nsConnection;
    private NSDiscovery nsDiscovery;

    private ConcurrentHashMap<String, ServiceDescription> foundServices
            = new ConcurrentHashMap<>();

    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners
            = new CopyOnWriteArrayList<>();

    private String remoteAppId;

    public SamsungConvergenceDiscoveryProvider(Context context) {
        this.context = context;

        ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);

        DiscoveryFilter filter = new DiscoveryFilter("SamsungTizenDial", "urn:dial-multiscreen-org:service:dial:1");
        List<DiscoveryFilter> filters = new ArrayList<>();
        filters.add(filter);

        ssdpDiscoveryProvider.setFilters(filters);
    }

    //  <editor-fold desc="listeners">

    private DiscoveryProviderListener ssdpDiscoveryListener = new DiscoveryProviderListener() {
        @Override
        public void onServiceAdded(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            String uuid = serviceDescription.getUUID();
            if (!uuid.startsWith("uuid:")) {
                uuid = "uuid:" + serviceDescription.getUUID();
            }

            String modelName = serviceDescription.getModelName();

            String uid = getDeviceID(serviceDescription.getIpAddress(),
                    serviceDescription.getFriendlyName());

            String year = null;
            if (!TextUtils.isEmpty(modelName)) {
                year = getModelYear(modelName);
            }

            SmartLog.d(LOG_TAG, "model year: " + year);
            SmartLog.d(LOG_TAG, "id:  " + uid);

            onModelYearObtained(uid, year);
        }


        @Override
        public void onServiceRemoved(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            //do nothing
        }

        @Override
        public void onServiceUpdated(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            //do nothing
        }

        @Override
        public void onServiceDiscoveryFailed(DiscoveryProvider provider, ServiceCommandError error) {
            SmartLog.t(LOG_TAG, "[ssdpDiscoveryListener] SSDP discovery failed " + error.getMessage());
            notifyListenersThatDiscoveryFailed(error);
        }
    };

    private NSListener discoveryListener = new NSListener("DiscoveryListener") {
        @Override
        public void onWifiChanged() {
            SmartLog.t(LOG_TAG, "[discoveryListener] onWifiChanged ");
        }

        @Override
        public void onDeviceChanged(List<NSDevice> devices) {
            SmartLog.t(LOG_TAG, "[discoveryListener] onDeviceChanged " + devices.size());
            for (NSDevice device : devices) {
                String uid = getDeviceID(device);
                SmartLog.t(LOG_TAG, "[discoveryListener] Device ID  " + uid);
                SmartLog.t(LOG_TAG, "[discoveryListener] Device Name  " + device.getName());

                ServiceDescription serviceDescription = foundServices.get(uid);

                if (serviceDescription == null) {
                    SmartLog.t(LOG_TAG, "[discoveryListener] create service description ");
                    serviceDescription = new ServiceDescription();
                    updateServiceDescription(serviceDescription, device, nsConnection);
                    onServiceObtained(uid, serviceDescription);
                } else {
                    SmartLog.t(LOG_TAG, "[discoveryListener] update service description ");
                    ServiceDescription updateServiceDescription = new ServiceDescription();
                    updateServiceDescription(updateServiceDescription, device, nsConnection);

                    if (!updateServiceDescription.equals(serviceDescription)) {
                        notifyListenersThatServiceUpdated(serviceDescription);
                    } else {
                        SmartLog.t(LOG_TAG, "[discoveryListener] service description has not changed, skip updating ");
                    }
                }
            }
        }

        @Override
        public void onConnected(NSDevice device) {
            SmartLog.t(LOG_TAG, "[discoveryListener] onConnected ");
        }

        @Override
        public void onDisconnected() {
            SmartLog.t(LOG_TAG, "[discoveryListener] onDisconnected ");
        }

        @Override
        public void onConnectionFailed(int code) {
            SmartLog.t(LOG_TAG, "[discoveryListener] onConnectionFailed " + code);
        }

        @Override
        public void onMessageSent() {
            SmartLog.t(LOG_TAG, "onMessageSent ");
        }

        @Override
        public void onMessageSendFailed(int code) {
            SmartLog.t(LOG_TAG, "onMessageSendFailed ");
        }

        @Override
        public void onMessageReceived(String message) {
            SmartLog.t(LOG_TAG, "onMessageReceived " + message);
        }
    };

    private void onModelYearObtained(String uid, String year) {
        deviceYearsMap.put(uid, year);

        ServiceDescription foundDesc = foundServices.get(uid);

        ServiceDescription serviceDescription = serviceMap.get(uid);

        boolean convergenceService = false;

        for (String convYear : modelYearsConvergence) {
            if (convYear.equals(year)) {
                convergenceService = true;
                break;
            }
        }

        if (foundDesc == null && serviceDescription != null && convergenceService) {
            foundServices.put(uid, serviceDescription);
            notifyListenersThatServiceAdded(serviceDescription);
        }
    }

    private void onServiceObtained(String uid, ServiceDescription serviceDescription) {
        serviceMap.put(uid, serviceDescription);

        ServiceDescription foundDesc = foundServices.get(uid);
        String year = deviceYearsMap.get(uid);

        boolean convergenceService = false;

        for (String convYear : modelYearsConvergence) {
            if (convYear.equals(year)) {
                convergenceService = true;
                break;
            }
        }

        if (foundDesc == null && convergenceService) {
            foundServices.put(uid, serviceDescription);
            notifyListenersThatServiceAdded(serviceDescription);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="discovery">

    @Override
    public void start() {
        SmartLog.t(LOG_TAG, "start " + remoteAppId + " discovery");

        if (nsConnection == null) {
            nsConnection = new NSConnection(remoteAppId, context);
            nsDiscovery = new NSDiscovery(nsConnection);
        }

        nsConnection.registerListener(discoveryListener);

        ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);

        DiscoveryFilter filter = new DiscoveryFilter("SamsungTizenDial", "urn:dial-multiscreen-org:service:dial:1");
        List<DiscoveryFilter> filters = new ArrayList<>();
        filters.add(filter);

        ssdpDiscoveryProvider.setFilters(filters);

        ssdpDiscoveryProvider.addListener(ssdpDiscoveryListener);
        ssdpDiscoveryProvider.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                nsDiscovery.search();
            }
        }).start();

    }

    @Override
    public void stop() {
        SmartLog.t(LOG_TAG, "stop ");

        if (nsDiscovery != null) {
            nsDiscovery.dispose();
        }

        ssdpDiscoveryProvider.removeListener(ssdpDiscoveryListener);
        ssdpDiscoveryProvider.stop();

        for (ServiceDescription serviceDescription : foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        foundServices.clear();
    }

    @Override
    public void restart() {
        SmartLog.t(LOG_TAG, "restart ");

        stop();
        start();
    }

    @Override
    public void rescan() {
        restart();
    }

    @Override
    public void reset() {
        SmartLog.t(LOG_TAG, "reset ");

        foundServices.clear();
        stop();
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        SmartLog.t(LOG_TAG, "addListener ");
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
        SmartLog.t(LOG_TAG, "removeListener ");
        serviceListeners.remove(listener);
    }

    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    @Override
    public void setFilters(List<DiscoveryFilter> filters) {
        // intentionally left blank
    }

    @Override
    public boolean isEmpty() {
        return foundServices.isEmpty();
    }

    public void setPlayerId(String remoteAppId) {
        this.remoteAppId = remoteAppId;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        SmartLog.t(LOG_TAG, "notifyListenersThatServiceAdded ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceAdded(SamsungConvergenceDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceRemoved(SamsungConvergenceDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError error) {
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceDiscoveryFailed(SamsungConvergenceDiscoveryProvider.this, error);
        }
    }

    private void notifyListenersThatServiceUpdated(final ServiceDescription serviceDescription) {
        SmartLog.t(LOG_TAG, "notifyListenersThatServiceUpdated ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceUpdated(SamsungConvergenceDiscoveryProvider.this, serviceDescription);
        }
    }

    private void updateServiceDescription(ServiceDescription serviceDescription, NSDevice device, NSConnection connection) {
        String uid = getDeviceID(device);
        ConvergenceService service = new ConvergenceService(device, connection);

        serviceDescription.setDevice(service);
        serviceDescription.setFriendlyName(device.getName());
        serviceDescription.setIpAddress(device.getIP());
        serviceDescription.setServiceID(SamsungConvergenceService.ID);
        serviceDescription.setUUID(uid);
    }

    @NonNull
    private String getDeviceID(NSDevice device) {
        return device.getIP() + device.getName();
    }

    @NonNull
    private String getDeviceID(String ip, String name) {
        return ip + name;
    }

    private String getModelYear(String modelName) {
        String deviceYear = null;

        String deviceScreenType = null;
        for (String modelScreenType : modelScreenTypes) {
            if (modelName.startsWith(modelScreenType)) {
                deviceScreenType = modelScreenType;
            }
        }

        if (deviceScreenType != null) {
            String deviceRegionType = null;
            for (String modelRegionType : modelRegionTypes) {
                if (modelName.startsWith(deviceScreenType + modelRegionType)) {
                    deviceRegionType = modelRegionType;
                }
            }

            if (deviceRegionType != null) {

                modelName = modelName.substring(deviceScreenType.length()
                        + deviceRegionType.length()
                        + 2);

                for (String modelYear : modelYears) {
                    if (modelName.startsWith(modelYear)) {
                        deviceYear = modelYear;
                    }
                }
            }
        }

        return deviceYear;
    }

    //  </editor-fold>

}
