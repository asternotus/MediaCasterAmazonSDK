package com.connectsdk.discovery.provider.firetv;

import android.content.Context;
import com.mega.cast.utils.log.SmartLog;

import com.amazon.whisperplay.install.InstallDiscoveryController;
import com.amazon.whisperplay.install.RemoteInstallService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Dmitry on 20.10.16.
 */

public class FireTVApplicationProvider {

    private static final String LOG_TAG = FireTVApplicationProvider.class.getSimpleName();

    private InstallDiscoveryController installDiscoveryController;
    public List<RemoteInstallService> installServices;
    private boolean started = false;

    public FireTVApplicationProvider(Context context) {
        this(new InstallDiscoveryController(context));
    }

    public FireTVApplicationProvider(InstallDiscoveryController installDiscoveryController) {
        this.installDiscoveryController = installDiscoveryController;
    }

    public void start(final InstallDiscoveryController.IInstallDiscoveryListener listener) {
        SmartLog.d(LOG_TAG, "Start install service discovery...");

        installServices = new ArrayList<>();

        started = true;

        installDiscoveryController.start(new InstallDiscoveryController.IInstallDiscoveryListener() {

            @Override
            public void installServiceDiscovered(RemoteInstallService remoteInstallService) {
                if (remoteInstallService != null) {
                    SmartLog.d(LOG_TAG, "installServiceDiscovered " + remoteInstallService.getName());
                    if (!installServices.contains(remoteInstallService)) {
                        installServices.add(remoteInstallService);
                    }
                    if (listener != null) {
                        listener.installServiceDiscovered(remoteInstallService);
                    }
                }
            }

            @Override
            public void installServiceLost(RemoteInstallService remoteInstallService) {
                SmartLog.d(LOG_TAG, "installServiceLost " + remoteInstallService);
                installServices.remove(remoteInstallService);
                if (listener != null) {
                    listener.installServiceLost(remoteInstallService);
                }
            }

            @Override
            public void discoveryFailure() {
                SmartLog.d(LOG_TAG, "discoveryFailure ");
                installServices = null;
                if (listener != null) {
                    listener.discoveryFailure();
                }
            }
        });
    }

    public void stop() {
        SmartLog.d(LOG_TAG, "stop ");
        if (started) {
            try {
                SmartLog.d(LOG_TAG, "try to stop install service discovery...");
                installDiscoveryController.stop();
                installServices = null;
            } catch (Exception ex) {
                SmartLog.e(LOG_TAG, "Stop exception " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void installApplication(final String asin, final String appId, final InstallApplicationListener installApplicationListener) {
        SmartLog.d(LOG_TAG, "installApplication " + asin);
        if (installServices != null && !installServices.isEmpty()) {
            SmartLog.d(LOG_TAG, "list install services...");
            for (RemoteInstallService installService : installServices) {
                installAppByAsin(installService, asin, installApplicationListener);
            }
        } else {
            SmartLog.d(LOG_TAG, "Could not find any install services! ");
            stop();
            start(new InstallDiscoveryController.IInstallDiscoveryListener() {
                @Override
                public void installServiceDiscovered(RemoteInstallService remoteInstallService) {
                    installAppByAsin(remoteInstallService, asin, installApplicationListener);
                }

                @Override
                public void installServiceLost(RemoteInstallService remoteInstallService) {

                }

                @Override
                public void discoveryFailure() {
                    installApplicationListener.onFailure(new Exception("Discovery failure!"));
                }
            });
        }
    }

    public void installApplicationChecked(final RemoteInstallService remoteInstallService, final String asin, String appId, final InstallApplicationListener installApplicationListener) {
        SmartLog.d(LOG_TAG, "Install application " + remoteInstallService.getName() + " asin = " + asin + " appID = " + appId);

        checkInstalledPackageVersion(remoteInstallService, appId,
                new RemoteInstallService.FutureListener<String>() {
                    @Override
                    public void futureIsNow(Future<String> future) {
                        boolean installed = false;
                        try {
                            String version = future.get();
                            if (version.equals(RemoteInstallService.PACKAGE_NOT_INSTALLED)) {
                                SmartLog.i("LOG", "package not installed");
                            } else {
                                installed = true;
                                SmartLog.i("LOG", "version = " + version);
                            }
                        } catch (InterruptedException e) {
                            SmartLog.e("LOG", "InterruptedException", e);
                        } catch (ExecutionException e) {
                            SmartLog.e("LOG", "ExecutionException", e);
                        }
                        if (!installed) {
                            installAppByAsin(remoteInstallService, asin, installApplicationListener);
                        }
                    }
                });
    }

    private void installAppByAsin(final RemoteInstallService remoteInstallService, String asin, final InstallApplicationListener installApplicationListener) {
        SmartLog.d(LOG_TAG, "installApp on " + remoteInstallService.getName());
        remoteInstallService.installByASIN(asin)
                .getAsync(new RemoteInstallService.FutureListener<Void>() {
                    @Override
                    public void futureIsNow(Future<Void> future) {
                        try {
                            future.get();
                            SmartLog.d(LOG_TAG, "Application installed on " + remoteInstallService.getName());
                            installApplicationListener.onInstall(remoteInstallService.getName());
                        } catch (Exception e) {
                            SmartLog.e("LOG", "InterruptedException", e);
                            installApplicationListener.onFailure(e);
                        }
                    }
                });
    }

    private void checkInstalledPackageVersion(final RemoteInstallService remoteInstallService, String appId, RemoteInstallService.FutureListener<String> listener) {
        remoteInstallService.getInstalledPackageVersion(appId).getAsync(listener);
    }

    public interface InstallApplicationListener {
        void onInstall(String deviceName);

        void onFailure(Exception e);
    }


}
