//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class NetworkUtil {
    public NetworkUtil() {
    }

    public static InetAddress getInetAddressByName(String name) {
        try {
            return InetAddress.getByName(name);
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }
    }

    public static final boolean isUsableNetworkInterface(NetworkInterface networkInterface) {
        try {
            return networkInterface.supportsMulticast();
        } catch (Exception var2) {
            return false;
        }
    }

    public static boolean isUsableAddress(InetAddress address) {
        return isIPv4Address(address.getHostAddress());
    }

    public static List<NetworkInterface> getUsableNetworkInterfaces() {
        ArrayList usableInterfaces = new ArrayList();

        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();

            while(e.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface)e.nextElement();
                if(isUsableNetworkInterface(networkInterface)) {
                    usableInterfaces.add(networkInterface);
                }
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return usableInterfaces;
    }

    public static List<InetAddress> getUsableAddresses(NetworkInterface networkInterface) {
        ArrayList usableAddresses = new ArrayList();
        Enumeration addresses = networkInterface.getInetAddresses();

        while(addresses.hasMoreElements()) {
            InetAddress address = (InetAddress)addresses.nextElement();
            if(isUsableAddress(address)) {
                usableAddresses.add(address);
            }
        }

        return usableAddresses;
    }

    public static List<InetAddress> getUsableAddresses() {
        ArrayList usableAddresses = new ArrayList();
        List networkInterfaces = getUsableNetworkInterfaces();
        Iterator i$ = networkInterfaces.iterator();

        while(i$.hasNext()) {
            NetworkInterface networkInterface = (NetworkInterface)i$.next();
            List addresses = getUsableAddresses(networkInterface);
            usableAddresses.addAll(addresses);
        }

        return usableAddresses;
    }

    public static InetAddress getBroadcastAddress() {
        try {
            List e = getUsableNetworkInterfaces();
            Iterator i$ = e.iterator();

            while(i$.hasNext()) {
                NetworkInterface networkInterface = (NetworkInterface)i$.next();
                List interfaceAddresses = networkInterface.getInterfaceAddresses();
                Iterator i$1 = interfaceAddresses.iterator();

                while(i$1.hasNext()) {
                    InterfaceAddress interfaceAddress = (InterfaceAddress)i$1.next();
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if(broadcast != null) {
                        return broadcast;
                    }
                }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return null;
    }

    public static boolean isIPv6Address(String host) {
        try {
            InetAddress ignored = InetAddress.getByName(host);
            return ignored instanceof Inet6Address;
        } catch (Exception var2) {
            return false;
        }
    }

    public static boolean isIPv4Address(String host) {
        try {
            InetAddress ignored = InetAddress.getByName(host);
            return ignored instanceof Inet4Address;
        } catch (Exception var2) {
            return false;
        }
    }

    public static InetAddress getLocalAddress() {
        List networkInterfaces = getUsableNetworkInterfaces();
        Iterator i$ = networkInterfaces.iterator();

        while(i$.hasNext()) {
            NetworkInterface networkInterface = (NetworkInterface)i$.next();
            List addresses = getUsableAddresses(networkInterface);
            Iterator i$1 = addresses.iterator();

            while(i$1.hasNext()) {
                InetAddress address = (InetAddress)i$1.next();
                if(isIPv4Address(address.getHostName())) {
                    return address;
                }
            }
        }

        return null;
    }

    public static InetAddress getLocalHost() {
        InetAddress address = null;

        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException var2) {
            ;
        }

        return address;
    }

    public static String getLocalHostName() {
        String name = "";
        InetAddress address = getLocalAddress();
        if(address != null) {
            name = address.getHostName();
        }

        return name;
    }

    public static String getHardwareAddress() {
        String hardwareAddress = null;

        try {
            InetAddress e = getLocalAddress();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(e);
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < mac.length; ++i) {
                sb.append(String.format("%02X%s", new Object[]{Byte.valueOf(mac[i]), i < mac.length - 1?"-":""}));
            }

            hardwareAddress = sb.toString();
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return hardwareAddress;
    }

    public static String getHostURL(String host, int port, String uri) {
        String hostAddr = host;
        if(isIPv6Address(host)) {
            hostAddr = "[" + host + "]";
        }

        return "http://" + hostAddr + ":" + Integer.toString(port) + uri;
    }

    public static String printNetworkAddresses(boolean onlyShowUsable) {
        StringBuilder result = new StringBuilder();
        result.append("[NetworkAddresses]");

        try {
            Object e;
            if(onlyShowUsable) {
                e = getUsableNetworkInterfaces();
            } else {
                e = Collections.list(NetworkInterface.getNetworkInterfaces());
            }

            Iterator usableAddresses = ((List)e).iterator();

            while(usableAddresses.hasNext()) {
                NetworkInterface i$ = (NetworkInterface)usableAddresses.next();
                result.append("\n\n").append("[NetworkInterface]").append("\ndisplayName: ").append(i$.getDisplayName()).append("\nisUp: ").append(i$.isUp()).append("\nisLoopBack: ").append(i$.isLoopback()).append("\nisVirtual: ").append(i$.isVirtual()).append("\nmulticast: ").append(i$.supportsMulticast());
                Enumeration address = i$.getSubInterfaces();

                while(address.hasMoreElements()) {
                    result.append("\nsubInterfaces:");
                    NetworkInterface interfaceAddresses = (NetworkInterface)address.nextElement();
                    result.append(" - ").append(interfaceAddresses.getDisplayName());
                }

                List interfaceAddresses1 = i$.getInterfaceAddresses();
                Iterator i$1 = interfaceAddresses1.iterator();

                while(i$1.hasNext()) {
                    InterfaceAddress interfaceAddress = (InterfaceAddress)i$1.next();
                    result.append("\n\n").append(i$.getDisplayName()).append(" - [InterfaceAddress]");
                    InetAddress address1 = interfaceAddress.getAddress();
                    result.append("\naddress: ").append(address1).append("\nhostAddress: ").append(address1.getHostAddress()).append("\nhostName: ").append(address1.getHostName()).append("\nisLinkLocal: ").append(address1.isLinkLocalAddress()).append("\nisSiteLocal: ").append(address1.isSiteLocalAddress()).append("\nisLoopBack: ").append(address1.isLoopbackAddress()).append("\nbroadcastAddress: ").append(interfaceAddress.getBroadcast()).append("\nisIPv4: ").append(isIPv4Address(address1.getHostAddress())).append("\nisIPv6: ").append(isIPv6Address(address1.getHostAddress()));
                }
            }

            result.append("\n\n[UsableAddresses]");
            List usableAddresses1 = getUsableAddresses();
            Iterator i$2 = usableAddresses1.iterator();

            while(i$2.hasNext()) {
                InetAddress address2 = (InetAddress)i$2.next();
                result.append("\n").append(address2.getHostAddress());
            }
        } catch (RuntimeException var10) {
            var10.printStackTrace();
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        return result.toString();
    }
}
