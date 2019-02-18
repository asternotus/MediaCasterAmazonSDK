/*
 * FireTVDiscoveryProvider
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 08 Jul 2015
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
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.amazon.whisperlink.service.Device;
import com.amazon.whisperlink.service.Route;
import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.discovery.provider.firetv.FireTVApplicationProvider;
import com.connectsdk.discovery.provider.firetv.WhisperLink_2_Fixes;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FireTVDiscoveryProvider provides discovery implementation for FireTV devices.
 * FireTVDiscoveryProvider acts as a layer on top of Fling SDK, and requires the Fling SDK library
 * to function. This implementation does not use discovery filters because only one type of service
 * can be discovered. Currently it can discover only FireTV device with default media player
 * application.
 * <p/>
 * Using Connect SDK for discovery/control of FireTV devices will result in your app complying with
 * the Fling SDK terms of service.
 */
public class FireTVDiscoveryProvider implements DiscoveryProvider {

    private static final String LOG_TAG = FireTVDiscoveryProvider.class.getSimpleName();
    private static final long RESTART_DELAY_MILLISEC = 5000;
    private Context context;

    private FireTVApplicationProvider fireTVApplicationProvider;
    private DiscoveryController discoveryController;

    private boolean isRunning;

    DiscoveryController.IDiscoveryListener fireTVListener;

    ConcurrentHashMap<String, ServiceDescription> foundServices
            = new ConcurrentHashMap<>();

    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners
            = new CopyOnWriteArrayList<>();

    private String playerId;

    private boolean installServiceEnabled = true;

    public FireTVDiscoveryProvider(Context context) {
        this(new DiscoveryController(context), new FireTVApplicationProvider(context));
        this.context = context;
    }

