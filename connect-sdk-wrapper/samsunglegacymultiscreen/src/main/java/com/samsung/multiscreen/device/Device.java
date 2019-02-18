//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device;

import com.samsung.multiscreen.application.Application;
import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.application.requests.GetApplicationRequest;
import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.ChannelAsyncResult;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.requests.FindDeviceByCodeRequest;
import com.samsung.multiscreen.device.requests.FindLocalDialDevicesRequest;
import com.samsung.multiscreen.device.requests.GetDeviceRequest;
import com.samsung.multiscreen.device.requests.HidePinCodeRequest;
import com.samsung.multiscreen.device.requests.PollConnectedHostRequest;
import com.samsung.multiscreen.device.requests.ShowPinCodeRequest;
import com.samsung.multiscreen.impl.Service;
import com.samsung.multiscreen.net.dial.DialClient;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Device {
    private static final Logger LOG = Logger.getLogger(Device.class.getName());
    protected static final String CLOUD_SERVICE_URL = "https://multiscreen.samsung.com";
    protected static final String CLOUD_DISCOVERY_URL = "https://multiscreen.samsung.com/discovery/reservations";
    protected static final String ATTRIB_DEVICE_NAME = "DeviceName";
    protected static final String ATTRIB_ID = "UDN";
    protected static final String ATTRIB_IPADDRESS = "IP";
    protected static final String ATTRIB_NETWORKTYPE = "NetworkType";
    protected static final String ATTRIB_SSID = "SSID";
    protected static final String ATTRIB_SERVICEURI = "ServiceURI";
    protected static final String ATTRIB_DIALURI = "DialURI";
    protected static final String DEVICE_CAPABILITY_VERSION = "samsung:multiscreen:1";
    private Map<String, String> attributesMap;
    private Channel channel;

    protected Device(Map<String, String> attributes) {
        this.attributesMap = attributes;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[Device]").append("\nid: ").append(this.getId()).append("\nname: ").append(this.getName()).append("\nnetworkType: ").append(this.getNetworkType()).append("\nssid: ").append(this.getSSID()).append("\nipAddress: ").append(this.getIPAddress()).append("\nserviceURI: ").append(this.getServiceURI());
        return builder.toString();
    }

    public String getAttribute(String key) {
        return this.attributesMap != null?(String)this.attributesMap.get(key):"";
    }

    public String getId() {
        return this.getAttribute("UDN");
    }

    public String getName() {
        return this.getAttribute("DeviceName");
    }

    public String getNetworkType() {
        return this.getAttribute("NetworkType");
    }

    public String getSSID() {
        return this.getAttribute("SSID");
    }

    public String getIPAddress() {
        return this.getAttribute("IP");
    }

    public URI getServiceURI() {
        String serviceURIStr = this.getAttribute("ServiceURI");
        return serviceURIStr == null?URI.create(""):URI.create(serviceURIStr);
    }

    public void showPinCode(DeviceAsyncResult<Boolean> callback) {
        ShowPinCodeRequest showPinCodeRequest = new ShowPinCodeRequest(this.getServiceURI(), callback);
        Service.getInstance().getExecutorService().execute(showPinCodeRequest);
    }

    public void hidePinCode(DeviceAsyncResult<Boolean> callback) {
        HidePinCodeRequest hidePinCodeRequest = new HidePinCodeRequest(this.getServiceURI(), callback);
        Service.getInstance().getExecutorService().execute(hidePinCodeRequest);
    }

    public static void search(DeviceAsyncResult<List<Device>> callback) {
        FindLocalDialDevicesRequest findLocalDialDevicesRequest = new FindLocalDialDevicesRequest(3000, "samsung:multiscreen:1", callback);
        Service.getInstance().getExecutorService().execute(findLocalDialDevicesRequest);
    }

    public static void getByCode(String pinCode, DeviceAsyncResult<Device> callback) {
        URI pinCodeURI = URI.create("https://multiscreen.samsung.com/discovery/reservations?code=" + pinCode);
        FindDeviceByCodeRequest findDeviceByCodeRequest = new FindDeviceByCodeRequest(pinCodeURI, "samsung:multiscreen:1", callback);
        Service.getInstance().getExecutorService().execute(findDeviceByCodeRequest);
    }

    public void getApplication(String runTitle, DeviceAsyncResult<Application> callback) {
        DialClient dialClient = new DialClient(this.getDialURI().toString());
        GetApplicationRequest getApplicationRequest = new GetApplicationRequest(runTitle, this.getDialURI(), this, dialClient, callback);
        Service.getInstance().getExecutorService().execute(getApplicationRequest);
    }

    public static void getDevice(URI serviceUri, DeviceAsyncResult<Device> callback) {
        GetDeviceRequest req = new GetDeviceRequest(serviceUri, callback);
        Service.getInstance().getExecutorService().execute(req);
    }

    public void connectToChannel(String channelId, DeviceAsyncResult<Channel> callback) {
        this.connectToChannel(channelId, (Map)null, callback);
    }

    public void connectToChannel(String channelId, final Map<String, String> clientAttributes, final DeviceAsyncResult<Channel> callback) {
        this.getHostConnectedChannel(channelId, new DeviceAsyncResult<Channel>() {
            public void onResult(final Channel channel) {
                Device.LOG.info("connectToChannel() getChannel() onResult() channel:\n" + channel);
                ChannelAsyncResult connectChannelCallback = new ChannelAsyncResult<Boolean>() {
                    public void onResult(Boolean result) {
                        if(result.booleanValue()) {
                            callback.onResult(channel);
                        } else {
                            callback.onError(new DeviceError("Unknown Channel Connection failure"));
                        }

                    }

                    public void onError(ChannelError error) {
                        callback.onError(new DeviceError(error.getMessage()));
                    }
                };
                if(clientAttributes == null) {
                    channel.connect(connectChannelCallback);
                } else {
                    channel.connect(clientAttributes, connectChannelCallback);
                }

            }

            public void onError(DeviceError error) {
                callback.onError(error);
            }
        });
    }

    protected void getHostConnectedChannel(String id, DeviceAsyncResult<Channel> callback) {
        byte attempts = 5;
        short delayMilliseconds = 2000;
        PollConnectedHostRequest req = new PollConnectedHostRequest(this.getServiceURI(), id, attempts, delayMilliseconds, callback);
        Service.getInstance().getExecutorService().execute(req);
    }

    protected URI getDialURI() {
        String dialURIStr = this.getAttribute("DialURI");
        return dialURIStr == null?URI.create(""):URI.create(dialURIStr);
    }

    public static void main(String[] args) {
        getDevice(URI.create("http://192.168.43.194:8001/ms/1.0/"), new DeviceAsyncResult<Device>() {
            public void onResult(Device device) {
                device.getApplication("ChatDemo", new DeviceAsyncResult<Application>() {
                    public void onResult(Application application) {
                        application.launch(new ApplicationAsyncResult<Boolean>() {
                            public void onResult(Boolean result) {
                                Device.LOG.info("test application.launch() result: " + result);
                            }

                            public void onError(ApplicationError e) {
                                Device.LOG.info("test application.launch() error: " + e);
                            }
                        });
                    }

                    public void onError(DeviceError e) {
                    }
                });
            }

            public void onError(DeviceError error) {
            }
        });
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
