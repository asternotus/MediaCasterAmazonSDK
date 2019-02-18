//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import com.mega.cast.utils.log.SmartLog;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.samsung.multiscreenfix.util.HttpUtil;
import com.samsung.multiscreenfix.util.JSONUtil;
import com.samsung.multiscreenfix.util.RunUtil;
import com.samsung.multiscreenfix.util.HttpUtil.ResultCreator;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jmdns.ServiceInfo;
import org.json.JSONObject;

public class Service {
    private static final String TAG = "Service";
    private static final String ID_PROPERTY = "id";
    private static final String VERSION_PROPERTY = "ve";
    private static final String NAME_PROPERTY = "fn";
    private static final String TYPE_PROPERTY = "md";
    private static final String ENDPOINT_PROPERTY = "se";
    private static final String PROPERTY_DEVICE = "device";
    private static final String PROPERTY_DUID = "duid";
    private static final String PROPERTY_VERSION = "version";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_TYPE = "type";
    private static final String PROPERTY_URI = "uri";
    private static final String PROPERTY_ISSUPPORT = "isSupport";
    public static final String SUPPORT_DMP = "DMP_available";
    public static final String TYPE_SMART_TV = "Samsung SmartTV";
    public static final String TYPE_SPEAKER = "Samsung Speaker";
    private static final int DEFAULT_WOW_TIMEOUT_VALUE = 60000;
    private static final int TV_YEAR_SSL_SUPPORT = 15;
    private static final int TV_YEAR_DMP_SUPPORT = 15;
    private static boolean isWoWAndConnectStarted = false;
    private Service.SecureModeState isSecureModeSupported;
    private final String id;
    private final String version;
    private final String name;
    private final String type;
    private final Map<String, Object> isSupport;
    private final Uri uri;
    final Boolean isStandbyService;

    protected Service(Service service) {
        this.isSecureModeSupported = Service.SecureModeState.Unknown;
        this.id = service.id;
        this.version = service.version;
        this.name = service.name;
        this.type = service.type;
        this.isSupport = new HashMap(service.isSupport);
        this.uri = Uri.parse(service.uri.toString());
        this.isStandbyService = Boolean.valueOf(false);
        this.isSecureModeSupported = service.isSecureModeSupported;
    }

    Boolean isEqualTo(Service service) {
        return this.hashCode() != service.hashCode()?Boolean.valueOf(false):(!this.name.equals(service.name)?Boolean.valueOf(false):(this.isStandbyService != service.isStandbyService?Boolean.valueOf(false):(!this.id.equals(service.id)?Boolean.valueOf(false):(!this.uri.equals(service.uri)?Boolean.valueOf(false):(!this.type.equals(service.type)?Boolean.valueOf(false):(!this.version.equals(service.version)?Boolean.valueOf(false):(!this.isSupport.equals(service.isSupport)?Boolean.valueOf(false):(this.isSecureModeSupported != service.isSecureModeSupported?Boolean.valueOf(false):Boolean.valueOf(true)))))))));
    }

    public static Search search(Context context) {
        return Search.getInstance(context);
    }

    public static void getByURI(Uri uri, Result<Service> result) {
        getByURI(uri, 30000, result);
    }

    public static void getByURI(Uri uri, int timeout, Result<Service> result) {
        StringCallback httpStringCallback = HttpHelper.createHttpCallback(new ResultCreator<Service>() {
            public Service createResult(Map<String, Object> data) {
                return Service.create(data);
            }
        }, result);
        HttpUtil.executeJSONRequest(uri, "GET", timeout, httpStringCallback);
    }

    public static void getById(Context context, String id, final Result<Service> result) {
        final ArrayList threads = new ArrayList();
        final CopyOnWriteArrayList results = new CopyOnWriteArrayList();
        Result searchResult = new Result<Service>() {
            public void onSuccess(Service service) {
                results.add(service);
                RunUtil.runInBackground(new Runnable() {
                    public void run() {
                        Iterator var1 = threads.iterator();

                        while(var1.hasNext()) {
                            ProviderThread thread = (ProviderThread)var1.next();
                            thread.terminate();
                        }

                    }
                });
            }

            public void onError(Error error) {
                results.add(error);
            }
        };
        threads.add(MDNSSearchProvider.getById(context, id, searchResult));
        ProviderThread thread = MSFDSearchProvider.getById(context, id, searchResult);
        if(thread != null) {
            threads.add(thread);
        }

        RunUtil.runInBackground(new Runnable() {
            public void run() {
                Iterator var1 = threads.iterator();

                while(var1.hasNext()) {
                    ProviderThread thread = (ProviderThread)var1.next();

                    try {
                        thread.join();
                    } catch (InterruptedException var4) {
                        var4.printStackTrace();
                    }
                }

                this.chooseResult();
            }

            private void chooseResult() {
                Error error = null;
                Iterator var3 = results.iterator();

                while(var3.hasNext()) {
                    Object obj = var3.next();
                    if(obj instanceof Service) {
                        Service service = (Service)obj;
                        result.onSuccess(service);
                        return;
                    }

                    if(error == null && obj instanceof Error) {
                        error = (Error)obj;
                    }
                }

                if(error != null) {
                    result.onError(error);
                }

            }
        });
    }

