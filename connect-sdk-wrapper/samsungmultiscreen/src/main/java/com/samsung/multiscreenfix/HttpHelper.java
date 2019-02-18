//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.samsung.multiscreenfix.util.JSONUtil;
import com.samsung.multiscreenfix.util.RunUtil;
import com.samsung.multiscreenfix.util.HttpUtil.ResultCreator;

import java.lang.*;
import java.util.Map;

class HttpHelper {
    HttpHelper() {
    }

    static StringCallback createHttpCallback(final ResultCreator<?> resultCreator, final Result callback) {
        return new StringCallback() {
            public void onCompleted(final Exception exception, final AsyncHttpResponse response, String data) {
                Runnable runnable;
                if(exception != null) {
                    runnable = new Runnable() {
                        public void run() {
                            callback.onError(Error.create(exception));
                        }
                    };
                } else {
                    try {
                        final long e = (long)response.code();
                        final Map map = JSONUtil.parse(data);
                        if(e != 200L) {
                            runnable = new Runnable() {
                                public void run() {
                                    Error error;
                                    if(map != null) {
                                        error = Error.create(e, map);
                                    } else {
                                        error = Error.create(response);
                                    }

                                    callback.onError(error);
                                }
                            };
                        } else {
                            runnable = new Runnable() {
                                public void run() {
                                    try {
                                        callback.onSuccess(resultCreator.createResult(map));
                                    } catch (Exception var2) {
                                        callback.onError(Error.create(var2));
                                    }

                                }
                            };
                        }
                    } catch (final Exception var8) {
                        runnable = new Runnable() {
                            public void run() {
                                callback.onError(Error.create(var8));
                            }
                        };
                    }
                }

                RunUtil.runOnUI(runnable);
            }
        };
    }
}
