//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class NetUtil {
    private static final String TAG = "NetUtil";

    public NetUtil() {
    }

    public static InetAddress getDeviceIpAddress(Context context) {
        String ip = getWifiIpAddress(context);
        InetAddress address = null;

        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException var4) {
            var4.printStackTrace();
        }

        return address;
    }

    public static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService("wifi");
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf((long)ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException var6) {
            var6.printStackTrace();
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public static MulticastLock acquireMulticastLock(Context context, String tag) {
        WifiManager wifi = (WifiManager)context.getSystemService("wifi");
        MulticastLock multicastLock = wifi.createMulticastLock(tag);
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
        return multicastLock;
    }

    public static void releaseMulticastLock(MulticastLock multicastLock) {
        if(multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }

    }
}