    void isSecurityModeSupported(final Result<Boolean> result) {
        if(this.isStandbyService.booleanValue()) {
            result.onSuccess(Boolean.valueOf(true));
        } else if(this.isSecureModeSupported == Service.SecureModeState.Unknown) {
            this.getDeviceInfo(new Result<Device>() {
                public void onSuccess(Device device) {
                    if(device != null) {
                        String model = device.getModel();
                        int TVYear = 0;

                        try {
                            TVYear = Integer.parseInt(model.substring(0, 2));
                        } catch (NumberFormatException var5) {
                            Service.this.isSecureModeSupported = Service.SecureModeState.NotSupported;
                        }

                        if(TVYear >= 15) {
                            Service.this.isSecureModeSupported = Service.SecureModeState.Supported;
                        } else {
                            Service.this.isSecureModeSupported = Service.SecureModeState.NotSupported;
                        }

                        result.onSuccess(Boolean.valueOf(TVYear >= 15));
                    }

                }

                public void onError(Error error) {
                    result.onError(error);
                }
            });
        } else if(this.isSecureModeSupported == Service.SecureModeState.Supported) {
            result.onSuccess(Boolean.valueOf(true));
        } else {
            result.onSuccess(Boolean.valueOf(false));
        }

    }

    public static void WakeOnWirelessLan(String macAddr) {
        boolean SRC_PORT = true;
        boolean DST_PORT = true;
        String magicPacketId = "FF:FF:FF:FF:FF:FF";
        String broadCastAddr = "255.255.255.255";
        String wakeUpIdentifier = "SECWOW";
        String secureOn = "00:00:00:00:00:00";
        boolean wow_packet_max_size = true;
        boolean wow_packet_min_size = true;
        boolean wow_packet_sec_size = true;
        boolean wow_packet_ss_size = true;
        byte packetSizeAlloc = 102;
        byte reservedField = 0;
        byte applicationID = 0;
        int var17 = packetSizeAlloc + 6;
        var17 += 12;
        ByteBuffer wowPacketBuffer = ByteBuffer.allocate(var17);
        wowPacketBuffer.put(convertMacAddrToBytes("FF:FF:FF:FF:FF:FF"));

        for(int i = 0; i < 16; ++i) {
            wowPacketBuffer.put(convertMacAddrToBytes(macAddr));
        }

        wowPacketBuffer.put(convertMacAddrToBytes("00:00:00:00:00:00"));
        wowPacketBuffer.put("SECWOW".getBytes());
        wowPacketBuffer.putInt(reservedField);
        wowPacketBuffer.put((byte)applicationID);
        final byte[] magicPacket = wowPacketBuffer.array();
        RunUtil.runInBackground(new Runnable() {
            public void run() {
                DatagramSocket wowSocket = null;

                try {
                    wowSocket = new DatagramSocket(2014);
                    DatagramPacket wowPacket = new DatagramPacket(magicPacket, magicPacket.length);
                    wowPacket.setAddress(InetAddress.getByName("255.255.255.255"));
                    wowPacket.setPort(2014);
                    wowSocket.send(wowPacket);
                } catch (Exception var7) {
                    var7.printStackTrace();
                } finally {
                    if(wowSocket != null) {
                        wowSocket.close();
                    }

                }

            }
        });
    }

    public static void WakeOnWirelessAndConnect(String macAddr, Uri uri, Result<Service> connectCallback) {
        WakeOnWirelessAndConnect(macAddr, uri, '\uea60', connectCallback);
    }

