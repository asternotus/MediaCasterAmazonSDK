package com.connectsdk.service.roku.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for sent/received messages for the Roku Receiver
 * Created by Bojan Cvetojevic on 11/28/2017.
 */

public abstract class AbstractRokuMessage implements Serializable {

    protected Map<String, Object> data = new HashMap<>();

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Returns the value for the given key from the data field if it exits or null
     * @param key
     * @return
     */
    public Object getDataVal(String key) {
        if (data != null
                && data.containsKey(key)) {
            return data.get(key);
        }

        return null;
    }

    public String getDataValAsString(String key) {
        if (data != null
                && data.containsKey(key)) {
            return (String) data.get(key);
        }

        return null;
    }

    public Long getDataValAsLong(String key) {
        if (data != null
                && data.containsKey(key)) {
            return (Long) data.get(key);
        }

        return null;
    }

    public Double getDataValAsDouble(String key) {
        if (data != null
                && data.containsKey(key)) {
            return (Double) data.get(key);
        }

        return null;
    }

    public Integer getDataValAsInteger(String key) {
        if (data != null
                && data.containsKey(key)) {
            return (Integer) data.get(key);
        }

        return null;
    }
}
