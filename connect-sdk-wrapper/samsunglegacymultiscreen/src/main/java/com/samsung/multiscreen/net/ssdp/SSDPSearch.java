//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.ssdp;

import com.samsung.multiscreen.net.NetworkUtil;
import com.samsung.multiscreen.net.ssdp.SSDPSearchListener;
import com.samsung.multiscreen.net.ssdp.SSDPSearchResult;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSDPSearch {
    private static final Logger LOG = Logger.getLogger(SSDPSearch.class.getName());
    private String searchTarget;
    private boolean running;
    private int retryInterval = 500;
    private int timeout = 3000;
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService executor;
    private List<DatagramSocket> sockets;
    private List<SSDPSearchResult> results;
    private SSDPSearchListener listener;

    public SSDPSearch(String searchTarget) {
        LOG.info("new SSDPSearch() searchTarget: " + searchTarget);
        this.searchTarget = searchTarget;
        this.running = false;
        this.scheduledExecutor = Executors.newScheduledThreadPool(3);
        this.executor = Executors.newCachedThreadPool();
        this.sockets = new ArrayList();
        this.results = new ArrayList();
    }

    public void start(SSDPSearchListener listener) {
        this.start(this.timeout, this.retryInterval, listener);
    }

    public void start(int timeout, SSDPSearchListener listener) {
        this.start(timeout, this.retryInterval, listener);
    }

    public void start(int timeout, int retryInterval, SSDPSearchListener listener) {
        LOG.info("start() running: " + this.running + ", timeout: " + timeout + ", retryInterval: " + retryInterval);
        if(!this.running) {
            this.running = true;
            this.listener = listener;
            int listenPort = 1900;
            List addresses = NetworkUtil.getUsableAddresses();
            Iterator i$ = addresses.iterator();

            while(i$.hasNext()) {
                InetAddress address = (InetAddress)i$.next();

                try {
                    ++listenPort;
                    this.searchAddress(address, listenPort);
                } catch (Exception var9) {
                    var9.printStackTrace();
                }
            }

            this.scheduledExecutor.schedule(new Runnable() {
                public void run() {
                    SSDPSearch.this.stop();
                }
            }, (long)timeout, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        LOG.info("stop() running: " + this.running);
        if(this.running) {
            this.running = false;

            try {
                this.executor.shutdownNow();
                this.scheduledExecutor.shutdownNow();
                Iterator e = this.sockets.iterator();

                while(e.hasNext()) {
                    DatagramSocket socket = (DatagramSocket)e.next();
                    socket.close();
                }
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            if(this.listener != null) {
                try {
                    this.listener.onResults(this.results);
                } catch (Exception var3) {
                    var3.printStackTrace();
                }

                this.listener = null;
            }

        }
    }

    private void searchAddress(InetAddress address, int listenPort) throws Exception {
        final String request = this.createRequest(this.searchTarget);

        try {
            final DatagramSocket e = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(address.getHostAddress()), listenPort));
            this.sockets.add(e);
            this.scheduledExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        DatagramPacket ex = new DatagramPacket(request.getBytes("UTF-8"), request.length(), InetAddress.getByName("239.255.255.250"), 1900);
                        e.send(ex);
                    } catch (RuntimeException var2) {
                        var2.printStackTrace();
                    } catch (Exception var3) {
                        var3.printStackTrace();
                    }

                }
            }, 0L, (long)this.retryInterval, TimeUnit.MILLISECONDS);
            this.executor.execute(new Runnable() {
                public void run() {
                    while(true) {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);

                        try {
                            e.receive(packet);
                            String ex = new String(packet.getData(), "UTF-8");
                            SSDPSearchResult result = SSDPSearchResult.createResult(ex);
                            if(result != null && !SSDPSearch.this.results.contains(result)) {
                                SSDPSearch.this.results.add(result);
                                if(SSDPSearch.this.listener != null) {
                                    try {
                                        SSDPSearch.this.listener.onResult(result);
                                    } catch (Exception var6) {
                                        var6.printStackTrace();
                                    }
                                }
                            }
                        } catch (IOException var7) {
                            SSDPSearch.LOG.info("Error reading from socket: " + var7.getLocalizedMessage());
                            return;
                        } catch (Exception var8) {
                            SSDPSearch.LOG.info("Error: " + var8.getLocalizedMessage());
                        }
                    }
                }
            });
        } catch (IOException var5) {
            LOG.info("Error opening MulticastSocket: " + var5.getLocalizedMessage());
            this.listener.onResults(this.results);
        } catch (Exception var6) {
            LOG.info("Error: " + var6.getLocalizedMessage());
        }

    }

    private String createRequest(String searchTarget) {
        String request = "M-SEARCH * HTTP/1.1\r\n";
        request = request + "HOST: 239.255.255.250:1900\r\n";
        request = request + "MAN: \"ssdp:discover\"\r\n";
        request = request + "ST: " + searchTarget + "\r\n";
        request = request + "MX: 3\r\n";
        request = request + "\r\n";
        return request;
    }

    private static void printResults(List<SSDPSearchResult> results) {
        Iterator i$ = results.iterator();

        while(i$.hasNext()) {
            SSDPSearchResult result = (SSDPSearchResult)i$.next();
            LOG.info("------------------------------------------------\n" + result);
        }

    }

    public static void main(String[] args) {
        SSDPSearch search = new SSDPSearch("urn:samsung.com:service:multi-screen-service:1");
        search.start(new SSDPSearchListener() {
            public void onResults(List<SSDPSearchResult> results) {
                SSDPSearch.LOG.info(">>>>>>>>>> onResults() >>>>>>>>>>>>>>>>>");
                SSDPSearch.printResults(results);
            }

            public void onResult(SSDPSearchResult result) {
                SSDPSearch.LOG.info(">>>>>>>>>> onResult() \n" + result);
            }
        });
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
