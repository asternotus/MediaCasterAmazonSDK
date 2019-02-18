//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.ssdp;

import com.samsung.multiscreen.net.ssdp.SSDPSearchResult;
import java.util.List;

public interface SSDPSearchListener {
    void onResult(SSDPSearchResult var1);

    void onResults(List<SSDPSearchResult> var1);
}
