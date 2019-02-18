//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.ChannelClient;

public interface IChannelListener {
    void onConnect();

    void onDisconnect();

    void onClientConnected(ChannelClient var1);

    void onClientDisconnected(ChannelClient var1);

    void onClientMessage(ChannelClient var1, String var2);
}