    public FireTVDiscoveryProvider(DiscoveryController discoveryController, FireTVApplicationProvider fireTVApplicationProvider) {
        SmartLog.d(LOG_TAG, "FireTVDiscoveryProvider created ");
        this.discoveryController = discoveryController;
        this.fireTVApplicationProvider = fireTVApplicationProvider;

        this.fireTVListener = new FireTVDiscoveryListener();
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    /**
     * Safely start discovery. Ignore if it's already started.
     */
    @Override
    public void start() {
        startFlingDiscovery();
    }

    private void startFlingDiscovery() {
        SmartLog.d(LOG_TAG, "startFlingDiscovery ");

        if (!isRunning) {
            try {
                if (!TextUtils.isEmpty(playerId)) {
                    SmartLog.d(LOG_TAG, "Start discovery with playerID: " + playerId);
                    discoveryController.start(playerId, fireTVListener);
                } else {
                    SmartLog.d(LOG_TAG, "Start discovery");
                    discoveryController.start(fireTVListener);
                }
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                SmartLog.e(LOG_TAG, "Could not start discovery controller. " + ex.getMessage());
            }

            if (fireTVApplicationProvider != null && installServiceEnabled) {
                fireTVApplicationProvider.start(null);
            }

            isRunning = true;
        } else {
            SmartLog.d(LOG_TAG, "discovery has been already started! ");
        }
    }

    public void attemptToInstallApplication(String asin, FireTVApplicationProvider.InstallApplicationListener listener) {
        if (fireTVApplicationProvider != null) {
            fireTVApplicationProvider.installApplication(asin, playerId, listener);
        }
    }

    /**
     * Safely stop discovery and remove all found FireTV services because they don't work when
     * discovery is stopped. Ignore if it's already stopped.
     */
    @Override
    public void stop() {
        SmartLog.d(LOG_TAG, "stop");

        if (isRunning) {
            try {
                discoveryController.stop();
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                SmartLog.e(LOG_TAG, "Could not stop discovery controller. " + ex.getMessage());
            }

            isRunning = false;
        }

        for (ServiceDescription serviceDescription : foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        foundServices.clear();

        if (fireTVApplicationProvider != null && installServiceEnabled) {
            fireTVApplicationProvider.stop();
        }

//        WhisperLink_2_Fixes.resetWhisperLink();
    }

    /**
     * Safely restart discovery
     */
    @Override
    public void restart() {
        SmartLog.d(LOG_TAG, "restart");
        stop();
        start();
    }

    public void setInstallServiceEnabled(boolean installServiceEnabled) {
        this.installServiceEnabled = installServiceEnabled;
        if (!installServiceEnabled) {
            if (fireTVApplicationProvider != null && installServiceEnabled) {
                fireTVApplicationProvider.stop();
            }
        }
    }

    public boolean isInstallServiceEnabled() {
        return installServiceEnabled;
    }

    /**
     * Invokes restart method since FlingSDK doesn't have analog of rescan
     */
    @Override
    public void rescan() {
        // discovery controller doesn't have rescan capability
        restart();
    }

    /**
     * Stop discovery and removes all cached services
     */
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

    /**
     * DiscoveryFilter is not used in current implementation
     */
    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    /**
     * DiscoveryFilter is not used in current implementation
     */
    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    /**
     * DiscoveryFilter is not used in current implementation
     */
    @Override
    public void setFilters(List<DiscoveryFilter> filters) {
        // intentionally left blank
    }

    @Override
    public boolean isEmpty() {
        return foundServices.isEmpty();
    }

    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "notifyListenersThatServiceAdded ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceAdded(FireTVDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "notifyListenersThatServiceLost ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceRemoved(FireTVDiscoveryProvider.this, serviceDescription);
        }
    }

    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError error) {
        SmartLog.d(LOG_TAG, "notifyListenersThatDiscoveryFailed ");
        for (DiscoveryProviderListener listener : serviceListeners) {
            listener.onServiceDiscoveryFailed(FireTVDiscoveryProvider.this, error);
        }
    }


    class FireTVDiscoveryListener implements DiscoveryController.IDiscoveryListener {

        @Override
        public void playerDiscovered(RemoteMediaPlayer remoteMediaPlayer) {
            SmartLog.d(LOG_TAG, "playerDiscovered");
            if (remoteMediaPlayer == null) {
                SmartLog.e(LOG_TAG, "remote media player is null! ");
                return;
            }

            String uid = remoteMediaPlayer.getUniqueIdentifier();
            SmartLog.d(LOG_TAG, "Device ID  " + uid);
            SmartLog.d(LOG_TAG, "Device Name  " + remoteMediaPlayer.getName());

            ServiceDescription serviceDescription = foundServices.get(uid);

            if (serviceDescription == null) {
                serviceDescription = new ServiceDescription();
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
                foundServices.put(uid, serviceDescription);
                notifyListenersThatServiceAdded(serviceDescription);
            } else {
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
            }
        }

        @Override
        public void playerLost(RemoteMediaPlayer remoteMediaPlayer) {
            SmartLog.d(LOG_TAG, "playerLost");
            if (remoteMediaPlayer == null) {
                return;
            }
            ServiceDescription serviceDescription
                    = foundServices.get(remoteMediaPlayer.getUniqueIdentifier());
            if (serviceDescription != null) {
                notifyListenersThatServiceLost(serviceDescription);
                foundServices.remove(remoteMediaPlayer.getUniqueIdentifier());
            }
        }

        @Override
        public void discoveryFailure() {
            SmartLog.d(LOG_TAG, "discoveryFailure");
            final ServiceCommandError error = new ServiceCommandError("FireTV discovery failure");
            notifyListenersThatDiscoveryFailed(error);
        }


        public String getIpAddress(RemoteMediaPlayer remoteMediaPlayer) {
            for (Field field : remoteMediaPlayer.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = null;
                try {
                    value = field.get(remoteMediaPlayer);
                    if (value != null) {
                        SmartLog.d(LOG_TAG, field.getName() + "=" + value);
                    }
                    if (value instanceof Device) {
                        Device device = (Device) value;

                        Map<String, Route> routes = device.getRoutes();
                        for (String key : routes.keySet()) {
                            final Route route = routes.get(key);
                            final String ipv4 = route.getIpv4();
                            final String ipv6 = route.getIpv6();
                            SmartLog.d(LOG_TAG, String.format("route %s\n ipv4: %s\n ipv6: %s",
                                    key, ipv4, ipv6));
                            if (!TextUtils.isEmpty(ipv4)) {
                                return ipv4;
                            } else if (!TextUtils.isEmpty(ipv6)) {
                                return ipv6;
                            }

                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void updateServiceDescription(ServiceDescription serviceDescription,
                                              RemoteMediaPlayer remoteMediaPlayer) {
            String uid = remoteMediaPlayer.getUniqueIdentifier();
            serviceDescription.setDevice(remoteMediaPlayer);
            serviceDescription.setFriendlyName(remoteMediaPlayer.getName());
            serviceDescription.setIpAddress(getIpAddress(remoteMediaPlayer));
            serviceDescription.setServiceID(FireTVService.ID);
            serviceDescription.setUUID(uid);
        }

    }

}
