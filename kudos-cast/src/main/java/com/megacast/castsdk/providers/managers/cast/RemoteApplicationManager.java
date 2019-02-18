package com.megacast.castsdk.providers.managers.cast;

import android.support.annotation.Nullable;

import com.megacast.castsdk.model.ApplicationSession;
import com.megacast.castsdk.model.ApplicationState;
import com.megacast.castsdk.model.Device;

import java.util.List;

import rx.Observable;

/**
 * Created by Dmitry on 16.09.16.
 */
public interface RemoteApplicationManager {

    Observable<Void> showTVProgressBar(Device device);

    Observable<Void> hideTVProgressBar(Device device);

    Observable<ApplicationSession> launchApplication(Device device, String appID, @Nullable Object params);

    Observable<ApplicationState> getApplicationState(final Device device, final ApplicationSession applicationSession);

    Observable<ApplicationState> getApplicationState(final Device device, final String appID);

    Observable<Void> closeApplication(Device device, String receiverAppId);

    Observable<List<String>> getAppList(Device device);

    Observable<Object> installApplication(Device device, String receiverAppId);
}
