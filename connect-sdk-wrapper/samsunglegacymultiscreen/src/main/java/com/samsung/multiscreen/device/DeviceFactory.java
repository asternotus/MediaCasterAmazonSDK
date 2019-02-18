//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.net.http.client.Response;
import com.samsung.multiscreen.net.json.JSONUtil;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceFactory {
    private static final Logger LOG = Logger.getLogger(DeviceFactory.class.getName());

    DeviceFactory() {
    }

    public static Device parseDevice(Response response) {
        try {
            String e = new String(response.body, "UTF-8");
            Map jsonMap = JSONUtil.parse(e);
            return jsonMap.size() > 0?createWithMap(jsonMap):null;
        } catch (UnsupportedEncodingException var3) {
            return null;
        }
    }

    public static Device parseDeviceWithCapability(Response response, String targetCapability) {
        Object device = null;

        try {
            String e = new String(response.body, "UTF-8");
            Map jsonMap = JSONUtil.parse(e);
            if(jsonMap.size() > 0) {
                ArrayList capabilities = (ArrayList)jsonMap.get("Capabilities");
                if(capabilities != null) {
                    for(int i = 0; i < capabilities.size(); ++i) {
                        Map capsObj = (Map)capabilities.get(i);
                        String capName = (String)capsObj.get("name");
                        LOG.info("Cloud device capability: " + capName);
                        if(capName != null && capName.equalsIgnoreCase(targetCapability)) {
                            String port = (String)capsObj.get("port");
                            String location = (String)capsObj.get("location");
                            String ip = (String)jsonMap.get("IP");
                            String serviceURI = "http://" + ip + ":" + port + location;
                            jsonMap.put("ServiceURI", serviceURI);
                            return createWithMap(jsonMap);
                        }
                    }
                }
            }
        } catch (Exception var13) {
            device = null;
        }

        return (Device)device;
    }

    public static Device parseDeviceWithCapability(String responseBody, String targetCapability) {
        Map jsonMap = JSONUtil.parse(responseBody);

        try {
            if(jsonMap.size() > 0) {
                ArrayList e = (ArrayList)jsonMap.get("Capabilities");
                if(e != null) {
                    for(int i = 0; i < e.size(); ++i) {
                        Map capsObj = (Map)e.get(i);
                        String capName = (String)capsObj.get("name");
                        LOG.info("Cloud device capability: " + capName);
                        if(capName != null && capName.equalsIgnoreCase(targetCapability)) {
                            String port = (String)capsObj.get("port");
                            String location = (String)capsObj.get("location");
                            String ip = (String)jsonMap.get("IP");
                            String serviceURI = "http://" + ip + ":" + port + location;
                            jsonMap.put("ServiceURI", serviceURI);
                            return createWithMap(jsonMap);
                        }
                    }
                }
            }
        } catch (Exception var11) {
            ;
        }

        return null;
    }

    public static List<Device> parseDeviceList(Response response) {
        try {
            String e = new String(response.body, "UTF-8");
            List jsonCloudDeviceList = JSONUtil.parseList(e);
            LOG.info("parseDeviceList() jsonCloudDeviceList: " + jsonCloudDeviceList.size());
            ArrayList cloudDeviceList = new ArrayList();
            Iterator i$ = jsonCloudDeviceList.iterator();

            while(i$.hasNext()) {
                Map jsonCloudDevice = (Map)i$.next();
                Device cloudDevice = jsonCloudDevice.size() > 0?createWithMap(jsonCloudDevice):null;
                if(cloudDevice != null && !cloudDeviceList.contains(cloudDevice)) {
                    cloudDeviceList.add(cloudDevice);
                }
            }

            return cloudDeviceList;
        } catch (UnsupportedEncodingException var7) {
            return null;
        }
    }

    public static Device createWithMapAndAppURI(Map<String, Object> map, URI applicationURI) {
        if(map != null && applicationURI != null) {
            map.put("DialURI", applicationURI.toString());
        }

        return createWithMap(map);
    }

    public static Device createWithMap(Map<String, Object> map) {
        if(map != null) {
            HashMap attribMap = new HashMap();
            Iterator i$ = map.entrySet().iterator();

            while(i$.hasNext()) {
                Entry entry = (Entry)i$.next();
                if(entry.getValue() instanceof String) {
                    String val = (String)entry.getValue();
                    attribMap.put(entry.getKey(), val);
                }
            }

            return new Device(attribMap);
        } else {
            return null;
        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
