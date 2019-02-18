//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkRequest.Builder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION;

import com.mega.cast.utils.log.SmartLog;

import java.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

class StandbyDeviceList {
    private static final String LOG_TAG = StandbyDeviceList.class.getSimpleName();
    private static String TAG = "StndbyDLHndlr";
    private static String DEVICE_LIST_FILE_NAME = "com.samsung.smartviewSDK.standbydevices";
    private static String KEY_STANDBYLIST = "STANDBYLIST_KEY";
    private static final int TIMEOUT = 7000;
    private static final int SUPPORTED_TV_MODEL_YEAR = 16;
    private static String VALUE_DUID = "id";
    private static String VALUE_MAC = "mac";
    private static String VALUE_URI = "uri";
    private static String VALUE_NAME = "name";
    private static String VALUE_BSSID = "ssid";
    private static StandbyDeviceList mInstance = null;
    private List<StandbyDeviceList.StandbyDevice> mList;
    private StandbyDeviceList.NetworkMonitor mNetworkMonitor;
    private SharedPreferences mLocalStorage;
    private Search.SearchListener mSearchListener;
    private Boolean mShowStandbyDevicesTimerExpired;

    static StandbyDeviceList create(Context applicationContext, Search.SearchListener searchListener) {
        if (mInstance == null) {
            mInstance = new StandbyDeviceList(applicationContext, searchListener);
        }

        return mInstance;
    }

    void destruct() {
        if (mInstance != null) {
            mInstance = null;
            this.mList.clear();
            this.mNetworkMonitor.stopNetworkMonitoring();
            this.mLocalStorage = null;
            this.mSearchListener = null;
        }
    }

    static StandbyDeviceList getInstance() {
        return mInstance;
    }

    void start() {
        Timer timer = new Timer("showStandbyTVTimer", true);
        TimerTask task = new TimerTask() {
            public void run() {
                StandbyDeviceList.this.mShowStandbyDevicesTimerExpired = Boolean.valueOf(true);
                List standbyServices = StandbyDeviceList.this.get();

                for (int i = 0; i < standbyServices.size(); ++i) {
                    Service standbyService = (Service) standbyServices.get(i);
                    if (standbyService != null) {
                        StandbyDeviceList.this.mSearchListener.onFound(standbyService);
                    }
                }

            }
        };
        timer.schedule(task, 7000L);
    }

