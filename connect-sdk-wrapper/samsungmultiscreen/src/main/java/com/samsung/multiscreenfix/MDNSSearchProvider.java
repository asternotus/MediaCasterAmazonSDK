//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.content.Context;
import android.net.wifi.WifiManager.MulticastLock;
import com.mega.cast.utils.log.SmartLog;

import com.samsung.multiscreenfix.util.NetUtil;
import com.samsung.multiscreenfix.util.RunUtil;

import java.io.IOException;
import java.lang.*;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class MDNSSearchProvider extends SearchProvider {
    private static final String TAG = "MDNSSearchProvider";
    private static final long MAX_GET_SERVICE_INFO_WAIT_TIME = 15000L;
    private final Context context;
    private volatile MulticastLock multicastLock;
    private volatile JmDNS jmdns;
    static final String SERVICE_TYPE = "_samsungmsf._tcp.local.";
    private static final int SERVICE_CHECK_TIMEOUT = 2000;
    private final ServiceListener serviceListener = new ServiceListener() {
        public void serviceAdded(final ServiceEvent serviceEvent) {
            if(MDNSSearchProvider.this.searching) {
                RunUtil.runInBackground(new Runnable() {
                    public void run() {
                        Service service = MDNSSearchProvider.getService(MDNSSearchProvider.this.jmdns, serviceEvent.getType(), serviceEvent.getName());
                        if(service != null && service.getUri() != null) {
                            Service.getByURI(service.getUri(), 2000, new Result<Service>() {
                                public void onSuccess(Service result) {
                                    MDNSSearchProvider.this.addService(result);
                                }

                                public void onError(Error error) {
                                }
                            });
                        }
                    }
                });
            }
        }

        public void serviceRemoved(ServiceEvent serviceEvent) {
            Service service = MDNSSearchProvider.this.getServiceById(serviceEvent.getName());
            MDNSSearchProvider.this.removeServiceAndNotify(service);
        }

        public void serviceResolved(ServiceEvent serviceEvent) {
        }
    };

    private MDNSSearchProvider(Context context) {
        this.context = context;
    }

    private MDNSSearchProvider(Context context, Search.SearchListener searchListener) {
        super(searchListener);
        this.context = context;
    }

    public void start() {
        if(this.searching) {
            this.stop();
        }

        this.clearServices();
        this.searching = this.acquireMulticastLock() && this.createJmDNS();
    }

    public boolean stop() {
        if(!this.searching) {
            return false;
        } else {
            this.searching = false;
            this.destroyJmDNS();
            this.releaseMulticastLock();
            return true;
        }
    }

    public static SearchProvider create(Context context) {
        return new MDNSSearchProvider(context);
    }

    static SearchProvider create(Context context, Search.SearchListener searchListener) {
        return new MDNSSearchProvider(context, searchListener);
    }

    static ProviderThread getById(final Context context, final String id, final Result<Service> result) {
        ProviderThread thread = new ProviderThread(new Runnable() {
            public void run() {
                ProviderThread currentThread = (ProviderThread)Thread.currentThread();
                MulticastLock multicastLock = NetUtil.acquireMulticastLock(context, "MDNSSearchProvider");
                JmDNS jmdns = null;
                Runnable runnable = null;

                try {
                    InetAddress type = NetUtil.getDeviceIpAddress(context);
                    jmdns = JmDNS.create(type);
                } catch (final IOException var11) {
                    var11.printStackTrace();
                    runnable = new Runnable() {
                        public void run() {
                            result.onError(Error.create(var11));
                        }
                    };
                }

                if(jmdns != null) {
                    String var12 = id + "." + "_samsungmsf._tcp.local.";
                    String name = id;
                    int retry = 2;

                    ServiceInfo info;
                    for(info = null; !currentThread.isTerminate() && info == null && retry-- >= 0 && !Thread.currentThread().isInterrupted(); info = jmdns.getServiceInfo(var12, name, false, 5000L)) {
                        ;
                    }

                    if(!currentThread.isTerminate()) {
                        if(info == null) {
                            runnable = new Runnable() {
                                public void run() {
                                    result.onError(Error.create("Not Found"));
                                }
                            };
                        } else {
                            final Service e = Service.create(info);
                            runnable = new Runnable() {
                                public void run() {
                                    result.onSuccess(e);
                                }
                            };
                        }
                    }

                    try {
                        jmdns.close();
                    } catch (IOException var10) {
                        SmartLog.e("MDNSSearchProvider", "getById error: " + var10.getMessage());
                    }
                }

                NetUtil.releaseMulticastLock(multicastLock);
                if(runnable != null) {
                    RunUtil.runOnUI(runnable);
                }

            }
        }) {
            private boolean terminate = false;

            void terminate() {
                this.terminate = true;
            }

            public boolean isTerminate() {
                return this.terminate;
            }
        };
        thread.start();
        return thread;
    }

    private boolean createJmDNS() {
        this.destroyJmDNS();
        boolean success = false;

        try {
            InetAddress e = NetUtil.getDeviceIpAddress(this.context);
            this.jmdns = JmDNS.create(e);
            this.jmdns.addServiceListener("_samsungmsf._tcp.local.", this.serviceListener);
            success = true;
        } catch (IOException var3) {
            var3.printStackTrace();
        }

        return success;
    }

    private synchronized boolean destroyJmDNS() {
        boolean success = false;
        if(this.jmdns != null) {
            this.jmdns.removeServiceListener("_samsungmsf._tcp.local.", this.serviceListener);

            try {
                this.jmdns.close();
                success = true;
            } catch (IOException var3) {
                var3.printStackTrace();
            }

            this.jmdns = null;
        }

        return success;
    }

    static Service getService(JmDNS jmdns, String type, String name) {
        int retry = 2;

        ServiceInfo info;
        do {
            if(retry-- < 0) {
                return null;
            }

            info = jmdns.getServiceInfo(type, name, false, 5000L);
        } while(info == null);

        return Service.create(info);
    }

    private boolean acquireMulticastLock() {
        boolean success = false;

        try {
            if(this.multicastLock == null) {
                this.multicastLock = NetUtil.acquireMulticastLock(this.context, "MDNSSearchProvider");
            } else if(!this.multicastLock.isHeld()) {
                this.multicastLock.acquire();
            }

            success = true;
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return success;
    }

    private boolean releaseMulticastLock() {
        boolean success = false;

        try {
            NetUtil.releaseMulticastLock(this.multicastLock);
            success = true;
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return success;
    }
}