    public static void WakeOnWirelessAndConnect(String macAddr, Uri uri, int timeout, Result<Service> connectCallback) {
        if(!isWoWAndConnectStarted) {
            isWoWAndConnectStarted = true;
            if(macAddr == null) {
                throw new NullPointerException();
            } else {
                WakeOnWirelessLan(macAddr);
                Handler timerHandler = new Handler();
                WakeUpAndConnect(uri, connectCallback);
                timerHandler.postDelayed(new Runnable() {
                    public void run() {
                        Service.isWoWAndConnectStarted = false;
                    }
                }, (long)timeout);
            }
        }
    }

    private static void WakeUpAndConnect(final Uri uri, final Result<Service> connectCallback) {
        getByURI(uri, new Result<Service>() {
            public void onError(Error error) {
                if(Service.isWoWAndConnectStarted) {
                    Service.WakeUpAndConnect(uri, connectCallback);
                } else {
                    Service.isWoWAndConnectStarted = false;
                    connectCallback.onError(error);
                }

            }

            public void onSuccess(Service service) {
                Service.isWoWAndConnectStarted = false;
                connectCallback.onSuccess(service);
            }
        });
    }

    private static byte[] convertMacAddrToBytes(String macAddr) {
        String[] macAddrAtoms = macAddr.split(":");
        byte[] macAddressBytes = new byte[6];

        for(int i = 0; i < 6; ++i) {
            Integer hex = Integer.valueOf(Integer.parseInt(macAddrAtoms[i], 16));
            macAddressBytes[i] = hex.byteValue();
        }

        return macAddressBytes;
    }

    public void getDeviceInfo(Result<Device> callback) {
        Uri uri = this.getUri();
        StringCallback httpStringCallback = HttpHelper.createHttpCallback(new ResultCreator<Device>() {
            public Device createResult(Map<String, Object> data) {
                Map deviceData = (Map)data.get("device");
                return Device.create(deviceData);
            }
        }, callback);
        HttpUtil.executeJSONRequest(uri, "GET", httpStringCallback);
    }

    public Channel createChannel(Uri uri) {
        return Channel.create(this, uri);
    }

    Application createApplication(Uri uri) {
        if(uri == null) {
            throw new NullPointerException();
        } else {
            return Application.create(this, uri);
        }
    }

    public Application createApplication(Uri uri, String channelId) {
        if(uri != null && channelId != null) {
            return Application.create(this, uri, channelId, (Map)null);
        } else {
            throw new NullPointerException();
        }
    }

    public Application createApplication(Uri uri, String channelId, Map<String, Object> startArgs) {
        if(uri != null && channelId != null && startArgs != null) {
            return Application.create(this, uri, channelId, startArgs);
        } else {
            throw new NullPointerException();
        }
    }

    Application createApplication(String id) {
        if(id == null) {
            throw new NullPointerException();
        } else {
            Uri uri = Uri.parse(id);
            return Application.create(this, uri);
        }
    }

    public Application createApplication(String id, String channelId) {
        if(id != null && channelId != null) {
            Uri uri = Uri.parse(id);
            return Application.create(this, uri, channelId, (Map)null);
        } else {
            throw new NullPointerException();
        }
    }

    public VideoPlayer createVideoPlayer(String appName) {
        if(this.id == null) {
            throw new NullPointerException();
        } else {
            Uri uri = Uri.parse(this.id);
            return new VideoPlayer(this, uri, appName);
        }
    }

    public PhotoPlayer createPhotoPlayer(String appName) {
        if(this.id == null) {
            throw new NullPointerException();
        } else {
            Uri uri = Uri.parse(this.id);
            return new PhotoPlayer(this, uri, appName);
        }
    }

    public AudioPlayer createAudioPlayer(String appName) {
        if(this.id == null) {
            throw new NullPointerException();
        } else {
            Uri uri = Uri.parse(this.id);
            return new AudioPlayer(this, uri, appName);
        }
    }

    public Application createApplication(String id, String channelId, Map<String, Object> startArgs) {
        if(id != null && channelId != null && startArgs != null) {
            Uri uri = Uri.parse(id);
            return Application.create(this, uri, channelId, startArgs);
        } else {
            throw new NullPointerException();
        }
    }

    static Service create(ServiceInfo info) {
        if(info == null) {
            throw new NullPointerException();
        } else {
            String id = info.getPropertyString("id");
            String version = info.getPropertyString("ve");
            String name = info.getPropertyString("fn");
            String type = info.getPropertyString("md");
            Map isSupport = JSONUtil.parse(info.getPropertyString("isSupport"));
            Uri endPoint = Uri.parse(info.getPropertyString("se"));
            return new Service(id, version, name, type, isSupport, endPoint, Boolean.valueOf(false));
        }
    }

