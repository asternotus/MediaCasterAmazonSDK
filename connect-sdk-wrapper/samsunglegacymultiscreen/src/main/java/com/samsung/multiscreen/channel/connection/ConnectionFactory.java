//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.connection;

import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.connection.ChannelConnection;
import com.samsung.multiscreen.channel.connection.ChannelWebsocketConnection;
import com.samsung.multiscreen.channel.info.ChannelInfo;
import java.util.Map;

public class ConnectionFactory {
    public ConnectionFactory() {
    }

    public ChannelConnection getConnection(Channel channel, ChannelInfo channelInfo, Map<String, String> attributes) {
        return new ChannelWebsocketConnection(channel, channelInfo, attributes);
    }
}
