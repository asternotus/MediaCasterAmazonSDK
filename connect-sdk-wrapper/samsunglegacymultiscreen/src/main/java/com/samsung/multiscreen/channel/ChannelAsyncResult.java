//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.ChannelError;

public interface ChannelAsyncResult<T> {
    void onResult(T var1);

    void onError(ChannelError var1);
}
