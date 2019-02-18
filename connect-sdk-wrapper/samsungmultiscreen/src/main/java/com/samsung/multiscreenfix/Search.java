//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.content.Context;
import com.mega.cast.utils.log.SmartLog;

import com.samsung.multiscreenfix.util.RunUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Search {
    private static final String TAG = "Search";
    private static final int SERVICE_CHECK_TIMEOUT = 2000;
    private static Search instance;
    private final Context context;
    private final List<SearchProvider> providers = new ArrayList();
    private final List<SearchProvider> removedProviders = new ArrayList();
    private SearchProvider bleProvider = null;
    private boolean startingBle = false;
    private boolean stoppingBle = false;
    private int onStartNotified;
    private int numRunning;
    private StandbyDeviceList mStandbyDeviceList;
    private final Search.SearchListener searchListener = new Search.SearchListener() {
        public void onStart() {
            if(--Search.this.onStartNotified == 0 && Search.this.onStartListener != null) {
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        if(Search.this.onStartListener != null) {
                            Search.this.onStartListener.onStart();
                        }

                    }
                });
            }

        }

        public void onStop() {
            if(--Search.this.numRunning <= 0) {
                if(Search.this.clearProviders) {
                    Search.this.removeAllProviders();
                } else {
                    Search.this.processRemovedProviders();
                }

                if(Search.this.onStopListener != null) {
                    RunUtil.runOnUI(new Runnable() {
                        public void run() {
                            if(Search.this.onStopListener != null) {
                                Search.this.onStopListener.onStop();
                            }

                        }
                    });
                }
            }

        }

        public void onFound(final Service service) {
            if(Search.this.addService(service)) {
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        if(Search.this.mStandbyDeviceList != null) {
                            Service lostService = Search.this.mStandbyDeviceList.getLostStandbyService(service);
                            if(lostService != null) {
                                Search.this.onServiceLostListener.onLost(lostService);
                            }
                        }

                        if(Search.this.onServiceFoundListener != null) {
                            Search.this.onServiceFoundListener.onFound(service);
                        }

                    }
                });
            }

        }

        public void onLost(Service service) {
            Search.this.validateService(service);
        }

        public void onFoundOnlyBLE(final String NameOfTV) {
            if(Search.this.onBleFoundListener != null) {
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        if(Search.this.onBleFoundListener != null) {
                            Search.this.onBleFoundListener.onFoundOnlyBLE(NameOfTV);
                        }

                    }
                });
            }

        }
    };
    private boolean clearProviders = false;
    private List<Service> services = new CopyOnWriteArrayList();
    private volatile Search.OnStartListener onStartListener;
    private volatile Search.OnStopListener onStopListener;
    private volatile Search.OnServiceFoundListener onServiceFoundListener;
    private volatile Search.OnServiceLostListener onServiceLostListener;
    private volatile Search.OnBleFoundListener onBleFoundListener;

    static Search getInstance(Context context) {
        if(instance == null) {
            instance = new Search(context);
        }

        return instance;
    }

    private Search(Context context) {
        this.context = context;
    }

    public boolean isSearching() {
        Iterator var1 = this.providers.iterator();

        SearchProvider provider;
        do {
            if(!var1.hasNext()) {
                return false;
            }

            provider = (SearchProvider)var1.next();
        } while(!provider.isSearching());

        return true;
    }

    public boolean isSearchingBle() {
        return this.bleProvider != null && this.bleProvider.isSearching();
    }

    public boolean start() {
        return this.start(Boolean.valueOf(true));
    }

    public boolean start(Boolean showStandbyDevices) {
        if(this.isSearching()) {
            return false;
        } else {
            this.startDiscovery();
            SmartLog.d("Search", "start() called & Discovery started.");
            if(showStandbyDevices.booleanValue()) {
                this.mStandbyDeviceList = StandbyDeviceList.create(this.context, this.searchListener);
                this.mStandbyDeviceList.start();
            } else if(this.mStandbyDeviceList != null) {
                this.mStandbyDeviceList.destruct();
                this.mStandbyDeviceList = null;
            }

            return true;
        }
    }

    public boolean stop() {
        this.stopDiscovery();
        return true;
    }

    public void clearStandbyDeviceList() {
        if(this.mStandbyDeviceList != null) {
            this.mStandbyDeviceList.clear();
        }

    }

    public boolean isSupportBle() {
        return this.context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
    }

    public boolean startUsingBle() {
        if(!this.isSupportBle()) {
            return false;
        } else if(this.isSearchingBle()) {
            return false;
        } else {
            this.startDiscoveryUsingBle();
            return true;
        }
    }

    public boolean stopUsingBle() {
        this.stopDiscoveryUsingBle();
        return true;
    }

    public List<Service> getServices() {
        return Collections.unmodifiableList(this.services);
    }

    public void addProvider(SearchProvider provider) {
        if(provider == null) {
            throw new NullPointerException();
        } else {
            List var2 = this.providers;
            synchronized(this.providers) {
                this.providers.add(provider);
                provider.setSearchListener(this.searchListener);
            }
        }
    }

    public synchronized boolean removeProvider(SearchProvider provider) {
        if(provider == null) {
            throw new NullPointerException();
        } else if(!provider.isSearching()) {
            return this.providers.remove(provider);
        } else {
            this.removedProviders.add(provider);
            return false;
        }
    }

    private synchronized void processRemovedProviders() {
        if(!this.removedProviders.isEmpty()) {
            Iterator var1 = (new ArrayList(this.removedProviders)).iterator();

            while(var1.hasNext()) {
                SearchProvider provider = (SearchProvider)var1.next();
                if(!provider.isSearching() && this.providers.remove(provider)) {
                    this.removedProviders.remove(provider);
                }
            }
        }

    }

    public synchronized void removeAllProviders() {
        this.clearProviders = false;
        if(!this.isSearching()) {
            this.providers.clear();
        } else {
            this.clearProviders = true;
        }

    }

    private void startDiscovery() {
        if(this.providers.isEmpty()) {
            SmartLog.w("Search", "No search providers specified. Adding default providers...");
            this.providers.add(MDNSSearchProvider.create(this.context, this.searchListener));
            this.providers.add(MSFDSearchProvider.create(this.context, this.searchListener));
        }

        this.services.clear();
        this.onStartNotified = this.numRunning = this.providers.size();
        Iterator var1 = this.providers.iterator();

        while(var1.hasNext()) {
            final SearchProvider provider = (SearchProvider)var1.next();
            RunUtil.runInBackground(new Runnable() {
                public void run() {
                    provider.start();
                    Search.this.searchListener.onStart();
                    if(!provider.isSearching()) {
                        Search.this.searchListener.onStop();
                    }

                }
            });
        }

    }

    private void stopDiscovery() {
        Iterator var1 = this.providers.iterator();

        while(var1.hasNext()) {
            final SearchProvider provider = (SearchProvider)var1.next();
            Runnable runnable = new Runnable() {
                public void run() {
                    boolean stop = provider.stop();
                    if(stop) {
                        Search.this.searchListener.onStop();
                        if(Search.this.mStandbyDeviceList != null) {
                            Search.this.mStandbyDeviceList.stop();
                        }
                    }

                }
            };
            RunUtil.runInBackground(runnable);
        }

    }

    private void startDiscoveryUsingBle() {
        if(this.bleProvider == null) {
            this.bleProvider = BLESearchProvider.create(this.context, this.searchListener);
        }

        this.onStartNotified = this.numRunning = 1;
        RunUtil.runInBackground(new Runnable() {
            public void run() {
                Search.this.bleProvider.start();
                Search.this.searchListener.onStart();
                if(!Search.this.bleProvider.isSearching()) {
                    Search.this.searchListener.onStop();
                }

            }
        });
    }

    private void stopDiscoveryUsingBle() {
        Runnable runnable = new Runnable() {
            public void run() {
                boolean stop = Search.this.bleProvider.stop();
                if(stop) {
                    Search.this.searchListener.onStop();
                }

            }
        };
        RunUtil.runInBackground(runnable);
    }

    private boolean addService(Service service) {
        if(service == null) {
            return false;
        } else {
            List var2 = this.services;
            synchronized(this.services) {
                Boolean found = Boolean.valueOf(false);

                for(int itr = 0; itr < this.services.size(); ++itr) {
                    if(((Service)this.services.get(itr)).isEqualTo(service).booleanValue()) {
                        found = Boolean.valueOf(true);
                        break;
                    }
                }

                if(!found.booleanValue()) {
                    this.services.add(service);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private boolean removeService(Service service) {
        if(service == null) {
            return false;
        } else {
            List var2 = this.services;
            synchronized(this.services) {
                for(int i = 0; i < this.services.size(); ++i) {
                    if(((Service)this.services.get(i)).isEqualTo(service).booleanValue()) {
                        this.services.remove(i);
                        return true;
                    }
                }

                return false;
            }
        }
    }

    private void removeAndNotify(final Service service) {
        if(this.removeService(service) && this.onServiceLostListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    if(Search.this.onServiceLostListener != null) {
                        Search.this.onServiceLostListener.onLost(service);
                        if(Search.this.mStandbyDeviceList != null) {
                            Service foundService = Search.this.mStandbyDeviceList.getFoundStandbyService(service);
                            if(foundService != null) {
                                Search.this.onServiceFoundListener.onFound(foundService);
                            }
                        }
                    }

                }
            });
        }

    }

    private void validateService(Service service) {
        this.removeAndNotify(service);
        if(this.mStandbyDeviceList != null && !service.isStandbyService.booleanValue()) {
            Iterator var2 = this.providers.iterator();

            while(var2.hasNext()) {
                SearchProvider provider = (SearchProvider)var2.next();
                provider.removeService(service);
            }
        }

    }

    public void setOnStartListener(Search.OnStartListener onStartListener) {
        this.onStartListener = onStartListener;
    }

    public void setOnStopListener(Search.OnStopListener onStopListener) {
        this.onStopListener = onStopListener;
    }

    public void setOnServiceFoundListener(Search.OnServiceFoundListener onServiceFoundListener) {
        this.onServiceFoundListener = onServiceFoundListener;
    }

    public void setOnServiceLostListener(Search.OnServiceLostListener onServiceLostListener) {
        this.onServiceLostListener = onServiceLostListener;
    }

    public void setOnBleFoundListener(Search.OnBleFoundListener onBleFoundListener) {
        this.onBleFoundListener = onBleFoundListener;
    }

    interface SearchListener extends Search.OnStartListener, Search.OnStopListener, Search.OnServiceFoundListener, Search.OnServiceLostListener, Search.OnBleFoundListener {
    }

    public interface OnBleFoundListener {
        void onFoundOnlyBLE(String var1);
    }

    public interface OnServiceLostListener {
        void onLost(Service var1);
    }

    public interface OnServiceFoundListener {
        void onFound(Service var1);
    }

    public interface OnStopListener {
        void onStop();
    }

    public interface OnStartListener {
        void onStart();
    }
}
