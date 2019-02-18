package com.connectsdk.discovery.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.discovery.provider.samsung.SamsungSmartViewDeviceInfo;
import com.connectsdk.service.SamsungFastCastService;
import com.connectsdk.service.SamsungSmartViewService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.samsung.CastManager;
import com.connectsdk.service.samsung.OnDiscoveryListener;
import com.samsung.multiscreenfix.Device;
import com.samsung.multiscreenfix.Error;
import com.samsung.multiscreenfix.Result;
import com.samsung.multiscreenfix.Service;

import org.cybergarage.xml.Node;
import org.cybergarage.xml.ParserException;
import org.cybergarage.xml.parser.XmlPullParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungSmartViewDiscoveryProvider implements DiscoveryProvider {

    private static final String LOG_TAG = SamsungSmartViewDiscoveryProvider.class.getSimpleName();

    private static final String SAMSUNG_PROVIDER_PREF = "SAMSUNG_PROVIDER_PREF";

    private static final long RECONNECTION_DELAY = 60 * 1000;
    private static final long STANDBY_DISCOVERY_DELAY = 5 * 1000;
    private static final String UNKNOWN_PRODUCT_CAP = "unknown";

    private final Context context;

    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners
            = new CopyOnWriteArrayList<>();

    private ConcurrentHashMap<String, ServiceDescription> foundServices
            = new ConcurrentHashMap<>();

    // map uuid -> product capability
    private ConcurrentHashMap<String, String> productCapMap
            = new ConcurrentHashMap<>();

    // map uuid -> legacy device
    private ConcurrentHashMap<String, com.samsung.multiscreen.device.Device> legacySmartViewDevices
            = new ConcurrentHashMap<>();

    // map uuid -> product capability
    private ConcurrentHashMap<String, Service> services
            = new ConcurrentHashMap<>();

    private Timer timer = new Timer();

    private final CastManager mCastManager;
    private SSDPDiscoveryProvider ssdpDiscoveryProvider;
    private SamsungLegacySmartViewDiscoveryProvider legacyDiscoveryProvider;

    private TimerTask standbyDevicesTask;

    private boolean wakeOnLanEnabled = false;

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

    private String[] modelYearsTizen = {
            "Q" /*= 2017 QLED*/,
            "M" /*= 2017 HD*/,
            "K" /*= 2016*/,
            "L" /*= 2015*/,
            "J" /*= 2015*/,
    };

    public SamsungSmartViewDiscoveryProvider(Context context) {
        this.context = context;
        CastManager.init(context);
        mCastManager = CastManager.getInstance();

        legacyDiscoveryProvider = new SamsungLegacySmartViewDiscoveryProvider(context);

        ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);

        DiscoveryFilter filter = new DiscoveryFilter("SamsungTizenDial", "urn:dial-multiscreen-org:service:dial:1");
        List<DiscoveryFilter> filters = new ArrayList<>();
        filters.add(filter);

        ssdpDiscoveryProvider.setFilters(filters);
    }

    //  <editor-fold desc="listeners">

    private OnDiscoveryListener smartViewDiscoveryListener = new OnDiscoveryListener() {

        TimerTask disconnectTask;
        Service reconnectService;

        @Override
        public void onServiceLost(Service service) {
            SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] onServiceLost " + service.getId() + "  " + service.getName());

            reconnectService = service;

            disconnectTask = new TimerTask() {
                @Override
                public void run() {
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] disconnect from service ");
                    ServiceDescription serviceDescription = foundServices.get(reconnectService.getId());
                    if (serviceDescription != null) {
                        notifyListenersThatServiceLost(serviceDescription);
                        foundServices.remove(reconnectService.getId());
                    }
                }
            };
            new Timer().schedule(disconnectTask, RECONNECTION_DELAY);
        }

        @Override
        public void onServiceFound(final Service service) {
            SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] onServiceFound " + service.getId() + "  " + service.getName());

            if (reconnectService != null && service.equals(reconnectService) && disconnectTask != null) {
                SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] keep connection to service ");
                disconnectTask.cancel();
            }

            service.getDeviceInfo(new Result<Device>() {
                @Override
                public void onSuccess(Device device) {
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Obtained smart view service for " + service.getId());

                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device Name  " + service.getName());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device type  " + service.getType());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device platform  " + device.getPlatform());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device firmware  " + device.getFirmwareVersion());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device model  " + device.getModel());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device uuid  " + device.getDuid());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device id  " + device.getId());
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Device description  " + device.getDescription());

                    onServiceObtained(device, service);
                }

                @Override
                public void onError(Error error) {
                    SmartLog.t(LOG_TAG, "[smartViewDiscoveryListener] Could not obtain device info for device " + service.getName());
                }
            });
        }
    };

    private DiscoveryProviderListener ssdpDiscoveryListener = new DiscoveryProviderListener() {
        @Override
        public void onServiceAdded(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            String uuid = serviceDescription.getUUID();
            if (!uuid.startsWith("uuid:")) {
                uuid = "uuid:" + serviceDescription.getUUID();
            }

            SmartLog.t(LOG_TAG, "[ssdpDiscoveryListener] Obtained ssdp device for " + uuid);
            SmartLog.t(LOG_TAG, "[ssdpDiscoveryListener] Obtained ssdp device  " + serviceDescription.getIpAddress());

            String modelName = serviceDescription.getModelName();

            if (!TextUtils.isEmpty(modelName) && isLateOrsayDevice(modelName)) {
                return;
            }

            XmlPullParser parser = new XmlPullParser();
            try {
                String locationXML = serviceDescription.getLocationXML();
                Node node = parser.parse(locationXML);
                Node device = node.getNode("device");

                if (device != null) {
                    SmartLog.d(LOG_TAG, "[ssdpDiscoveryListener] device is not null");
                    Node productCap = device.getNode("sec:ProductCap");
                    if (productCap != null) {
                        SmartLog.t(LOG_TAG, String.format("product capabilities for %s: %s", uuid, productCap));
                        onProductCapabilityObtained(uuid, productCap.getValue());
                    } else {
                        SmartLog.d(LOG_TAG, "model name " + modelName);
                        String year = null;
                        if (!TextUtils.isEmpty(modelName)) {
                            year = getModelYear(modelName);
                        }

                        SmartLog.d(LOG_TAG, "model year: " + year);

                        if (year != null) {
                            for (int i = 0; i < modelYearsTizen.length; i++) {
                                if (modelYearsTizen[i].equals(year)) {
                                    onProductCapabilityObtained(uuid, "Tizen");
                                }
                            }
                        }
                    }
                }
            } catch (ParserException e) {
                SmartLog.t(LOG_TAG, "[ssdpDiscoveryListener] Could not parse location xml " + e.getMessage());
                e.printStackTrace();
            }
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

    private DiscoveryProviderListener legacySmartViewDiscoveryListener = new DiscoveryProviderListener() {
        @Override
        public void onServiceAdded(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            String uuid = serviceDescription.getUUID();
            if (!uuid.startsWith("uuid:")) {
                uuid = "uuid:" + serviceDescription.getUUID();
            }

            SmartLog.t(LOG_TAG, "[legacySmartViewDiscoveryListener] Obtained legacy device for " + uuid);
            SmartLog.t(LOG_TAG, "[legacySmartViewDiscoveryListener] Obtained legacy device for " + serviceDescription.getIpAddress());

            com.samsung.multiscreen.device.Device device = (com.samsung.multiscreen.device.Device) serviceDescription.getDevice();

            onLegacyDeviceObtained(uuid, device);
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
            SmartLog.t(LOG_TAG, "legacy smart view discovery failed " + error.getMessage());
            notifyListenersThatDiscoveryFailed(error);
        }
    };

    //  </editor-fold>

    //  <editor-fold desc="discovery">

    @Override
    public void start() {
        SmartLog.t(LOG_TAG, "start");
        startInternal();
    }

    protected void startInternal() {
        ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);
        DiscoveryFilter filter = new DiscoveryFilter("SamsungTizenDial", "urn:dial-multiscreen-org:service:dial:1");
        List<DiscoveryFilter> filters = new ArrayList<>();
        filters.add(filter);
        ssdpDiscoveryProvider.setFilters(filters);

        mCastManager.addOnDiscoveryListener(smartViewDiscoveryListener);
        legacyDiscoveryProvider.addListener(legacySmartViewDiscoveryListener);
        ssdpDiscoveryProvider.addListener(ssdpDiscoveryListener);

        mCastManager.startDiscovery();

        ssdpDiscoveryProvider.start();

        legacyDiscoveryProvider.start();

        if (wakeOnLanEnabled) {
            issueStandbyDiscoveryTask();
        }
    }

    @Override
    public void stop() {
        SmartLog.t(LOG_TAG, "stop");

        stopInternal();

        for (ServiceDescription serviceDescription : foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        foundServices.clear();

        if (wakeOnLanEnabled) {
            cancelStandbyDiscoveryTask();
        }
    }

    protected void stopInternal() {
        mCastManager.removeOnDiscoveryListener(smartViewDiscoveryListener);
        legacyDiscoveryProvider.removeListener(legacySmartViewDiscoveryListener);
        ssdpDiscoveryProvider.removeListener(ssdpDiscoveryListener);

        mCastManager.stopDiscovery();
        mCastManager.reset();

        ssdpDiscoveryProvider.stop();

        legacyDiscoveryProvider.stop();
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

    //  </editor-fold>

    //  <editor-fold desc="private">

    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        SmartLog.t(LOG_TAG, "notifyListenersThatServiceAdded ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceAdded(SamsungSmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        SmartLog.t(LOG_TAG, "notifyListenersThatServiceLost ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceRemoved(SamsungSmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatServiceUpdated(final ServiceDescription serviceDescription) {
        SmartLog.t(LOG_TAG, "notifyListenersThatServiceUpdated ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceUpdated(SamsungSmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError error) {
        SmartLog.t(LOG_TAG, "notifyListenersThatDiscoveryFailed " + error.getMessage());
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceDiscoveryFailed(SamsungSmartViewDiscoveryProvider.this, error);
        }
    }

    private void onLegacyDeviceObtained(String uuid, com.samsung.multiscreen.device.Device device) {
        legacySmartViewDevices.put(uuid, device);

        updateFoundServices();
    }

    private void onProductCapabilityObtained(String uuid, String productCap) {
        if (productCap != null) {
            productCapMap.put(uuid, productCap);

            updateFoundServices();
        }
    }

    private void onServiceObtained(Device device, Service service) {
        String uuid = service.getId();

        services.put(uuid, service);

        SamsungTvInfo tvInfo = new SamsungTvInfo(service, device.getWifiMac());
        saveSamsungTvInfo(tvInfo);

        if (device.getPlatform() != null && device.getPlatform().equals("Tizen")) {
            SmartLog.d(LOG_TAG, "got Tizen device on " + uuid);
            onProductCapabilityObtained(uuid, "Tizen");
        }

        updateFoundServices();
    }

    private void updateFoundServices() {
        SmartLog.t(LOG_TAG, "updateFoundServices");

        for (String uuid : services.keySet()) {
            SmartLog.t(LOG_TAG, "[updateFoundServices] service: " + uuid);
            Service service = services.get(uuid);

            String productCapabilities = null;

            if (productCapMap.containsKey(uuid)) {
                productCapabilities = productCapMap.get(uuid);
            }

            SmartLog.t(LOG_TAG, "[updateFoundServices] product capabilities " + productCapabilities);

            boolean tizen = productCapabilities != null && productCapabilities.contains("Tizen");

            com.samsung.multiscreen.device.Device legacyDevice = legacySmartViewDevices.get("uuid:" + uuid);

            boolean legacy = legacyDevice != null;

            SmartLog.t(LOG_TAG, "[updateFoundServices] legacy: " + legacy);

            if (tizen || legacy) {
                SmartLog.t(LOG_TAG, "[updateFoundServices] FOUND SMART VIEW DEVICE, is tizen: " + tizen + ", is legacy: " + legacy);
                ServiceDescription serviceDescription = foundServices.get(uuid);
                if (serviceDescription == null) {
                    serviceDescription = createServiceDescription(service, tizen, legacyDevice);
                    foundServices.put(uuid, serviceDescription);
                    notifyListenersThatServiceAdded(serviceDescription);
                }
            }
        }

        if (services.isEmpty() && !legacySmartViewDevices.isEmpty()) {
            SmartLog.d(LOG_TAG, "Legacy devices are out of synch!");
            mCastManager.removeOnDiscoveryListener(smartViewDiscoveryListener);
            mCastManager.stopDiscovery();
            mCastManager.reset();

            mCastManager.addOnDiscoveryListener(smartViewDiscoveryListener);
            mCastManager.startDiscovery();
        }
    }

    private ServiceDescription createServiceDescription(Service service, boolean tizen, com.samsung.multiscreen.device.Device legacyDevice) {
        ServiceDescription serviceDescription = new ServiceDescription();

        SamsungSmartViewDeviceInfo info = new SamsungSmartViewDeviceInfo(service, legacyDevice, tizen);

        String uri = service.getUri().toString();

        String uid = service.getId();
        serviceDescription.setDevice(info);
        serviceDescription.setFriendlyName(service.getName());
        serviceDescription.setIpAddress(uri);
        serviceDescription.setServiceID(SamsungSmartViewService.ID);
        serviceDescription.setUUID(uid);

        return serviceDescription;
    }

    private ServiceDescription createServiceDescription(com.samsung.multiscreen.device.Device legacyDevice) {
        ServiceDescription serviceDescription = new ServiceDescription();

        Service service = new Service(legacyDevice.getId(), legacyDevice.getName(), legacyDevice.getName(), legacyDevice.getNetworkType(),
                null, null, false);

        SamsungSmartViewDeviceInfo info = new SamsungSmartViewDeviceInfo(service, legacyDevice, false);

        String uri = legacyDevice.getServiceURI().toString();

        String uid = service.getId();
        serviceDescription.setDevice(info);
        serviceDescription.setFriendlyName(service.getName());
        serviceDescription.setIpAddress(uri);
        serviceDescription.setServiceID(SamsungSmartViewService.ID);
        serviceDescription.setUUID(uid);

        return serviceDescription;
    }

    private void issueStandbyDiscoveryTask() {
        cancelStandbyDiscoveryTask();
        standbyDevicesTask = new TimerTask() {
            @Override
            public void run() {
                loadStandbyDevices();
            }
        };

        timer.schedule(standbyDevicesTask, STANDBY_DISCOVERY_DELAY);
    }

    private void cancelStandbyDiscoveryTask() {
        if (standbyDevicesTask != null) {
            standbyDevicesTask.cancel();
        }
    }

    private void loadStandbyDevices() {
        SmartLog.t(LOG_TAG, "loadStandbyDevices ");

        SharedPreferences preferences = context.getSharedPreferences(SAMSUNG_PROVIDER_PREF, 0);
        Map<String, ?> prefMap = preferences.getAll();
        for (String key : prefMap.keySet()) {
            SamsungTvInfo tvInfo = getSamsungTvInfo(key);
            if (tvInfo != null) {
                final String uuid = tvInfo.getUuid();
                ServiceDescription serviceDescription = foundServices.get(uuid);

                if (serviceDescription == null) {
                    serviceDescription = new ServiceDescription();
                    serviceDescription.setDevice(null);
                    serviceDescription.setFriendlyName(tvInfo.getFriendlyName());
                    serviceDescription.setIpAddress(tvInfo.getIpAddress());
                    serviceDescription.setServiceID(SamsungFastCastService.ID);
                    serviceDescription.setUUID(uuid);
                    serviceDescription.setWifiMac(tvInfo.getWifiMac());


                    foundServices.put(uuid, serviceDescription);
                    notifyListenersThatServiceAdded(serviceDescription);
                }
            }
        }
    }

    private void saveSamsungTvInfo(SamsungTvInfo tvInfo) {
        SmartLog.t(LOG_TAG, "saveSamsungTvInfo " + tvInfo.getUuid());
        SharedPreferences.Editor editor = context.getSharedPreferences(SAMSUNG_PROVIDER_PREF, 0).edit();
        editor.putString(tvInfo.getWifiMac(), tvInfo.toJson().toString());
        editor.commit();
    }

    private SamsungTvInfo getSamsungTvInfo(String wifiMac) {
        SmartLog.t(LOG_TAG, "getSamsungTvInfo " + wifiMac);
        SharedPreferences preferences = context.getSharedPreferences(SAMSUNG_PROVIDER_PREF, 0);
        String storedInfoStr = preferences.getString(wifiMac, null);
        if (!TextUtils.isEmpty(storedInfoStr)) {
            try {
                SamsungTvInfo samsungTvInfo = new SamsungTvInfo(new JSONObject(storedInfoStr));
                SmartLog.t(LOG_TAG, "[getSamsungTvInfo] tv info detected for " + wifiMac);
                return samsungTvInfo;
            } catch (JSONException e) {
                SmartLog.t(LOG_TAG, "[getSamsungTvInfo] Could not parse stored samsung tv info for mac address " + wifiMac + "! " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    //  </editor-fold>

    class SamsungTvInfo {

        private static final String WIFI_MAC = "WIFI_MAC";
        private static final String FRIENDLY_NAME = "FRIENDLY_NAME";
        private static final String IP_ADDRESS = "IP_ADDRESS";
        private static final String UUID = "UUID";

        private final String friendlyName;
        private final String ipAddress;
        private final String uuid;
        private final String wifiMac;

        public SamsungTvInfo(JSONObject object) throws JSONException {
            this.friendlyName = object.getString(FRIENDLY_NAME);
            this.ipAddress = object.getString(IP_ADDRESS);
            this.uuid = object.getString(UUID);
            this.wifiMac = object.getString(WIFI_MAC);
        }

        public SamsungTvInfo(final Service service, String wifiMac) {
            this.friendlyName = service.getName();
            this.ipAddress = service.getUri().toString();
            this.uuid = service.getId();
            this.wifiMac = wifiMac;
        }

        public JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put(FRIENDLY_NAME, friendlyName);
                object.put(IP_ADDRESS, ipAddress);
                object.put(UUID, uuid);
                object.put(WIFI_MAC, wifiMac);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getUuid() {
            return uuid;
        }

        public String getWifiMac() {
            return wifiMac;
        }
    }

    private boolean isLateOrsayDevice(String modelName) {
        return modelName.contains("J4300") || modelName.contains("J5300");
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

}
