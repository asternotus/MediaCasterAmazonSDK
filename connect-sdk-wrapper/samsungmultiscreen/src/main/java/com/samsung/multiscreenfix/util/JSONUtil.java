//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONUtil {
    private static ContainerFactory containerFactory = new ContainerFactory() {
        public List creatArrayContainer() {
            return new ArrayList();
        }

        public Map createObjectContainer() {
            return new HashMap();
        }
    };

    public static Map<String, Object> parse(String data) {
        if(data == null) {
            return containerFactory.createObjectContainer();
        } else {
            JSONParser parser = new JSONParser();

            Map map;
            try {
                map = (Map)parser.parse(data, containerFactory);
            } catch (ClassCastException var4) {
                map = containerFactory.createObjectContainer();
            } catch (ParseException var5) {
                map = containerFactory.createObjectContainer();
            }

            return map;
        }
    }

    public static JSONObject parseObject(String data) {
        JSONParser parser = new JSONParser();

        try {
            return (JSONObject)parser.parse(data);
        } catch (Exception var3) {
            return new JSONObject();
        }
    }

    public static JSONArray parseArray(String data) {
        JSONParser parser = new JSONParser();

        try {
            return (JSONArray)parser.parse(data);
        } catch (Exception var3) {
            return new JSONArray();
        }
    }

    public static List<Map<String, Object>> parseList(String data) {
        if(data == null) {
            return containerFactory.creatArrayContainer();
        } else {
            List list;
            try {
                JSONParser e = new JSONParser();
                list = (List)e.parse(data, containerFactory);
            } catch (ClassCastException var3) {
                list = containerFactory.creatArrayContainer();
            } catch (ParseException var4) {
                list = containerFactory.creatArrayContainer();
            }

            return list;
        }
    }

    public static Map<String, Object> toJSONObjectMap(Object object) {
        return (Map)object;
    }

    public static Map<String, String> toPropertyMap(Object object) {
        return (Map)object;
    }

    public static List<Map<String, Object>> toJSONObjectMapList(Object object) {
        return (List)object;
    }

    public static JSONObject toObject(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putAll(map);
        return jsonObject;
    }

    public static String toJSONString(Map<String, Object> map) {
        return JSONValue.toJSONString(map);
    }

    private JSONUtil() {
    }
}
