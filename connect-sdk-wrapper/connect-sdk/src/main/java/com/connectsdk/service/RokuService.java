/*
 * RokuService
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 26 Feb 2014
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

package com.connectsdk.service;

import android.text.TextUtils;

import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.service.capability.Installer;
import com.connectsdk.service.roku.CommandRequestMessageFactory;
import com.connectsdk.service.roku.RokuReceiverController;
import com.connectsdk.service.roku.model.CommandRequestMessage;
import com.connectsdk.service.roku.model.MetaDataKeys;
import com.connectsdk.service.roku.model.EventMessage;
import com.connectsdk.service.roku.model.EventType;
import com.mega.cast.utils.log.SmartLog;

import android.webkit.MimeTypeMap;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.etc.helper.DeviceServiceReachability;
import com.connectsdk.etc.helper.HttpConnection;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.roku.RokuApplicationListParser;
import com.connectsdk.service.sessions.LaunchSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RokuService extends DeviceService implements Launcher, Installer, MediaPlayer, MediaControl, KeyControl, TextInputControl {

    public static final String ID = "Roku";
    private static final String LOG_TAG = RokuService.class.getSimpleName();

    private static final double RECEIVER_UNSUPORTED_MEDIA_ERROR_CODE = -5.0;
    private static final double RECEIVER_TIMEOUT_MEDIA_ERROR_CODE = -2.0;
    private static List<String> registeredApps = new ArrayList<String>();

    DIALService dialService;

    static {
        registeredApps.add("YouTube");
        registeredApps.add("Netflix");
        registeredApps.add("Amazon");
    }

    private String clientAppType;


    public static void registerApp(String appId) {
        if (!registeredApps.contains(appId))
            registeredApps.add(appId);
    }

    private RokuReceiverController rokuReceiverController;

    private PlayStateSubscription playStateSubscription;
    private EventMessageSubscription eventMessageSubscription;

    private MediaControl.PlayStateStatus currentPlaystate = MediaControl.PlayStateStatus.Unknown;
    private AppInfo latestLaunchedAppInfo;
    private String clientVersion;

    public RokuService(ServiceDescription serviceDescription,
                       ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        rokuReceiverController = new RokuReceiverController();
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);

        if (this.serviceDescription != null)
            this.serviceDescription.setPort(8060);

        probeForAppSupport();
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "roku:ecp");
    }

    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        } else if (clazz.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        } else if (clazz.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        } else if (clazz.equals(Installer.class)) {
            return getInstallerCapabilityLevel();
        } else if (clazz.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        } else if (clazz.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override
    public Launcher getLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public Installer getInstaller() {
        return this;
    }

    @Override
    public void installApplication(final String appId, final ResponseListener<Boolean> listener) {
        SmartLog.d(LOG_TAG, "installApplication " + appId);

        home(new ResponseListener<Object>() {
            @Override
            public void onSuccess(Object object) {
                sendInstallCommand(appId, listener);
            }

            @Override
            public void onError(ServiceCommandError error) {
                sendInstallCommand(appId, listener);
            }
        });
    }

    private void sendInstallCommand(final String appId, final ResponseListener<Boolean> listener) {
        String action = "install";

        // TODO: 12/19/2017 Replace this with actual appId once we have a channel uploaded
        String uri = requestURL(action, appId);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, new ResponseListener<Object>() {
            @Override
            public void onSuccess(Object object) {
                SmartLog.e(LOG_TAG, "onSuccess ");
                listener.onSuccess(true);
            }

            @Override
            public void onError(final ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "onError " + error.getMessage());

                if (error.getCode() == ServiceCommandError.SERVICE_UNAVAILABLE && !appId.equals("12")) {
                    // dark magic
                    // we try to install Netflix, when immediately jump to install MegaCast
                    installApplication("12", new ResponseListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean object) {
                            installApplication(appId, listener);
                        }

                        @Override
                        public void onError(ServiceCommandError netflixError) {
                            listener.onError(error);
                        }
                    });
                } else {
                    listener.onError(error);
                }
            }
        });
        request.send();
    }

    @Override
    public CapabilityPriorityLevel getInstallerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void setClientAppType(String clientAppType) {
        this.clientAppType = clientAppType;
    }

    class RokuLaunchSession extends LaunchSession {

        public void close(ResponseListener<Object> responseListener) {
            home(responseListener);
        }
    }

    @Override
    public void launchApp(String appId, AppLaunchListener listener) {
        if (appId == null) {
            Util.postError(listener, new ServiceCommandError(0,
                    "Must supply a valid app id", null));
            return;
        }

        AppInfo appInfo = new AppInfo();
        appInfo.setId(appId);

        launchAppWithInfo(appInfo, listener);
    }

    @Override
    public void launchAppWithInfo(AppInfo appInfo,
                                  Launcher.AppLaunchListener listener) {
        launchAppWithInfo(appInfo, null, listener);
    }

    @Override
    public void launchAppWithInfo(final AppInfo appInfo, Object params,
                                  final Launcher.AppLaunchListener listener) {
        if (appInfo == null || appInfo.getId() == null) {
            Util.postError(listener, new ServiceCommandError(-1,
                    "Cannot launch app without valid AppInfo object",
                    appInfo));

            return;
        }

        String baseTargetURL = requestURL("launch", appInfo.getId());
        String queryParams = "";

        // The DeviceDiscoveryManager declares an showReceiver to receive the parameters as String
        // But the string params are created from a json object so lets repack them in a json object
        if (params != null) {
            if (params instanceof String) {
                try {
                    params = new JSONObject((String) params);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (params != null && params instanceof JSONObject) {
            JSONObject jsonParams = (JSONObject) params;

            int count = 0;
            Iterator<?> jsonIterator = jsonParams.keys();

            while (jsonIterator.hasNext()) {
                String key = (String) jsonIterator.next();
                String value = null;

                try {
                    value = jsonParams.getString(key);
                } catch (JSONException ex) {
                }

                if (value == null)
                    continue;

                String urlSafeKey = null;
                String urlSafeValue = null;
                String prefix = (count == 0) ? "?" : "&";

                try {
                    urlSafeKey = URLEncoder.encode(key, "UTF-8");
                    urlSafeValue = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException ex) {

                }

                if (urlSafeKey == null || urlSafeValue == null)
                    continue;

                String appendString = prefix + urlSafeKey + "=" + urlSafeValue;
                queryParams = queryParams + appendString;

                count++;
            }
        }

        String targetURL = null;

        if (queryParams.length() > 0)
            targetURL = baseTargetURL + queryParams;
        else
            targetURL = baseTargetURL;

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                LaunchSession launchSession = new RokuLaunchSession();
                launchSession.setService(RokuService.this);
                launchSession.setAppId(appInfo.getId());
                launchSession.setAppName(appInfo.getName());
                launchSession.setSessionType(LaunchSession.LaunchSessionType.App);
                latestLaunchedAppInfo = appInfo;
                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, targetURL, null, responseListener);
        request.send();
    }

    @Override
    public void closeApp(LaunchSession launchSession,
                         ResponseListener<Object> listener) {
        home(listener);
    }

    @Override
    public void closeApp(String appId, ResponseListener<Object> listener) {
        // Unfortunately Roku ECP doesnt have exit, quite or close command and neither does Dial
        home(listener);
    }

    @Override
    public void getAppList(final AppListListener listener) {
        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                String msg = (String) response;

                if (msg == null) {
                    Util.postError(listener,
                            new ServiceCommandError(ServiceCommandError.INTERNAL_SERVER_ERROR, "Error getting app list."));
                    return;
                }

                SAXParserFactory saxParserFactory = SAXParserFactory
                        .newInstance();
                InputStream stream;
                try {
                    stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
                    SAXParser saxParser = saxParserFactory.newSAXParser();

                    RokuApplicationListParser parser = new RokuApplicationListParser();
                    saxParser.parse(stream, parser);

                    List<AppInfo> appList = parser.getApplicationList();

                    Util.postSuccess(listener, appList);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        String action = "query";
        String param = "apps";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, responseListener);
        request.setHttpMethod(ServiceCommand.TYPE_GET);
        request.send();
    }

    @Override
    public void getRunningApp(AppInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<AppInfoListener> subscribeRunningApp(
            AppInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return new NotSupportedServiceSubscription<AppInfoListener>();
    }

    @Override
    public void getAppState(LaunchSession launchSession,
                            AppStateListener listener) {
        queryAppState(launchSession.getAppId(), listener);
    }

    @Override
    public void getAppState(String getAppId, AppStateListener listener) {
        queryAppState(getAppId, listener);
    }

    private void queryAppState(final String appId, final AppStateListener listener) {
        checkIfAppIsInstalled(appId, new ResponseListener<Boolean>() {
            @Override
            public void onSuccess(Boolean appInstalledStatus) {
                if (appInstalledStatus) {
                    // App is installed..lets check its state
                    String action = "query";
                    String param = "active-app";

                    String uri = requestURL(action, param);

                    ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                        @Override
                        public void onSuccess(Object response) {
                            String msg = (String) response;

                            SAXParserFactory saxParserFactory = SAXParserFactory
                                    .newInstance();
                            InputStream stream;
                            try {
                                stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
                                SAXParser saxParser = saxParserFactory.newSAXParser();

                                RokuApplicationListParser parser = new RokuApplicationListParser();
                                saxParser.parse(stream, parser);

                                List<AppInfo> appList = parser.getApplicationList();

                                if (!appList.isEmpty() && appList.get(0).getId().equals(appId)) {
                                    latestLaunchedAppInfo = appList.get(0);
                                    Util.postSuccess(listener, new AppState(true, true));
                                } else {
                                    Util.postSuccess(listener, new AppState(false, false));
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ServiceCommandError error) {
                            Util.postError(listener, error);
                        }
                    };

                    ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                            RokuService.this, uri, null, responseListener);
                    request.setHttpMethod(ServiceCommand.TYPE_GET);
                    request.send();
                } else {
                    Util.postError(listener, ServiceCommandError.getError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED));
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void checkIfAppIsInstalled(final String appId, final ResponseListener<Boolean> listener) {
        getAppList(new AppListListener() {

            @Override
            public void onError(ServiceCommandError error) {
            }

            @Override
            public void onSuccess(List<AppInfo> object) {
                boolean found = false;
                for (AppInfo app : object) {
                    if (appId.equals(app.getId())) {
                        found = true;
                        break;
                    }
                }

                if (listener != null) {
                    listener.onSuccess(found);
                }
            }
        });
    }

    @Override
    public ServiceSubscription<AppStateListener> subscribeAppState(
            LaunchSession launchSession, AppStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return null;
    }

    @Override
    public void launchBrowser(String url, Launcher.AppLaunchListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void launchYouTube(String contentId,
                              Launcher.AppLaunchListener listener) {
        launchYouTube(contentId, (float) 0.0, listener);
    }

    @Override
    public void launchYouTube(String contentId, float startTime,
                              AppLaunchListener listener) {
        if (getDIALService() != null) {
            getDIALService().getLauncher().launchYouTube(contentId, startTime,
                    listener);
        } else {
            Util.postError(listener, new ServiceCommandError(
                    0,
                    "Cannot reach DIAL service for launching with provided start time",
                    null));
        }
    }

    @Override
    public void launchNetflix(final String contentId,
                              final Launcher.AppLaunchListener listener) {
        getAppList(new AppListListener() {

            @Override
            public void onSuccess(List<AppInfo> appList) {
                for (AppInfo appInfo : appList) {
                    if (appInfo.getName().equalsIgnoreCase("Netflix")) {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("mediaType", "movie");

                            if (contentId != null && contentId.length() > 0)
                                payload.put("contentId", contentId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        launchAppWithInfo(appInfo, payload, listener);
                        break;
                    }
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void launchHulu(final String contentId,
                           final Launcher.AppLaunchListener listener) {
        getAppList(new AppListListener() {

            @Override
            public void onSuccess(List<AppInfo> appList) {
                for (AppInfo appInfo : appList) {
                    if (appInfo.getName().contains("Hulu")) {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("contentId", contentId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        launchAppWithInfo(appInfo, payload, listener);
                        break;
                    }
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void launchAppStore(final String appId, AppLaunchListener listener) {
        AppInfo appInfo = new AppInfo("11");
        appInfo.setName("Channel Store");

        JSONObject params = null;
        try {
            params = new JSONObject() {
                {
                    put("contentId", appId);
                }
            };
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public KeyControl getKeyControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void up(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Up";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void down(final ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Down";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void left(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Left";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void right(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Right";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void ok(final ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Select";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void back(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Back";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void home(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Home";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void showProgressScreen(ResponseListener<Object> responseListener) {
        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createShowProgressScreenRequest());

        responseListener.onSuccess(null);
    }

    @Override
    public void hideProgressScreen(ResponseListener<Object> responseListener) {
        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createHideProgressScreenRequest());

        responseListener.onSuccess(null);
    }

    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void play(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "play ");

        String action = "keypress";
        String param = "Play";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();

//        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
//            @Override
//            public void onSuccess(MediaControl.PlayStateStatus object) {
//                SmartLog.d(LOG_TAG, "play state received " + object.name());
//                if (object == MediaControl.PlayStateStatus.Playing) {
//                    SmartLog.d(LOG_TAG, "play successful! ");
//                    listener.onSuccess(object);
//                    playStateSubscription.removeListener(this);
//                }
//            }
//
//            @Override
//            public void onError(ServiceCommandError error) {
//                listener.onError(error);
//                playStateSubscription.removeListener(this);
//            }
//        });
//
//        rokuReceiverController
//                .sendCommand(CommandRequestMessageFactory.createResumeRequest());
    }

    @Override
    public void changeSubtitles(SubtitleInfo info, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void pause(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "pause ");

        String action = "keypress";
        String param = "Play";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();

//        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
//            @Override
//            public void onSuccess(MediaControl.PlayStateStatus object) {
//                SmartLog.d(LOG_TAG, "pause state received " + object.name());
//                if (object == MediaControl.PlayStateStatus.Paused) {
//                    SmartLog.d(LOG_TAG, "pause successful! ");
//                    listener.onSuccess(object);
//                    playStateSubscription.removeListener(this);
//                }
//            }
//
//            @Override
//            public void onError(ServiceCommandError error) {
//                listener.onError(error);
//                playStateSubscription.removeListener(this);
//            }
//        });
//
//        rokuReceiverController
//                .sendCommand(CommandRequestMessageFactory.createPauseRequest());
    }

    @Override
    public void stop(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "stop ");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "stop state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Finished || object == MediaControl.PlayStateStatus.Idle) {
                    SmartLog.d(LOG_TAG, "stop successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createStopRequest());
    }

    @Override
    public void rewind(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Rev";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void fastForward(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Fwd";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void previous(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void next(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void getDuration(final MediaControl.DurationListener listener) {
        SmartLog.d(LOG_TAG, "getDuration");

        eventMessageSubscription.addListener(new EventMessageListener() {
            @Override
            public void onSuccess(EventMessage object) {
                if (object.getMessageType().equals(EventType.DURATION_EVENT)) {
                    SmartLog.d(LOG_TAG, "EventMessage received " + object.getMessageType());
                    Double duration = object.getDataValAsDouble(MetaDataKeys.VALUE_EXTRA_KEY);
                    if (duration != null
                            && duration >= 0) {
                        listener.onSuccess(duration.longValue());
                        eventMessageSubscription.removeListener(this);
                    }
                }

            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "getDuration onError");
                listener.onError(error);
                eventMessageSubscription.removeListener(this);
            }
        });

        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createGetDurationRequest());
    }

    @Override
    public void getPosition(final MediaControl.PositionListener listener) {
        SmartLog.d(LOG_TAG, "getPosition");

        eventMessageSubscription.addListener(new EventMessageListener() {
            @Override
            public void onSuccess(EventMessage object) {
                if (object.getMessageType().equals(EventType.POSITION_EVENT)) {
                    SmartLog.d(LOG_TAG, "EventMessage received " + object.getMessageType());
                    Double position = object.getDataValAsDouble(MetaDataKeys.VALUE_EXTRA_KEY);
                    if (position >= 0) {
                        listener.onSuccess(position.longValue());
                        eventMessageSubscription.removeListener(this);
                    }
                }

            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "getPosition onError");
                listener.onError(error);
                eventMessageSubscription.removeListener(this);
            }
        });

        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createGetPositionRequest());
    }

    @Override
    public void seek(long position, final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "seek " + position);

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "seek state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Playing || object == MediaControl.PlayStateStatus.Paused) {
                    SmartLog.d(LOG_TAG, "seek successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "seek onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createSeekRequest(position));
    }

    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void getMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(
            MediaInfoListener listener) {
        listener.onError(ServiceCommandError.notSupported());
        return null;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    public void displayImage(String url, String mimeType, String title,
                             String description, String iconSrc,
                             MediaPlayer.LaunchListener listener) {
        SmartLog.d(LOG_TAG, String.format("display media for roku" +
                "\n url: %s" +
                "\n mimeType: %s" +
                "\n title: %s" +
                "\n description: %s" +
                "\n iconSrc: %s", url, mimeType, title, description, iconSrc));
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    @Override
    public void displayImage(MediaInfo mediaInfo,
                             MediaPlayer.LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title,
                          String description, String iconSrc, boolean shouldLoop,
                          MediaPlayer.LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop,
                          MediaPlayer.LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    private void setMediaSource(MediaInfo mediaInfo, final MediaPlayer.LaunchListener listener) {
        SmartLog.d(LOG_TAG, "setMediaSource " + mediaInfo.getTitle());

        if (TextUtils.isEmpty(mediaInfo.getMimeType())) {
            listener.onError(new ServiceCommandError("Wrong media info: mime type is null!"));
        }

        final CommandRequestMessage playRequest = CommandRequestMessageFactory.createPlayRequest(mediaInfo);

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {

            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "[play] state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Playing) {
                    SmartLog.d(LOG_TAG, "play successful! ");
                    listener.onSuccess(createMediaLaunchObject());
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "[play] onError ");

                if (error.getCode() == ServiceCommandError.MEDIA_UNSUPPORTED) {
                    SmartLog.e(LOG_TAG, "[play] media format is not supported! ");
                }

                listener.onError(error);

                playStateSubscription.removeListener(this);
            }
        });

        rokuReceiverController.sendCommand(playRequest);
    }

    private MediaPlayer.MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(latestLaunchedAppInfo.getId());
        launchSession.setAppName(latestLaunchedAppInfo.getName());
        MediaPlayer.MediaLaunchObject mediaLaunchObject = new MediaPlayer.MediaLaunchObject(launchSession, this);
        return mediaLaunchObject;
    }

    @Override
    public void closeMedia(LaunchSession launchSession,
                           ResponseListener<Object> listener) {
        stop(listener);
    }

    @Override
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(
            TextInputStatusListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return new NotSupportedServiceSubscription<TextInputStatusListener>();
    }

    @Override
    public void sendText(String input) {
        if (input == null || input.length() == 0) {
            return;
        }

        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = null;
        try {
            param = "Lit_" + URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This can be safetly ignored since it isn't a dynamic encoding.
            e.printStackTrace();
        }

        String uri = requestURL(action, param);

        SmartLog.d(Util.T, "RokuService::send() | uri = " + uri);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void sendKeyCode(KeyCode keyCode, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void sendEnter() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = "Enter";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void sendDelete() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = "Backspace";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void unsubscribe(URLServiceSubscription<?> subscription) {
    }

    @Override
    public void sendCommand(final ServiceCommand<?> mCommand) {
        Util.runInBackground(new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mCommand;
                Object payload = command.getPayload();

                try {
                    SmartLog.d("", "RESP " + command.getTarget());
                    HttpConnection connection = HttpConnection.newInstance(URI.create(command.getTarget()));
                    if (command.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
                        connection.setMethod(HttpConnection.Method.POST);
                        if (payload != null) {
                            connection.setPayload(payload.toString());
                        }
                    }
                    connection.execute();
                    int code = connection.getResponseCode();
                    SmartLog.d("", "RESP " + code);
                    if (code == 200 || code == 201 || code == 204) {
                        Util.postSuccess(command.getResponseListener(), connection.getResponseString());
                    } else {
                        Util.postError(command.getResponseListener(), ServiceCommandError.getError(code));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.postError(command.getResponseListener(), new ServiceCommandError(0, e.getMessage(), null));
                }

            }
        });
    }

    private String requestURL(String action, String parameter) {
        StringBuilder sb = new StringBuilder();

        sb.append("http://");
        sb.append(serviceDescription.getIpAddress()).append(":");
        sb.append(serviceDescription.getPort()).append("/");

        if (action != null)
            sb.append(action);

        if (parameter != null)
            sb.append("/").append(parameter);

        SmartLog.d(LOG_TAG, "request url " + sb.toString());

        return sb.toString();
    }

    private void probeForAppSupport() {
        getAppList(new AppListListener() {

            @Override
            public void onError(ServiceCommandError error) {
            }

            @Override
            public void onSuccess(List<AppInfo> object) {
                List<String> appsToAdd = new ArrayList<String>();

                for (String probe : registeredApps) {
                    for (AppInfo app : object) {
                        if (app.getName().contains(probe)) {
                            appsToAdd.add("Launcher." + probe);
                            appsToAdd.add("Launcher." + probe + ".Params");
                        }
                    }
                }

                addCapabilities(appsToAdd);
            }
        });
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();

        capabilities.add(Up);
        capabilities.add(Down);
        capabilities.add(Left);
        capabilities.add(Right);
        capabilities.add(OK);
        capabilities.add(Back);
        capabilities.add(Home);
        capabilities.add(Send_Key);

        capabilities.add(Application);
        capabilities.add(AppState);

        capabilities.add(Application_Params);
        capabilities.add(Application_List);
        capabilities.add(AppStore);
        capabilities.add(AppStore_Params);
        capabilities.add(Application_Close);

        capabilities.add(Installer.Application_Install);

        capabilities.add(Display_Image);
        capabilities.add(Play_Video);
        capabilities.add(Play_Audio);
        capabilities.add(Close);
        capabilities.add(MetaData_Title);

        capabilities.add(FastForward);
        capabilities.add(Rewind);
        capabilities.add(Play);
        capabilities.add(Pause);

        capabilities.add(Send);
        capabilities.add(Send_Delete);
        capabilities.add(Send_Enter);

        setCapabilities(capabilities);
    }

    @Override
    public void getPlayState(final PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "getPlayState");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
                listener.onSuccess(object);
                playStateSubscription.removeListener(this);
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "getPlayState onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        rokuReceiverController
                .sendCommand(CommandRequestMessageFactory.createStatusRequest());
    }

    //TODO required
    @Override
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "subscribePlayState ");
        if (!playStateSubscription.getListeners().contains(listener)) {
            playStateSubscription.addListener(listener);
        }
        getPlayState(listener);
        return playStateSubscription;
    }

    @Override
    public void unSubscribePlayState(MediaControl.PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "unSubscribePlayState ");
        if (playStateSubscription != null && playStateSubscription.getListeners().contains(listener)) {
            SmartLog.d(LOG_TAG, "remove listener ");
            playStateSubscription.removeListener(listener);
        }
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect() {
        SmartLog.d(LOG_TAG, "connect");
        connected = true;

        rokuReceiverController.setConnectionStateListener(new RokuReceiverController.ConnectionStateListener() {
            @Override
            public void onSuccessfulConnection() {
                SmartLog.d(LOG_TAG, "onSuccessfulConnection to Roku Receiver");
                reportConnected(true);
                rokuReceiverController
                        .sendCommand(CommandRequestMessageFactory.createClientInfoMsg(clientVersion));
                rokuReceiverController
                        .sendCommand(CommandRequestMessageFactory.createClientAppTypeMsg(clientAppType));
            }

            @Override
            public void onForcedDisconnection() {
                SmartLog.d(LOG_TAG, "onForcedDisconnection from Roku Receiver");
                if (currentPlaystate == PlayStateStatus.Playing
                        || currentPlaystate == PlayStateStatus.Buffering
                        || currentPlaystate == PlayStateStatus.Paused) {
                    currentPlaystate = PlayStateStatus.Error;
                    playStateSubscription.notifyListeners(currentPlaystate);
                }

                disconnect();
            }
        });

        if (playStateSubscription != null) {
            playStateSubscription.unsubscribe();
        }
        if (eventMessageSubscription != null) {
            eventMessageSubscription.unsubscribe();
        }

        playStateSubscription = new RokuService.PlayStateSubscription();
        eventMessageSubscription = new RokuService.EventMessageSubscription();

        rokuReceiverController.setReceiverAddr(getServiceDescription().getIpAddress());
        rokuReceiverController.connectToReceiver();

    }

    @Override
    public void disconnect() {
        SmartLog.d(LOG_TAG, "disconnect");
        connected = false;

        if (mServiceReachability != null)
            mServiceReachability.stop();

        if (playStateSubscription != null) {
            if (currentPlaystate != MediaControl.PlayStateStatus.Finished
                    && currentPlaystate != PlayStateStatus.Error) {
                playStateSubscription.notifyListeners(MediaControl.PlayStateStatus.Finished);
            }

            playStateSubscription.unsubscribe();
        }

        if (eventMessageSubscription != null) {
            eventMessageSubscription.unsubscribe();
        }

        rokuReceiverController.disconnectFromReceiver();

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                if (listener != null)
                    listener.onDisconnect(RokuService.this, null);
            }
        });
    }

    @Override
    public void onLoseReachability(DeviceServiceReachability reachability) {
        if (connected) {
            disconnect();
        } else {
            if (mServiceReachability != null)
                mServiceReachability.stop();
        }
    }

    public DIALService getDIALService() {
        if (dialService == null) {
            DiscoveryManager discoveryManager = DiscoveryManager.getInstance();
            ConnectableDevice device = discoveryManager.getAllDevices().get(
                    serviceDescription.getIpAddress());

            if (device != null) {
                DIALService foundService = null;

                for (DeviceService service : device.getServices()) {
                    if (DIALService.class.isAssignableFrom(service.getClass())) {
                        foundService = (DIALService) service;
                        break;
                    }
                }

                dialService = foundService;
            }
        }

        return dialService;
    }

    private MediaControl.PlayStateStatus createPlayStateStatusFromRokuReceiverStatus(EventMessage eventMessage) {
        String type = eventMessage.getMessageType();
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(EventType.STATUS_EVENT)) {
                String state = eventMessage.getDataValAsString(MetaDataKeys.STATE_EXTRA_KEY);
                if (state != null) {
                    switch (state) {
                        case "error":
                            currentPlaystate = MediaControl.PlayStateStatus.Error;
                            break;
                        case "buffering":
                            currentPlaystate = MediaControl.PlayStateStatus.Buffering;
                            break;
                        case "playing":
                            currentPlaystate = MediaControl.PlayStateStatus.Playing;
                            break;
                        case "paused":
                            currentPlaystate = MediaControl.PlayStateStatus.Paused;
                            break;
                        case "stopped":
                        case "finished":
                            currentPlaystate = MediaControl.PlayStateStatus.Finished;
                            break;
                        case "cancelled":
                            currentPlaystate = MediaControl.PlayStateStatus.Cancelled;
                            break;
                        default:
                            currentPlaystate = MediaControl.PlayStateStatus.Unknown;
                    }
                    return currentPlaystate;
                }
            }
        }

        return null;
    }

    /**
     * Internal play state subscription implementation
     */
    class PlayStateSubscription extends RokuService.Subscription<PlayStateStatus, PlayStateListener>
            implements RokuReceiverController.MessageListener {

        public PlayStateSubscription() {
            init();
        }

        public PlayStateSubscription(PlayStateListener listener) {
            super(listener);
            init();
        }

        @Override
        public void unsubscribe() {
            if (rokuReceiverController != null) {
                rokuReceiverController.removeMessageListener(this);
            }
            listeners.clear();
        }

        private void init() {
            setStatusRepetitionAllowed(true);
            rokuReceiverController.addMessageListener(this);
        }

        @Override
        public void onMessageReceived(EventMessage eventMessage) {
            switch (eventMessage.getMessageType()) {
                case EventType.STATUS_EVENT:
                    MediaControl.PlayStateStatus status = createPlayStateStatusFromRokuReceiverStatus(eventMessage);
                    if (status != null) {
                        notifyListeners(status);
                        if (status == PlayStateStatus.Error) {
                            String errorMsg = eventMessage.getDataValAsString(MetaDataKeys.ERROR_MSG_EXTRA_KEY);
                            Double errorCode = eventMessage.getDataValAsDouble(MetaDataKeys.ERROR_CODE_EXTRA_KEY);

                            if (errorMsg == null) {
                                errorMsg = "Could not play content!";
                                errorCode = -1.0;
                            }

                            SmartLog.e(LOG_TAG, errorMsg + " " + errorCode);
                            ServiceCommandError error = null;
                            if (errorCode == RECEIVER_UNSUPORTED_MEDIA_ERROR_CODE || errorCode == RECEIVER_TIMEOUT_MEDIA_ERROR_CODE) {
                                error = new ServiceCommandError(ServiceCommandError.MEDIA_UNSUPPORTED,
                                        errorMsg);
                            } else {
                                error = new ServiceCommandError(ServiceCommandError.INTERNAL_SERVER_ERROR,
                                        errorMsg);
                            }

                            notifyListenersError(error);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Internal event message subscription implementation
     */
    // TODO: 12/6/2017 Maybe remove this subscription type since all events can be implemented through the playStateSubscription
    class EventMessageSubscription extends RokuService.Subscription<EventMessage, EventMessageListener> implements RokuReceiverController.MessageListener {

        public EventMessageSubscription() {
            init();
        }

        public EventMessageSubscription(EventMessageListener listener) {
            super(listener);
            init();
        }

        @Override
        public void unsubscribe() {
            if (rokuReceiverController != null) {
                rokuReceiverController.removeMessageListener(this);
            }
            listeners.clear();
        }

        private void init() {
            if (rokuReceiverController != null) {
                rokuReceiverController.addMessageListener(this);
            }
        }

        @Override
        public void onMessageReceived(EventMessage eventMessage) {
            notifyListeners(eventMessage);
        }
    }

    /**
     * Success block that is called upon any recevied event message from the {@link RokuReceiverController}
     * <p>
     * Passes a EventMessage that was received from the receiver
     */
    interface EventMessageListener extends ResponseListener<EventMessage> {

    }

    private abstract static class Subscription<Status, Listener extends ResponseListener<Status>>
            implements ServiceSubscription<Listener> {

        List<Listener> listeners = new ArrayList<Listener>();

        Status prevStatus;
        private boolean statusRepetitionAllowed = false;

        public Subscription() {
        }

        public Subscription(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
        }

        synchronized void notifyListeners(final Status status) {
            if (!status.equals(prevStatus) || statusRepetitionAllowed) {
                List<Listener> listenersIterate = new ArrayList<Listener>();
                listenersIterate.addAll(listeners);

                for (final Listener listener : listenersIterate) {
                    // Maybe wrap the whole method to run on UI thread? Gotta check first if synchronized played a vital role here...
                    // Other receivers (amazon, samsung) receive their messages on the UI thread apparently from their msg comm SDK's
                    Util.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess(status);
                        }
                    });
                }
            }
            prevStatus = status;
        }

        synchronized void notifyListenersError(final ServiceCommandError error) {
            List<Listener> listenersIterate = new ArrayList<Listener>();
            listenersIterate.addAll(listeners);

            for (final Listener listener : listenersIterate) {
                Util.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(error);
                    }
                });
            }
        }

        @Override
        public Listener addListener(Listener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
            return listener;
        }

        @Override
        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        @Override
        public List<Listener> getListeners() {
            return listeners;
        }

        public void setStatusRepetitionAllowed(boolean statusRepetitionAllowed) {
            this.statusRepetitionAllowed = statusRepetitionAllowed;
        }
    }


}
