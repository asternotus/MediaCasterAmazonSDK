//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build.VERSION;
import com.mega.cast.utils.log.SmartLog;

import com.samsung.multiscreenfix.util.JSONUtil;
import com.samsung.multiscreenfix.util.NetUtil;
import com.samsung.multiscreenfix.util.RunUtil;

import java.io.IOException;
import java.lang.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MSFDSearchProvider extends SearchProvider {
    private static final String TAG = "MSFDSearchProvider";
    private static final long MAX_GET_SERVICE_INFO_WAIT_TIME = 15000L;
    private static final String MULTICAST_GROUP = "224.0.0.7";
    private static final int MULTICAST_PORT = 8001;
    private static final int SOCKET_TIMEOUT = 10000;
    private static final long DISCOVER_START_DELAY = 100L;
    private static final long DISCOVER_INTERVAL = 1000L;
    private static final int MAX_DISCOVER_NUM = 3;
    private static final int SERVICE_CHECK_TIMEOUT = 2000;
    private static final String KEY_TYPE_STATE = "type";
    private static final String KEY_TTL = "ttl";
    private static final String KEY_SID = "sid";
    private static final String KEY_DATA = "data";
    private static final String KEY_VERSION_1 = "v1";
    private static final String KEY_VERSION_2 = "v2";
    private static final String KEY_URI = "uri";
    private static final String TYPE_DISCOVER = "discover";
    private static final String STATE_UP = "up";
    private static final String STATE_DOWN = "down";
    private static final String STATE_ALIVE = "alive";
    private static final String discoverMessage;
    private final Context context;
    private static volatile InetAddress multicastInetAddress;
    private DatagramPacket discoverPacket = null;
    private volatile MulticastSocket socket;
    private volatile MulticastLock multicastLock;
    private boolean receive = false;
    private final Map<String, Long> aliveMap = new ConcurrentHashMap();
    private ScheduledExecutorService executor;
    private Thread receiverThread;
    private final Runnable receiveHandler = new Runnable() {
        public void run() {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                while(MSFDSearchProvider.this.receive) {
                    try {
                        this.reapServices();
                        MSFDSearchProvider.this.socket.receive(receivePacket);
                        if(receivePacket.getLength() > 0) {
                            Map responseMap;
                            try {
                                String e = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                                responseMap = JSONUtil.parse(e);
                            } catch (Exception var21) {
                                SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var21));
                                continue;
                            }

                            String state;
                            if(responseMap != null && !responseMap.isEmpty() && !"discover".equals(state = (String)responseMap.get("type"))) {
                                final String id = (String)responseMap.get("sid");
                                if(id != null) {
                                    Service service = MSFDSearchProvider.this.getServiceById(id);
                                    if(!"alive".equals(state) && !"up".equals(state)) {
                                        if(service != null && "down".equals(state)) {
                                            MSFDSearchProvider.this.aliveMap.remove(id);
                                            MSFDSearchProvider.this.removeServiceAndNotify(service);
                                        }
                                    } else {
                                        final long ttl = ((Long)responseMap.get("ttl")).longValue();
                                        if(service == null && !MSFDSearchProvider.this.aliveMap.containsKey(id)) {
                                            this.updateAlive(id, ttl);
                                            Map dataMap = (Map)responseMap.get("data");
                                            if(dataMap != null) {
                                                Map endpointMap = (Map)dataMap.get("v2");
                                                if(endpointMap != null) {
                                                    String url = (String)endpointMap.get("uri");
                                                    if(url != null) {
                                                        Uri uri = Uri.parse(url);
                                                        Service.getByURI(uri, 2000, new Result<Service>() {
                                                            public void onSuccess(Service result) {
                                                                updateAlive(id, ttl);
                                                                MSFDSearchProvider.this.addService(result);
                                                            }

                                                            public void onError(Error error) {
                                                                MSFDSearchProvider.this.aliveMap.remove(id);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        } else {
                                            this.updateAlive(id, ttl);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SocketTimeoutException var22) {
                        ;
                    } catch (SocketException var23) {
                        break;
                    } catch (IOException var24) {
                        SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var24));
                    } catch (Exception var25) {
                        SmartLog.e("MSFDSearchProvider", "receiveHandler exception: " + var25.getMessage());
                    }
                }
            } finally {
                if(MSFDSearchProvider.this.socket != null) {
                    MSFDSearchProvider.this.socket.close();
                }

            }

        }

        private synchronized void reapServices() {
            long now = (new Date()).getTime();
            Iterator var3 = MSFDSearchProvider.this.aliveMap.keySet().iterator();

            while(var3.hasNext()) {
                String id = (String)var3.next();
                long expires = ((Long)MSFDSearchProvider.this.aliveMap.get(id)).longValue();
                if(expires < now) {
                    Service service = MSFDSearchProvider.this.getServiceById(id);
                    MSFDSearchProvider.this.aliveMap.remove(id);
                    if(service != null) {
                        MSFDSearchProvider.this.removeServiceAndNotify(service);
                    }
                }
            }

        }

        private synchronized void updateAlive(String id, long ttl) {
            long now = (new Date()).getTime();
            long expires = now + ttl;
            MSFDSearchProvider.this.aliveMap.put(id, Long.valueOf(expires));
        }
    };

    private MSFDSearchProvider(Context context) {
        this.context = context;
    }

    private MSFDSearchProvider(Context context, Search.SearchListener searchListener) {
        super(searchListener);
        this.context = context;
    }

    private void setupDiscovery() throws IOException {
        multicastInetAddress = InetAddress.getByName("224.0.0.7");
        InetSocketAddress multicastGroup = new InetSocketAddress("224.0.0.7", 8001);
        this.discoverPacket = new DatagramPacket(discoverMessage.getBytes(), discoverMessage.length(), multicastGroup);
    }

    public void start() {
        if(this.searching) {
            this.stop();
        }

        this.clearServices();
        this.aliveMap.clear();

        try {
            if(this.discoverPacket == null) {
                this.setupDiscovery();
            }

            this.acquireMulticastLock();
            this.socket = new MulticastSocket(8001);
            this.socket.setBroadcast(true);
            this.socket.setSoTimeout(10000);
            this.socket.joinGroup(new InetSocketAddress(multicastInetAddress, 8001), NetworkInterface.getByName("eth0"));
            this.receive = true;
            this.receiverThread = new Thread(this.receiveHandler);
            this.receiverThread.start();
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.scheduleAtFixedRate(new Runnable() {
                private int numDiscover = 0;

                public void run() {
                    try {
                        int e = VERSION.SDK_INT;
                        if(e == 19) {
                            MSFDSearchProvider.this.socket.send(MSFDSearchProvider.this.discoverPacket);
                        } else if(this.numDiscover++ < 3) {
                            MSFDSearchProvider.this.socket.send(MSFDSearchProvider.this.discoverPacket);
                        } else {
                            MSFDSearchProvider.this.executor.shutdown();
                        }
                    } catch (IOException var2) {
                        SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var2));
                    }

                }
            }, 100L, 1000L, TimeUnit.MILLISECONDS);
            this.searching = true;
        } catch (IOException var2) {
            SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var2));
        }

        if(!this.searching) {
            if(this.socket != null) {
                this.socket.close();
            }

            NetUtil.releaseMulticastLock(this.multicastLock);
        }

    }

    public boolean stop() {
        if(!this.searching) {
            return false;
        } else {
            this.searching = false;
            NetUtil.releaseMulticastLock(this.multicastLock);
            if(this.executor != null) {
                this.executor.shutdown();
                this.executor = null;
            }

            this.receive = false;
            if(this.socket != null && multicastInetAddress != null) {
                try {
                    this.socket.leaveGroup(multicastInetAddress);
                } catch (IOException var3) {
                    SmartLog.e("MSFDSearchProvider", "stop exception: " + var3.getMessage());
                }
            }

            if(this.receiverThread != null) {
                try {
                    this.receiverThread.join(1000L);
                } catch (InterruptedException var2) {
                    SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var2));
                }

                this.receiverThread = null;
            }

            return true;
        }
    }

    public static SearchProvider create(Context context) {
        return new MSFDSearchProvider(context);
    }

    static SearchProvider create(Context context, Search.SearchListener searchListener) {
        return new MSFDSearchProvider(context, searchListener);
    }

    static ProviderThread getById(Context context, final String searchId, final Result<Service> result) {
        ProviderThread thread = null;
        MulticastSocket socket = null;

        try {
            final MulticastLock e = NetUtil.acquireMulticastLock(context, "MSFDSearchProvider");
            final InetAddress multicastInetAddress = InetAddress.getByName("224.0.0.7");
            InetSocketAddress multicastGroup = new InetSocketAddress("224.0.0.7", 8001);
            final DatagramPacket discoverPacket = new DatagramPacket(discoverMessage.getBytes(), discoverMessage.length(), multicastGroup);
            socket = new MulticastSocket(8001);
            socket.joinGroup(multicastInetAddress);
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            final MulticastSocket finalSocket = socket;
            thread = new ProviderThread(new Runnable() {
                private boolean searching = true;
                private boolean processing = false;

                public void run() {
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                        while(this.searching && !Thread.currentThread().isInterrupted()) {
                            try {
                                finalSocket.receive(receivePacket);
                                if(Thread.currentThread().isInterrupted()) {
                                    break;
                                }

                                if(!this.processing && receivePacket.getLength() > 0) {
                                    Map responseMap;
                                    try {
                                        String ex = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                                        responseMap = JSONUtil.parse(ex);
                                    } catch (Exception var17) {
                                        SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var17));
                                        continue;
                                    }

                                    String state;
                                    if(responseMap != null && !responseMap.isEmpty() && !"discover".equals(state = (String)responseMap.get("type"))) {
                                        String id = (String)responseMap.get("sid");
                                        if(id != null && searchId.equals(id)) {
                                            this.processing = true;
                                            if("alive".equals(state) || "up".equals(state)) {
                                                Map dataMap = (Map)responseMap.get("data");
                                                if(dataMap != null) {
                                                    Map endpointMap = (Map)dataMap.get("v2");
                                                    if(endpointMap != null) {
                                                        String url = (String)endpointMap.get("uri");
                                                        if(url != null) {
                                                            Uri uri = Uri.parse(url);
                                                            Service.getByURI(uri, 2000, new Result<Service>() {
                                                                public void onSuccess(final Service service) {
                                                                    searching = false;
                                                                    RunUtil.runOnUI(new Runnable() {
                                                                        public void run() {
                                                                            result.onSuccess(service);
                                                                        }
                                                                    });
                                                                }

                                                                public void onError(Error error) {
                                                                    processing = false;
                                                                }
                                                            });
                                                            continue;
                                                        }
                                                    }
                                                }
                                            }

                                            this.processing = false;
                                        }
                                    }
                                }
                            } catch (IOException var18) {
                                SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var18));
                                break;
                            }
                        }

                        try {
                            finalSocket.leaveGroup(multicastInetAddress);
                        } catch (IOException var16) {
                            SmartLog.e("MSFDSearchProvider", "ProviderThread exception: " + var16.getMessage());
                        }

                        NetUtil.releaseMulticastLock(e);
                        executor.shutdown();
                    } finally {
                        if(!finalSocket.isClosed()) {
                            finalSocket.close();
                        }

                    }

                }
            }) {
                void terminate() {
                    this.interrupt();
                }
            };
            thread.start();
            final MulticastSocket finalSocket1 = socket;
            executor.schedule(new Runnable() {
                public void run() {
                    if(!finalSocket1.isClosed()) {
                        finalSocket1.close();
                    }

                    RunUtil.runOnUI(new Runnable() {
                        public void run() {
                            result.onError(Error.create("Not Found"));
                        }
                    });
                }
            }, 15000L, TimeUnit.MILLISECONDS);
            MSFDSearchProvider.FutureRunnable runnable = new MSFDSearchProvider.FutureRunnable() {
                private int numDiscover = 0;
                private ScheduledFuture<?> future;

                public void run() {
                    try {
                        if(this.numDiscover++ < 3) {
                            finalSocket1.send(discoverPacket);
                        } else {
                            this.future.cancel(false);
                        }
                    } catch (IOException var2) {
                        SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var2));
                    }

                }

                public void setFuture(ScheduledFuture<?> future) {
                    this.future = future;
                }
            };
            ScheduledFuture t = executor.scheduleAtFixedRate(runnable, 100L, 1000L, TimeUnit.MILLISECONDS);
            runnable.setFuture(t);
        } catch (final Exception var13) {
            SmartLog.e("MSFDSearchProvider", SmartLog.getStackTraceString(var13));
            if(socket != null && !socket.isClosed()) {
                socket.close();
            }

            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    result.onError(Error.create(var13));
                }
            });
        }

        return thread;
    }

    private void acquireMulticastLock() {
        if(this.multicastLock == null) {
            this.multicastLock = NetUtil.acquireMulticastLock(this.context, "MSFDSearchProvider");
        } else if(!this.multicastLock.isHeld()) {
            this.multicastLock.acquire();
        }

    }

    static {
        HashMap discoverMap = new HashMap();
        discoverMap.put("type", "discover");
        discoverMessage = JSONUtil.toJSONString(discoverMap);
    }

    private interface FutureRunnable extends Runnable {
        void setFuture(ScheduledFuture<?> var1);
    }
}
