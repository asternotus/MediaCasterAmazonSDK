//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.connection;

import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.net.json.JSONRPCMessage;

public interface IChannelConnectionListener {
    void onConnect();

    void onConnectError(ChannelError var1);

    void onDisconnect();

    void onDisconnectError(ChannelError var1);

    void onMessage(JSONRPCMessage var1);
}
