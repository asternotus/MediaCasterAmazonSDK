package com.connectsdk.discovery.provider;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.SamsungLegacySmartViewService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.requests.FindLocalDialDevicesRequest;
import com.samsung.multiscreen.device.requests.GetDeviceRequest;
import com.samsung.multiscreen.impl.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Dmitry on 27.04.17.
 */

public class SamsungLegacySmartViewDiscoveryProvider implements DiscoveryProvider {

    private static final String LOG_TAG = SamsungLegacySmartViewDiscoveryProvider.class.getSimpleName();
    private static final int DISCOVERY_TIMEOUT = 20 * 1000;
    private static final String SAMSUNG_SMART_VIEW_SSDP_FILTER = "samsung:multiscreen:1";
    private static final String SAMSUNG_SMART_VIEW_DIAL_URN = "urn:dial-multiscreen-org:service:dial:1";

    private final Context context;

    private ConcurrentHashMap<String, ServiceDescription> foundServices
            = new ConcurrentHashMap<>();

    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners
            = new CopyOnWriteArrayList<>();

    private Timer searchTimer;

    private TimerTask searchTask;

    private SSDPDiscoveryProvider ssdpDiscoveryProvider;

    private DiscoveryProviderListener ssdpListener = new DiscoveryProviderListener() {
        @Override
        public void onServiceAdded(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            SmartLog.d(LOG_TAG, String.format("service added\n" +
                            "name: %s\n" +
                            "ip address: %s\n" +
                            "app's url: %s\n" +
                            "service uri: %s\n" +
                            "service filter: %s\n" +
                            "service id: %s", serviceDescription.getFriendlyName(),
                    serviceDescription.getIpAddress(), serviceDescription.getApplicationURL(),
                    serviceDescription.getServiceURI(), serviceDescription.getServiceFilter(),
                    serviceDescription.getServiceID()));

            if (serviceDescription.getServiceURI() != null) {
                URI serviceUri = URI.create(serviceDescription.getServiceURI());
                final GetDeviceRequest deviceRequest = new GetDeviceRequest(serviceUri, null);

                DeviceAsyncResult<Device> callback = new DeviceAsyncResult<Device>() {
                    private double sleep_sec = 1;
                    private double retry_sec = 10 * 60;
                    private int retry = (int) (retry_sec / sleep_sec);

                    @Override
                    public void onResult(Device device) {
                        SmartLog.i(LOG_TAG, "Got device " + device.getName());
                        onDeviceFound(device);
                    }

                    @Override
                    public void onError(DeviceError error) {
//                        SmartLog.e(LOG_TAG, "Could not get device " + error.getMessage());
                        try {
                            Thread.sleep((long) (1000 * sleep_sec));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        retry -= 1;

                        if (retry > 0) {
                            Service.getInstance().getExecutorService().execute(deviceRequest);
                        }
                    }
                };
                deviceRequest.setCallback(callback);
                Service.getInstance().getExecutorService().execute(deviceRequest);
            }
        }

        @Override
        public void onServiceRemoved(DiscoveryProvider provider, ServiceDescription serviceDescription) {
            SmartLog.d(LOG_TAG, String.format("service removed\n" +
                            "ip address: %s\n" +
                            "app's url: %s\n" +
                            "service uri: %s\n" +
                            "service filter: %s\n" +
                            "service id: %s",
                    serviceDescription.getIpAddress(), serviceDescription.getApplicationURL(),
                    serviceDescription.getServiceURI(), serviceDescription.getServiceFilter(),
                    serviceDescription.getServiceID()));

            List<String> killList = new ArrayList<String>();

            for (String key : foundServices.keySet()) {
                ServiceDescription service = foundServices.get(key);
                SmartLog.d(LOG_TAG, "previously found service " + service.getIpAddress());
                if (service.getIpAddress().equals(serviceDescription.getIpAddress())) {
                    SmartLog.d(LOG_TAG, "Service is not available anymore! ");
                    killList.add(key);
                }
            }

            for (String key : killList) {
                ServiceDescription service = foundServices.get(key);
                notifyListenersThatServiceLost(service);

                foundServices.remove(key);
            }
        }

        @Override
        public void onServiceUpdated(DiscoveryProvider provider, ServiceDescription serviceDescription) {

        }

        @Override
        public void onServiceDiscoveryFailed(DiscoveryProvider provider, ServiceCommandError error) {
            SmartLog.e(LOG_TAG, "onServiceDiscoveryFailed " + error.getMessage());
        }
    };


    public SamsungLegacySmartViewDiscoveryProvider(Context context) {
        this.context = context;
        this.ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);

        List<DiscoveryFilter> filters = new ArrayList<>();
        //test filter
        filters.add(new DiscoveryFilter(SamsungLegacySmartViewService.ID,
                SAMSUNG_SMART_VIEW_DIAL_URN/*SAMSUNG_SMART_VIEW_SSDP_FILTER*/));
        ssdpDiscoveryProvider.setFilters(filters);
    }

