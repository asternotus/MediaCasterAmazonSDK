package com.megacast.connectsdkwrapper.internal.managers;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.connectsdk.service.capability.Installer;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.AppInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.megacast.connectsdkwrapper.internal.model.ConnectableApplicationSession;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.castsdk.model.ApplicationSession;
import com.megacast.castsdk.model.ApplicationState;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Dmitry on 16.09.16.
 */
public class RemoteApplicationManagerImpl implements RemoteApplicationManager {

    private static final String LOG_TAG = RemoteApplicationManagerImpl.class.getSimpleName();

    private String[] requiredCapabilities = {
            Launcher.Application, Launcher.AppState,
    };

    private String[] requiredInstallCapabilities = Installer.Capabilities;

    //  <editor-fold desc="public interface">

    @Override
    public Observable<Void> showTVProgressBar( final Device device) {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

                        connectableDevice.getMediaControl()
                                .showProgressScreen(new ResponseListener<Object>() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        sub.onNext(null);
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onError(ServiceCommandError error) {
                                        sub.onError(new Throwable(error.getMessage()));
                                    }
                                });
                    }
                }
        );
    }

    @Override
    public Observable<Void> hideTVProgressBar( final Device device) {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

                        connectableDevice.getMediaControl()
                                .hideProgressScreen(new ResponseListener<Object>() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        sub.onNext(null);
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onError(ServiceCommandError error) {
                                        sub.onError(new Throwable(error.getMessage()));
                                    }
                                });
                    }
                }
        );
    }

    @Override
    public Observable<ApplicationSession> launchApplication( final Device device,  final String appId, @Nullable final Object params) {
        return Observable.create(
                new Observable.OnSubscribe<ApplicationSession>() {
                    @Override
                    public void call(final Subscriber<? super ApplicationSession> sub) {
                        launchApplication(device, appId, params, new Launcher.AppLaunchListener() {
                            @Override
                            public void onSuccess(LaunchSession object) {
                                SmartLog.d(LOG_TAG, "launchApplication onSuccess");
                                ApplicationSession session = new ConnectableApplicationSession(object);
                                sub.onNext(session);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.d(LOG_TAG, "launchApplication onError " + error.getMessage());
                                if (error.getCause() != null) {
                                    sub.onError(error.getCause());
                                } else {
                                    sub.onError(new IllegalAccessException(error.getMessage()));
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<ApplicationState> getApplicationState( final Device device,  final ApplicationSession applicationSession) {
        return Observable.create(
                new Observable.OnSubscribe<ApplicationState>() {
                    @Override
                    public void call(final Subscriber<? super ApplicationState> sub) {
                        getApplicationState(device, applicationSession, new Launcher.AppStateListener() {
                            @Override
                            public void onSuccess(Launcher.AppState object) {
                                SmartLog.d(LOG_TAG, "getReceiverState onSuccess");
                                ApplicationState state = new ApplicationState(object.running, object.visible);
                                sub.onNext(state);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.d(LOG_TAG, "getReceiverState onError " + error.getMessage());
                                if (error.getCause() != null) {
                                    sub.onError(error.getCause());
                                } else {
                                    sub.onError(new IllegalAccessException(error.getMessage()));
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<ApplicationState> getApplicationState( final Device device,  final String appID) {
        return Observable.create(
                new Observable.OnSubscribe<ApplicationState>() {
                    @Override
                    public void call(final Subscriber<? super ApplicationState> sub) {
                        getApplicationState(device, appID, new Launcher.AppStateListener() {
                            @Override
                            public void onSuccess(Launcher.AppState object) {
                                SmartLog.d(LOG_TAG, "getReceiverState onSuccess");
                                ApplicationState state = new ApplicationState(object.running, object.visible);
                                sub.onNext(state);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.d(LOG_TAG, "getReceiverState onError " + error.getMessage());
                                if (error.getCause() != null) {
                                    sub.onError(error.getCause());
                                } else {
                                    sub.onError(new IllegalAccessException(error.getMessage()));
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<Void> closeApplication( final Device device,  final String receiverAppId) {
        SmartLog.i(LOG_TAG, "closeApplication ");
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        closeApplication(device, receiverAppId, new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                SmartLog.d(LOG_TAG, "close application success ");
                                sub.onNext(null);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.e(LOG_TAG, "close application error " + error.getMessage());
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<List<String>> getAppList( final Device device) {
        SmartLog.d(LOG_TAG, "getAppList");
        return Observable.create(
                new Observable.OnSubscribe<List<String>>() {
                    @Override
                    public void call(final Subscriber<? super List<String>> sub) {
                        getApplist(device, new Launcher.AppListListener() {
                            @Override
                            public void onSuccess(List<AppInfo> object) {
                                SmartLog.d(LOG_TAG, "getAppList onSuccess " + object.size());
                                List<String> appList = new ArrayList<String>();
                                for (AppInfo appInfo : object) {
                                    appList.add(appInfo.getId() + " " + appInfo.getName());
                                }
                                sub.onNext(appList);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.e(LOG_TAG, "getAppList error " + error.getMessage());
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<Object> installApplication( final Device device, final String appId) {
        SmartLog.d(LOG_TAG, "installApplication");
        return Observable.create(
                new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(final Subscriber<? super Object> sub) {
                        installApplication(device, appId, new ResponseListener<Boolean>() {
                            @Override
                            public void onSuccess(Boolean object) {
                                SmartLog.d(LOG_TAG, "installApplication onSuccess " + object.toString());
                                sub.onNext(object);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.e(LOG_TAG, "installApplication error " + error.getMessage());
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    private void installApplication(final Device device, String appId, ResponseListener<Boolean> listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
            return;
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        if (!connectableDevice.hasCapabilities(requiredInstallCapabilities)) {
            listener.onError(new ServiceCommandError("Device doesnt't have Installer capabilities!"));
            return;
        }

        connectableDevice
                .getCapability(Installer.class)
                .installApplication(appId, listener);
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private void launchApplication(final Device device, String appId, Object params, final Launcher.AppLaunchListener listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
            return;
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appId);
        appInfo.setId(appId);

        connectableDevice.getCapability(Launcher.class)
                .launchAppWithInfo(appInfo, params, new Launcher.AppLaunchListener() {
                    @Override
                    public void onSuccess(LaunchSession object) {
                        listener.onSuccess(object);
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        listener.onError(error);
                    }
                });
    }

    private void closeApplication(Device device, String appId, ResponseListener<Object> listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        connectableDevice.getCapability(Launcher.class).closeApp(appId, listener);
    }

    private void getApplicationState(Device device, ApplicationSession applicationSession, Launcher.AppStateListener listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
            return;
        }

        if (!(applicationSession instanceof ConnectableApplicationSession)) {
            listener.onError(new ServiceCommandError("Wrong session type!"));
            return;
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        Launcher launcher = connectableDevice.getLauncher();

        if (!connectableDevice.hasCapabilities(requiredCapabilities) || launcher == null) {
            listener.onError(new ServiceCommandError("Device hasn't Launcher capabilities!"));
            return;
        }

        LaunchSession session = ((ConnectableApplicationSession) applicationSession).getLaunchSession();

        launcher.getAppState(session, listener);
    }

    private void getApplicationState(Device device, String appID, Launcher.AppStateListener listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
            return;
        }

        if (TextUtils.isEmpty(appID)) {
            listener.onError(new ServiceCommandError("App ID is null"));
            return;
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        Launcher launcher = connectableDevice.getLauncher();

        if (!connectableDevice.hasCapabilities(requiredCapabilities) || launcher == null) {
            listener.onError(new ServiceCommandError("Device hasn't Launcher capabilities!"));
            return;
        }

        launcher.getAppState(appID, listener);
    }

    private void getApplist(Device device, Launcher.AppListListener listener) {
        if (!(device instanceof TvDevice)) {
            listener.onError(new ServiceCommandError("Wrong device type!"));
            return;
        }

        ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        Launcher launcher = connectableDevice.getLauncher();

        if (!connectableDevice.hasCapabilities(requiredCapabilities) || launcher == null) {
            listener.onError(new ServiceCommandError("Device hasn't Launcher capabilities!"));
            return;
        }

        launcher.getAppList(listener);
    }

    //  </editor-fold>


}