    private static Service create(Map<String, Object> map) {
        if(map == null) {
            throw new NullPointerException();
        } else {
            String id = (String)map.get("id");
            String version = (String)map.get("version");
            String name = (String)map.get("name");
            String type = (String)map.get("type");
            Map isSupport = JSONUtil.parse((String)map.get("isSupport"));
            Uri endPoint = Uri.parse((String)map.get("uri"));
            return new Service(id, version, name, type, isSupport, endPoint, Boolean.valueOf(false));
        }
    }

    protected static Service create(JSONObject jsonObject) {
        if(jsonObject == null) {
            throw new NullPointerException();
        } else {
            String id = null;
            String version = "Unknown";
            String name = null;
            String type = "Samsung SmartTV";
            HashMap isSupport = new HashMap();
            Uri endPoint = null;

            try {
                id = jsonObject.getString("id");
                name = jsonObject.getString("name");
                name = name.concat("(standby)");
                endPoint = Uri.parse(jsonObject.getString("uri"));
            } catch (Exception var8) {
                SmartLog.e("Service", "create(): Error: " + var8.getMessage());
            }

            return new Service(id, version, name, type, isSupport, endPoint, Boolean.valueOf(true));
        }
    }

    public void remove() {
        if(StandbyDeviceList.getInstance() != null) {
            StandbyDeviceList.getInstance().remove(this);
        }

    }

    public void isDMPSupported(final Result<Boolean> result) {
        if(result != null) {
            if(this.isStandbyService.booleanValue()) {
                result.onSuccess(Boolean.valueOf(true));
            } else if(this.isSupport != null && !this.isSupport.isEmpty()) {
                result.onSuccess(Boolean.valueOf(this.isSupport("DMP_available")));
            } else {
                this.getDeviceInfo(new Result<Device>() {
                    public void onSuccess(Device device) {
                        if(device != null) {
                            String model = device.getModel();
                            int TVYear = 0;

                            try {
                                TVYear = Integer.parseInt(model.substring(0, 2));
                            } catch (NumberFormatException var5) {
                                result.onSuccess(Boolean.valueOf(false));
                            }

                            result.onSuccess(Boolean.valueOf(TVYear == 15));
                        }

                    }

                    public void onError(Error error) {
                        result.onError(error);
                    }
                });
            }
        }

    }

    private boolean isSupport(String type) {
        return this.isSupport.get(type) != null && this.isSupport.get(type).equals("true");
    }

    public Service.SecureModeState getIsSecureModeSupported() {
        return this.isSecureModeSupported;
    }

    public String getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Object> getIsSupport() {
        return this.isSupport;
    }

    public Uri getUri() {
        return this.uri;
    }

    public Boolean getIsStandbyService() {
        return this.isStandbyService;
    }

    public void setIsSecureModeSupported(Service.SecureModeState isSecureModeSupported) {
        this.isSecureModeSupported = isSecureModeSupported;
    }

    public String toString() {
        return "Service(isSecureModeSupported=" + this.getIsSecureModeSupported() + ", id=" + this.getId() + ", version=" + this.getVersion() + ", name=" + this.getName() + ", type=" + this.getType() + ", isSupport=" + this.getIsSupport() + ", uri=" + this.getUri() + ", isStandbyService=" + this.getIsStandbyService() + ")";
    }

    public Service(String id, String version, String name, String type, Map<String, Object> isSupport, Uri uri, Boolean isStandbyService) {
        this.isSecureModeSupported = Service.SecureModeState.Unknown;
        this.id = id;
        this.version = version;
        this.name = name;
        this.type = type;
        this.isSupport = isSupport;
        this.uri = uri;
        this.isStandbyService = isStandbyService;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof Service)) {
            return false;
        } else {
            Service other = (Service)o;
            if(!other.canEqual(this)) {
                return false;
            } else {
                String this$id = this.getId();
                String other$id = other.getId();
                if(this$id == null) {
                    if(other$id != null) {
                        return false;
                    }
                } else if(!this$id.equals(other$id)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Service;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        String $id = this.getId();
        int result1 = result * 59 + ($id == null?0:$id.hashCode());
        return result1;
    }

    private static enum SecureModeState {
        Unknown,
        NotSupported,
        Supported;

        private SecureModeState() {
        }
    }
}