    @Override
    public void start() {
        SmartLog.d(LOG_TAG, "start ");
        if (searchTimer != null) {
            searchTimer.cancel();
        }

        this.ssdpDiscoveryProvider = new SSDPDiscoveryProvider(context);

        List<DiscoveryFilter> filters = new ArrayList<>();
        //test filter
        filters.add(new DiscoveryFilter(SamsungLegacySmartViewService.ID,
                SAMSUNG_SMART_VIEW_DIAL_URN/*SAMSUNG_SMART_VIEW_SSDP_FILTER*/));
        ssdpDiscoveryProvider.setFilters(filters);

        ssdpDiscoveryProvider.addListener(ssdpListener);
        ssdpDiscoveryProvider.start();

       /* searchTimer = new Timer();
        searchTask = new TimerTask() {
            @Override
            public void run() {
                updateDevices();
            }
        };

        searchTimer.schedule(searchTask, 0, DISCOVERY_TIMEOUT);*/
    }

    @Override
    public void stop() {
        SmartLog.d(LOG_TAG, "stop");

        for (ServiceDescription serviceDescription : foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        foundServices.clear();

        ssdpDiscoveryProvider.removeListener(ssdpListener);
        ssdpDiscoveryProvider.stop();

        if (searchTimer != null) {
            searchTask.cancel();
            searchTimer.cancel();

            searchTimer = null;
        }
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public void rescan() {
        restart();
    }

    @Override
    public void reset() {
        foundServices.clear();
        stop();
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
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

    //  <editor-fold desc="private">

    private void updateDevices() {

        final int id = new Random().nextInt(1000);

        SmartLog.d(LOG_TAG, "updateDevices, id: " + id);

    /*    Device.search(new DeviceAsyncResult<List<Device>>() {
            @Override
            public void onResult(List<Device> devices) {
                SmartLog.d(LOG_TAG, "update devices on result, id: " + id);
            }

            @Override
            public void onError(DeviceError deviceError) {
                SmartLog.e(LOG_TAG, "update devices on error, id: " + id + " " + deviceError.getMessage());
            }
        });
*/

        FindLocalDialDevicesRequest findLocalDialDevicesRequest
                = new FindLocalDialDevicesRequest(DISCOVERY_TIMEOUT, SAMSUNG_SMART_VIEW_SSDP_FILTER,
                new DeviceAsyncResult<List<Device>>() {
                    @Override
                    public void onResult(List<Device> devices) {
                        SmartLog.d(LOG_TAG, "update devices on result, id: " + id);
                        for (Device device : devices) {
                            onDeviceFound(device);
                        }
                    }

                    @Override
                    public void onError(DeviceError error) {
                        SmartLog.e(LOG_TAG, "update devices on error, id: " + id + " " + error.getMessage());
                        SmartLog.e(LOG_TAG, "Discovery error " + error.getMessage());
                        // ignore discovery error
                    }
                });
        Service.getInstance().getExecutorService().execute(findLocalDialDevicesRequest);
    }

    private void onDeviceFound(Device device) {
        ServiceDescription serviceDescription = foundServices.get(device.getId());

        if (serviceDescription == null) {
            SmartLog.d(LOG_TAG, "create service description ");
            serviceDescription = new ServiceDescription();
            updateServiceDescription(serviceDescription, device);
            foundServices.put(device.getId(), serviceDescription);
            notifyListenersThatServiceAdded(serviceDescription);
        } else {
            if (!serviceDescription.getIpAddress().equals(device.getIPAddress())) {
                SmartLog.d(LOG_TAG, "device ip changed, updating the description...");
                updateServiceDescription(serviceDescription, device);
                notifyListenersThatServiceUpdated(serviceDescription);
            }
        }
    }


    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "notifyListenersThatServiceAdded " + serviceDescription.getFriendlyName());
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceAdded(SamsungLegacySmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "notifyListenersThatServiceLost " + serviceDescription.getFriendlyName());
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceRemoved(SamsungLegacySmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError error) {
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceDiscoveryFailed(SamsungLegacySmartViewDiscoveryProvider.this, error);
        }
    }

    private void notifyListenersThatServiceUpdated(final ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "notifyListenersThatServiceUpdated " + serviceDescription.getFriendlyName());
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceUpdated(SamsungLegacySmartViewDiscoveryProvider.this, serviceDescription);
        }
    }

    private void updateServiceDescription(ServiceDescription serviceDescription, Device device) {
        String uid = device.getId();
        serviceDescription.setDevice(device);
        serviceDescription.setFriendlyName(device.getName());
        serviceDescription.setIpAddress(device.getIPAddress());
        serviceDescription.setServiceID(SamsungLegacySmartViewService.ID);
        serviceDescription.setUUID(uid);
    }

    //  </editor-fold>
}
