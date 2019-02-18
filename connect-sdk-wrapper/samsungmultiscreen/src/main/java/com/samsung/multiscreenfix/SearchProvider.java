//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class SearchProvider {
    private static final String TAG = "SearchProvider";
    boolean searching = false;
    private List<Service> services = new CopyOnWriteArrayList();
    protected List<String> TVListOnlyBle = new CopyOnWriteArrayList();
    private Search.SearchListener searchListener;

    SearchProvider(Search.SearchListener searchListener) {
        this.searchListener = searchListener;
    }

    public abstract void start();

    public abstract boolean stop();

    public List<Service> getServices() {
        return Collections.unmodifiableList(this.services);
    }

    protected synchronized void setServices(List<Service> services) {
        this.clearServices();
        if(services != null) {
            this.services.addAll(services);
        }

    }

    protected void addService(Service service) {
        if(service != null) {
            boolean found = false;
            List var3 = this.services;
            synchronized(this.services) {
                if(!this.services.contains(service)) {
                    this.services.add(service);
                    found = true;
                }
            }

            if(found && this.searchListener != null) {
                this.searchListener.onFound(service);
            }

        }
    }

    void removeService(Service service) {
        if(service != null) {
            List var2 = this.services;
            synchronized(this.services) {
                this.services.remove(service);
            }
        }
    }

    void removeServiceAndNotify(Service service) {
        if(service != null) {
            this.removeService(service);
            if(this.searchListener != null) {
                this.searchListener.onLost(service);
            }

        }
    }

    protected synchronized void clearServices() {
        this.services.clear();
    }

    protected Service getServiceById(String id) {
        Iterator var2 = this.services.iterator();

        Service service;
        do {
            if(!var2.hasNext()) {
                return null;
            }

            service = (Service)var2.next();
        } while(!service.getId().equals(id));

        return service;
    }

    protected void addTVOnlyBle(String NameOfTV) {
        if(NameOfTV != null) {
            boolean found = false;
            List var3 = this.TVListOnlyBle;
            synchronized(this.TVListOnlyBle) {
                if(!this.TVListOnlyBle.contains(NameOfTV)) {
                    this.TVListOnlyBle.add(NameOfTV);
                    found = true;
                }
            }

            if(found && this.searchListener != null) {
                this.searchListener.onFoundOnlyBLE(NameOfTV);
            }

        }
    }

    public boolean equals(Object obj) {
        return this.getClass().getName().equals(obj.getClass().getName());
    }

    protected SearchProvider() {
    }

    boolean isSearching() {
        return this.searching;
    }

    protected void setSearchListener(Search.SearchListener searchListener) {
        this.searchListener = searchListener;
    }
}
