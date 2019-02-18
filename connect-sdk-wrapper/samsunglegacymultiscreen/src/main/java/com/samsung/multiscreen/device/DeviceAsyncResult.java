//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device;

import com.samsung.multiscreen.device.DeviceError;

public interface DeviceAsyncResult<T> {
    void onResult(T var1);

    void onError(DeviceError var1);
}
