//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.ChannelClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChannelClients {
    private Map<String, ChannelClient> clientsMap = new HashMap();
    private String myClientId;

    protected ChannelClients() {
    }

    protected void clear() {
        this.myClientId = null;
        this.clientsMap.clear();
    }

    protected void reset(String myClientId, List<ChannelClient> clientList) {
        this.myClientId = myClientId;
        this.clientsMap.clear();
        Iterator i$ = clientList.iterator();

        while(i$.hasNext()) {
            ChannelClient client = (ChannelClient)i$.next();
            this.clientsMap.put(client.getId(), client);
        }

    }

    protected void add(ChannelClient client) {
        this.clientsMap.put(client.getId(), client);
    }

    protected void remove(ChannelClient client) {
        this.clientsMap.remove(client.getId());
    }

    public ChannelClient me() {
        return this.get(this.myClientId);
    }

    public ChannelClient get(String id) {
        return (ChannelClient)this.clientsMap.get(id);
    }

    public ChannelClient host() {
        Iterator iter = this.clientsMap.values().iterator();

        ChannelClient client;
        do {
            if(!iter.hasNext()) {
                return null;
            }

            client = (ChannelClient)iter.next();
        } while(!client.isHost());

        return client;
    }

    public List<ChannelClient> list() {
        return Collections.unmodifiableList(new ArrayList(this.clientsMap.values()));
    }

    public int size() {
        return this.clientsMap.size();
    }
}
