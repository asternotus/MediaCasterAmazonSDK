/*
 * SSDPDiscoveryProvider
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.discovery.provider;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.discovery.provider.ssdp.SSDPClient;
import com.connectsdk.discovery.provider.ssdp.SSDPDevice;
import com.connectsdk.discovery.provider.ssdp.SSDPPacket;
import com.connectsdk.service.config.ServiceDescription;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

public class SSDPDiscoveryProvider implements DiscoveryProvider {
    private static final String LOG_TAG = SSDPDiscoveryProvider.class.getSimpleName();
    Context context;

    boolean needToStartSearch = false;

    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;

    ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<String, ServiceDescription>();
    ConcurrentHashMap<String, ServiceDescription> discoveredServices = new ConcurrentHashMap<String, ServiceDescription>();

    List<DiscoveryFilter> serviceFilters;

    private SSDPClient ssdpClient;

    private Timer scanTimer;

    private Pattern uuidReg;

    private Thread responseThread;
    private Thread notifyThread;

    boolean isRunning = false;

    public SSDPDiscoveryProvider(Context context) {
        this.context = context;

        uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");

        serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
        serviceFilters = new CopyOnWriteArrayList<DiscoveryFilter>();
    }

    private void openSocket() {
        if (ssdpClient != null && ssdpClient.isConnected())
            return;

        try {
            InetAddress source = Util.getIpAddress(context);
            if (source == null)
                return;

            // // SmartLog.d(LOG_TAG, "creating socket");
            ssdpClient = createSocket(source);
        } catch (UnknownHostException e) {
            SmartLog.e(LOG_TAG, "UnknownHostException " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            SmartLog.e(LOG_TAG, "IOException " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected SSDPClient createSocket(InetAddress source) throws IOException {
        return new SSDPClient(source);
    }

    @Override
    public void start() {
        if (isRunning)
            return;

        // SmartLog.d(LOG_TAG, "start ");

        isRunning = true;

        openSocket();

        scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendSearch();
            }
        }, 100, RESCAN_INTERVAL);

        responseThread = new Thread(mResponseHandler);
        notifyThread = new Thread(mRespNotifyHandler);

        responseThread.start();
        notifyThread.start();

        // SmartLog.d(LOG_TAG, "provider was started! ");
    }

    public void sendSearch() {
//        // SmartLog.d(LOG_TAG, "sendSearch ");
        List<String> killKeys = new ArrayList<String>();

        long killPoint = new Date().getTime() - TIMEOUT;

        for (String key : foundServices.keySet()) {
            ServiceDescription service = foundServices.get(key);
            if (service == null || service.getLastDetection() < killPoint) {
                killKeys.add(key);
            }
        }

        for (String key : killKeys) {
            final ServiceDescription service = foundServices.get(key);

            if (service != null) {
                notifyListenersOfLostService(service);
            }

            if (foundServices.containsKey(key))
                foundServices.remove(key);
        }

        rescan();
    }

    @Override
    public void stop() {
        // SmartLog.d(LOG_TAG, "stop");

        isRunning = false;

        if (scanTimer != null) {
            scanTimer.cancel();
            scanTimer = null;
        }

        if (responseThread != null) {
            responseThread.interrupt();
            responseThread = null;
        }

        if (notifyThread != null) {
            notifyThread.interrupt();
            notifyThread = null;
        }

        if (ssdpClient != null) {
            ssdpClient.close();
            ssdpClient = null;
        }
    }

    @Override
    public void restart() {
        // // SmartLog.d(LOG_TAG, "restart");
        reset();
//        stop();
        start();
    }

    @Override
    public void reset() {
        stop();
        foundServices.clear();
        discoveredServices.clear();
    }

    @Override
    public void rescan() {
        // SmartLog.d(LOG_TAG, "rescan ");
        for (DiscoveryFilter searchTarget : serviceFilters) {
            final String message = SSDPClient.getSSDPSearchMessage(searchTarget.getServiceFilter());

            // // SmartLog.d(LOG_TAG, "rescan message\n" + message);

            Timer timer = new Timer();
            /* Send 3 times like WindowsMedia */
            for (int i = 0; i < 3; i++) {
                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {
                        try {
                            if (ssdpClient != null)
                                ssdpClient.send(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

                timer.schedule(task, i * 1000);
            }
        }

    }

    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {
        // // SmartLog.d(LOG_TAG, "addDeviceFilter " + filter.getServiceFilter());
        if (filter.getServiceFilter() == null) {
            SmartLog.e(Util.T, "This device filter does not have ssdp filter info");
        } else {
            serviceFilters.add(filter);
        }
    }

    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {
        serviceFilters.remove(filter);
    }

    @Override
    public void setFilters(List<DiscoveryFilter> filters) {
//        // SmartLog.d(LOG_TAG, "setFilters " + filters.size());
        serviceFilters = filters;
    }

    @Override
    public boolean isEmpty() {
        return serviceFilters.size() == 0;
    }

    private Runnable mResponseHandler = new Runnable() {
        @Override
        public void run() {
            // // SmartLog.d(LOG_TAG, "mResponseHandler ");
            while (ssdpClient != null) {
                try {
//                    // SmartLog.d(LOG_TAG, "mResponseHandler receive");
                    final DatagramPacket datagramPacket = ssdpClient.responseReceive();
//                    // SmartLog.d(LOG_TAG, "mResponseHandler received!");
                    handleSSDPPacket(new SSDPPacket(datagramPacket));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private Runnable mRespNotifyHandler = new Runnable() {
        @Override
        public void run() {
            // // SmartLog.d(LOG_TAG, "mRespNotifyHandler ");
            while (ssdpClient != null) {
                try {
                    // // SmartLog.d(LOG_TAG, "mRespNotifyHandler receive");
                    final DatagramPacket datagramPacket = ssdpClient.multicastReceive();
                    // // SmartLog.d(LOG_TAG, "mRespNotifyHandler received!");
                    handleSSDPPacket(new SSDPPacket(datagramPacket));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private void handleSSDPPacket(final SSDPPacket ssdpPacket) {
        // Debugging stuff
        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                SmartLog.d(LOG_TAG, "Packet received | type = " + ssdpPacket.getType());
//
                for (String key : ssdpPacket.getData().keySet()) {
                    SmartLog.d(LOG_TAG, "    " + key + " = " + ssdpPacket.getData().get(key));
                }
                SmartLog.d(LOG_TAG, "__________________________________________");
            }
        });
//         End Debugging stuff

        if (ssdpPacket == null || ssdpPacket.getData().size() == 0 || ssdpPacket.getType() == null)
            return;

        String serviceFilter = ssdpPacket.getData().get(ssdpPacket.getType().equals(SSDPClient.NOTIFY) ? "NT" : "ST");

        if (serviceFilter == null || SSDPClient.MSEARCH.equals(ssdpPacket.getType()) || !isSearchingForFilter(serviceFilter)) {
            return;
        }

        String usnKey = ssdpPacket.getData().get("USN");

        if (usnKey == null || usnKey.length() == 0)
            return;

        Matcher m = uuidReg.matcher(usnKey);

        if (!m.find()) {
            return;
        }

        String uuid = m.group();

        if (SSDPClient.BYEBYE.equals(ssdpPacket.getData().get("NTS"))) {
            final ServiceDescription service = foundServices.get(uuid);

            if (service != null) {
                foundServices.remove(uuid);

                notifyListenersOfLostService(service);
            }
        } else {
            String location = ssdpPacket.getData().get("LOCATION");

            if (location == null || location.length() == 0)
                return;

            ServiceDescription foundService = foundServices.get(uuid);
            ServiceDescription discoverdService = discoveredServices.get(uuid);

            boolean isNew = foundService == null && discoverdService == null;

            if (isNew) {
                foundService = new ServiceDescription();
                foundService.setUUID(uuid);
                foundService.setServiceFilter(serviceFilter);
                foundService.setIpAddress(ssdpPacket.getDatagramPacket().getAddress().getHostAddress());
                foundService.setPort(3001);

                discoveredServices.put(uuid, foundService);

                getLocationData(location, uuid, serviceFilter);
            }

            if (foundService != null)
                foundService.setLastDetection(new Date().getTime());
        }
    }

    public void getLocationData(final String location, final String uuid, final String serviceFilter) {
        try {
            getLocationData(new URL(location), uuid, serviceFilter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLocationData(final URL location, final String uuid, final String serviceFilter) {
        SmartLog.d(LOG_TAG, "getLocationData " + uuid + " , url: " + location);
        Util.runInBackground(new Runnable() {

            @Override
            public void run() {
                SSDPDevice device = null;
                try {
                    device = new SSDPDevice(location, serviceFilter);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }

                if (device != null) {
                    device.UUID = uuid;
                    boolean hasServices = containsServicesWithFilter(device, serviceFilter);

                    if (hasServices) {
                        final ServiceDescription service = discoveredServices.get(uuid);

                        if (service != null) {
                            service.setServiceFilter(serviceFilter);
                            service.setFriendlyName(device.friendlyName);
                            service.setModelName(device.modelName);
                            service.setModelNumber(device.modelNumber);
                            service.setModelDescription(device.modelDescription);
                            service.setManufacturer(device.manufacturer);
                            service.setApplicationURL(device.applicationURL);
                            service.setServiceList(device.serviceList);
                            service.setResponseHeaders(device.headers);
                            service.setLocationXML(device.locationXML);
                            service.setServiceURI(device.serviceURI);
                            service.setPort(device.port);

                            foundServices.put(uuid, service);

                            notifyListenersOfNewService(service);
                        }
                    }
                }

                discoveredServices.remove(uuid);
            }
        }, true);

    }

    private void notifyListenersOfNewService(ServiceDescription service) {
        SmartLog.i(LOG_TAG, "notifyListenersOfNewService ");
        List<String> serviceIds = serviceIdsForFilter(service.getServiceFilter());

        for (String serviceId : serviceIds) {
            ServiceDescription _newService = service.clone();
            _newService.setServiceID(serviceId);

            final ServiceDescription newService = _newService;

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {

                    for (DiscoveryProviderListener listener : serviceListeners) {
                        listener.onServiceAdded(SSDPDiscoveryProvider.this, newService);
                    }
                }
            });
        }
    }

    private void notifyListenersOfLostService(ServiceDescription service) {
        SmartLog.i(LOG_TAG, "notifyListenersOfLostService ");
        List<String> serviceIds = serviceIdsForFilter(service.getServiceFilter());

        for (String serviceId : serviceIds) {
            ServiceDescription _newService = service.clone();
            _newService.setServiceID(serviceId);

            final ServiceDescription newService = _newService;

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    for (DiscoveryProviderListener listener : serviceListeners) {
                        listener.onServiceRemoved(SSDPDiscoveryProvider.this, newService);
                    }
                }
            });
        }
    }

    public List<String> serviceIdsForFilter(String filter) {
        ArrayList<String> serviceIds = new ArrayList<String>();

        for (DiscoveryFilter serviceFilter : serviceFilters) {
            String ssdpFilter = serviceFilter.getServiceFilter();

            if (ssdpFilter.equals(filter)) {
                String serviceId = serviceFilter.getServiceId();

                if (serviceId != null)
                    serviceIds.add(serviceId);
            }
        }

        return serviceIds;
    }

    public boolean isSearchingForFilter(String filter) {
        for (DiscoveryFilter serviceFilter : serviceFilters) {
            String ssdpFilter = serviceFilter.getServiceFilter();

            if (ssdpFilter.equals(filter))
                return true;
        }

        return false;
    }

    public boolean containsServicesWithFilter(SSDPDevice device, String filter) {
//        List<String> servicesRequired = new ArrayList<String>();
//
//        for (JSONObject serviceFilter : serviceFilters) {
//        }

        //  TODO  Implement this method.  Not sure why needs to happen since there are now required services.

        return true;
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
        serviceListeners.remove(listener);
    }
}
