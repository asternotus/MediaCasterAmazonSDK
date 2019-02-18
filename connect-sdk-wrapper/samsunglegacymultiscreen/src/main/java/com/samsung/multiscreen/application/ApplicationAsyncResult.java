//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application;

import com.samsung.multiscreen.application.ApplicationError;

public interface ApplicationAsyncResult<T> {
    void onResult(T var1);

    void onError(ApplicationError var1);
}
