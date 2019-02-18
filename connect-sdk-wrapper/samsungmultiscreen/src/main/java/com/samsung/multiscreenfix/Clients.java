//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Clients {
    private final Map<String, Client> clients = new HashMap();
    private String myClientId;
    private Client host;
    private Client me;
    private final Channel channel;

    protected Clients(Channel channel) {
        this.channel = channel;
    }

    public Client get(String clientId) {
        return (Client)this.clients.get(clientId);
    }

    public Client getHost() {
        if(this.host == null) {
            Iterator var1 = this.clients.values().iterator();

            while(var1.hasNext()) {
                Client client = (Client)var1.next();
                if(client.isHost()) {
                    this.host = client;
                    break;
                }
            }
        }

        return this.host;
    }

    public Client me() {
        if(this.myClientId != null) {
            Client client = this.get(this.myClientId);
            if(client != null && !client.equals(this.me)) {
                this.me = client;
            }
        }

        return this.me;
    }

    public boolean isMe(Client client) {
        return client.getId().equals(this.myClientId);
    }

    public int size() {
        return this.clients.size();
    }

    public List<Client> list() {
        return Collections.unmodifiableList(new ArrayList(this.clients.values()));
    }

    protected void setMyClientId(String clientId) {
        this.myClientId = clientId;
        Client client = this.get(this.myClientId);
        if(client != null) {
            this.me = client;
        }

    }

    protected void reset() {
        this.myClientId = null;
        this.clients.clear();
    }

    protected void add(List<Client> clientList) {
        Iterator var2 = clientList.iterator();

        while(var2.hasNext()) {
            Client client = (Client)var2.next();
            this.add(client);
        }

    }

    protected void add(Client client) {
        this.clients.put(client.getId(), client);
    }

    protected void remove(Client client) {
        this.clients.remove(client.getId());
    }

    public String toString() {
        return "Clients(clients=" + this.clients + ", myClientId=" + this.myClientId + ", host=" + this.getHost() + ")";
    }

    public Channel getChannel() {
        return this.channel;
    }
}
