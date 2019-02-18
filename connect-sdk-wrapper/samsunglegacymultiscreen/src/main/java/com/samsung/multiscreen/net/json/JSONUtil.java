//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public JSONUtil() {
    }

    public static Map<String, Object> parse(String data) {
        Map map = null;
        if(data == null) {
            return containerFactory.createObjectContainer();
        } else {
            try {
                JSONParser e = new JSONParser();
                map = (Map)e.parse(data, containerFactory);
            } catch (ClassCastException var3) {
                map = containerFactory.createObjectContainer();
            } catch (ParseException var4) {
                map = containerFactory.createObjectContainer();
            }

            return map;
        }
    }

    public static List<Map<String, Object>> parseList(String data) {
        List list = null;
        if(data == null) {
            return containerFactory.creatArrayContainer();
        } else {
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
}