    void stop() {
        this.mShowStandbyDevicesTimerExpired = Boolean.valueOf(false);

        for (int i = 0; i < this.mList.size(); ++i) {
            ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).isActive = Boolean.valueOf(false);
        }

    }

    Service get(String duid) {
        try {
            for (int e = 0; e < this.mList.size(); ++e) {
                StandbyDeviceList.StandbyDevice device = (StandbyDeviceList.StandbyDevice) this.mList.get(e);
                if (device.duid.trim().equals(duid.trim())) {
                    JSONObject record = new JSONObject();
                    record.put(VALUE_DUID, device.duid);
                    record.put(VALUE_URI, device.ip);
                    record.put(VALUE_NAME, device.name);
                    return Service.create(record);
                }
            }
        } catch (Exception var5) {
            SmartLog.e(TAG, "get(Duid): Error: " + var5.getMessage());
        }

        return null;
    }

    void update(final Service service, final Boolean isActive) {
        if (service.getType().trim().equals("Samsung SmartTV")) {
            service.getDeviceInfo(new Result<Device>() {
                public void onSuccess(Device device) {
                    StandbyDeviceList.this.remove(service.getId());
                    String strModel = device.getModel().substring(0, 2);

                    int modelYr;
                    try {
                        modelYr = Integer.parseInt(strModel);
                    } catch (NumberFormatException var5) {
                        modelYr = 0;
                    }

                    if (modelYr >= 16) {
                        StandbyDeviceList.StandbyDevice standbyDevice = StandbyDeviceList.this.new StandbyDevice(service.getId(), StandbyDeviceList.this.mNetworkMonitor.getCurrentBSSID(), device.getWifiMac(), service.getUri().toString(), device.getName(), isActive);
                        StandbyDeviceList.this.mList.add(standbyDevice);
                        StandbyDeviceList.this.commit();
                    }

                }

                public void onError(Error error) {
                    for (int i = 0; i < StandbyDeviceList.this.mList.size(); ++i) {
                        if (((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).duid.trim().equals(service.getId().trim())) {
                            ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).isActive = Boolean.valueOf(false);
                            break;
                        }
                    }

                }
            });
        }
    }

    void remove(Service service) {
        if (service.isStandbyService.booleanValue() && this.remove(service.getId()).booleanValue()) {
            this.mSearchListener.onLost(service);
            this.commit();
        }

    }

    void clear() {
        (new Runnable() {
            public void run() {
                if (StandbyDeviceList.this.mList != null && StandbyDeviceList.this.mList.size() > 0) {
                    for (int i = 0; i < StandbyDeviceList.this.mList.size(); ++i) {
                        JSONObject record = new JSONObject();

                        try {
                            record.put(StandbyDeviceList.VALUE_DUID, ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).duid);
                            record.put(StandbyDeviceList.VALUE_BSSID, ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).bssid);
                            record.put(StandbyDeviceList.VALUE_MAC, ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).mac);
                            record.put(StandbyDeviceList.VALUE_URI, ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).ip);
                            record.put(StandbyDeviceList.VALUE_NAME, ((StandbyDeviceList.StandbyDevice) StandbyDeviceList.this.mList.get(i)).name);
                        } catch (Exception var4) {
                            SmartLog.e(StandbyDeviceList.TAG, "clear() Unsuccessful: error : " + var4.getMessage());
                            return;
                        }

                        StandbyDeviceList.this.mSearchListener.onLost(Service.create(record));
                        StandbyDeviceList.this.mList.remove(i);
                    }

                    StandbyDeviceList.this.commit();
                }

            }
        }).run();
    }

    Service getLostStandbyService(Service service) {
        if (!service.isStandbyService.booleanValue() && this.isStandbyDevice(service.getId()).booleanValue()) {
            this.update(service, Boolean.valueOf(true));
            if (this.mShowStandbyDevicesTimerExpired.booleanValue()) {
                return this.get(service.getId());
            }
        }

        return null;
    }

    Service getFoundStandbyService(Service service) {
        if (!service.isStandbyService.booleanValue() && this.isStandbyDevice(service.getId()).booleanValue()) {
            this.update(service, Boolean.valueOf(false));
            return this.get(service.getId());
        } else {
            return null;
        }
    }

    String getMac(Service service) {
        for (int i = 0; i < this.mList.size(); ++i) {
            if (service.getId().trim().equals(((StandbyDeviceList.StandbyDevice) this.mList.get(i)).duid.trim())) {
                return ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).mac;
            }
        }

        return null;
    }

    private StandbyDeviceList(Context appContext, Search.SearchListener searchListener) {
        this.mLocalStorage = appContext.getSharedPreferences(DEVICE_LIST_FILE_NAME, 0);
        this.mShowStandbyDevicesTimerExpired = Boolean.valueOf(false);
        this.mList = new ArrayList();
        String stringDeviceList = this.mLocalStorage.getString(KEY_STANDBYLIST, (String) null);
        JSONArray jsonDeviceList;
        if (stringDeviceList != null && !stringDeviceList.equals("[]")) {
            try {
                jsonDeviceList = new JSONArray(stringDeviceList);
            } catch (Exception var8) {
                SmartLog.e(TAG, "StandbyDeviceListHandler: Error: " + var8.getMessage());
                return;
            }
        } else {
            jsonDeviceList = new JSONArray();
        }

        try {
            if (jsonDeviceList.length() > 0) {
                for (int e = 0; e < jsonDeviceList.length(); ++e) {
                    JSONObject record = (JSONObject) jsonDeviceList.get(e);
                    StandbyDeviceList.StandbyDevice standbyDevice = new StandbyDeviceList.StandbyDevice(record.getString(VALUE_DUID), record.getString(VALUE_BSSID), record.getString(VALUE_MAC), record.getString(VALUE_URI), record.getString(VALUE_NAME), Boolean.valueOf(false));
                    this.mList.add(standbyDevice);
                }
            }
        } catch (Exception var9) {
            SmartLog.e(TAG, "StandbyDeviceListHandler: Error: " + var9.getMessage());
            return;
        }

        this.mNetworkMonitor = new StandbyDeviceList.NetworkMonitor(appContext, searchListener);
    }

    private StandbyDeviceList() {
    }

    private synchronized void commit() {
        synchronized (this) {
            JSONArray deviceList = new JSONArray();

            for (int i = 0; i < this.mList.size(); ++i) {
                JSONObject record = new JSONObject();

                try {
                    record.put(VALUE_DUID, ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).duid);
                    record.put(VALUE_BSSID, ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).bssid);
                    record.put(VALUE_MAC, ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).mac);
                    record.put(VALUE_URI, ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).ip);
                    record.put(VALUE_NAME, ((StandbyDeviceList.StandbyDevice) this.mList.get(i)).name);
                } catch (Exception var7) {
                    SmartLog.e(TAG, "close(): Error: " + var7.getMessage());
                    return;
                }

                deviceList.put(record);
                Editor editor = this.mLocalStorage.edit();
                editor.putString(KEY_STANDBYLIST, deviceList.toString());
                editor.apply();
            }

        }
    }

    private Boolean remove(String duid) {
        for (int i = 0; i < this.mList.size(); ++i) {
            if (((StandbyDeviceList.StandbyDevice) this.mList.get(i)).duid.trim().equals(duid.trim())) {
                this.mList.remove(i);
                return Boolean.valueOf(true);
            }
        }

        return Boolean.valueOf(false);
    }

    private List<Service> get() {
        ArrayList list = new ArrayList();

        try {
            for (int e = 0; e < this.mList.size(); ++e) {
                StandbyDeviceList.StandbyDevice device = (StandbyDeviceList.StandbyDevice) this.mList.get(e);
                if (!device.isActive.booleanValue() && this.mNetworkMonitor.getCurrentBSSID().equals(device.bssid.trim())) {
                    JSONObject record = new JSONObject();
                    record.put(VALUE_DUID, device.duid);
                    record.put(VALUE_URI, device.ip);
                    record.put(VALUE_NAME, device.name);
                    Service standbyService = Service.create(record);
                    list.add(standbyService);
                }
            }
        } catch (Exception var6) {
            SmartLog.e(TAG, "get(): Error: " + var6.getMessage());
        }

        return list;
    }

    private Boolean isStandbyDevice(String duid) {
        for (int i = 0; i < this.mList.size(); ++i) {
            if (((StandbyDeviceList.StandbyDevice) this.mList.get(i)).duid.trim().equals(duid.trim()) && this.mNetworkMonitor.getCurrentBSSID().equals(((StandbyDeviceList.StandbyDevice) this.mList.get(i)).bssid.trim())) {
                return Boolean.valueOf(true);
            }
        }

        return Boolean.valueOf(false);
    }

    private class NetworkMonitor {
        private String mCurrentBSSID;
        private NetworkInfo mNetworkInfo;
        private final ConnectivityManager mConnectivityManager;
        private NetworkCallback mNetworkCallback;

        NetworkMonitor(Context appContext, Search.SearchListener searchListener) {
            StandbyDeviceList.this.mSearchListener = searchListener;
            this.mConnectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            this.mNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
            if (this.mNetworkInfo != null && this.mNetworkInfo.isConnected()) {
                WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                this.mCurrentBSSID = wifiInfo.getBSSID();
            } else {
                this.mCurrentBSSID = "";
            }

            this.registerNetworkChangeCallback(appContext);
        }

        String getCurrentBSSID() {
            return this.mCurrentBSSID;
        }

        private void registerNetworkChangeCallback(final Context context) {
            SmartLog.d(LOG_TAG, "registerNetworkChangeCallback");
            (new Thread(new Runnable() {
                public void run() {
                    if (VERSION.SDK_INT >= 21) {
                        NetworkMonitor.this.mNetworkCallback = new NetworkCallback() {
                            public void onAvailable(Network network) {
                                SmartLog.d(LOG_TAG, "onNetwork Available");
                                super.onAvailable(network);
                                NetworkMonitor.this.mNetworkInfo = NetworkMonitor.this.mConnectivityManager.getActiveNetworkInfo();
                                if (NetworkMonitor.this.mNetworkInfo != null && NetworkMonitor.this.mNetworkInfo.isConnected()) {
                                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                    NetworkMonitor.this.mCurrentBSSID = wifiInfo.getBSSID();
                                    if (StandbyDeviceList.this.mShowStandbyDevicesTimerExpired.booleanValue()) {
                                        List lostStandbyServices = StandbyDeviceList.this.get();

                                        for (int i = 0; i < lostStandbyServices.size(); ++i) {
                                            StandbyDeviceList.this.mSearchListener.onFound((Service) lostStandbyServices.get(i));
                                        }
                                    }
                                } else {
                                    NetworkMonitor.this.mCurrentBSSID = "";
                                }

                            }

                            public void onLost(Network network) {
                                SmartLog.d(LOG_TAG, "onNetwork onLost");
                                super.onLost(network);
                                List lostStandbyServices = StandbyDeviceList.this.get();

                                for (int i = 0; i < lostStandbyServices.size(); ++i) {
                                    StandbyDeviceList.this.mSearchListener.onLost((Service) lostStandbyServices.get(i));
                                }

                                NetworkMonitor.this.mCurrentBSSID = "";
                            }
                        };
                        NetworkMonitor.this.mConnectivityManager.registerNetworkCallback((new Builder()).build(), NetworkMonitor.this.mNetworkCallback);
                    }

                }
            })).run();
        }

        void stopNetworkMonitoring() {
            if (this.mNetworkCallback != null) {
                this.mNetworkCallback = null;
            }

        }
    }

    private class StandbyDevice {
        String duid;
        String bssid;
        String mac;
        String ip;
        String name;
        Boolean isActive;

        StandbyDevice(String duid, String bssid, String mac, String ip, String name, Boolean isActive) {
            this.duid = duid;
            this.bssid = bssid;
            this.mac = mac;
            this.ip = ip;
            this.name = name;
            this.isActive = isActive;
        }
    }
}
