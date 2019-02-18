//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.json.simple;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class JSONValue {
    public JSONValue() {
    }

    public static Object parse(Reader in) {
        try {
            JSONParser e = new JSONParser();
            return e.parse(in);
        } catch (Exception var2) {
            return null;
        }
    }

    public static Object parse(String s) {
        StringReader in = new StringReader(s);
        return parse((Reader)in);
    }

    public static Object parseWithException(Reader in) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        return parser.parse(in);
    }

    public static Object parseWithException(String s) throws ParseException {
        JSONParser parser = new JSONParser();
        return parser.parse(s);
    }

    public static void writeJSONString(Object value, Writer out) throws IOException {
        if(value == null) {
            out.write("null");
        } else if(value instanceof String) {
            out.write(34);
            out.write(escape((String)value));
            out.write(34);
        } else if(value instanceof Double) {
            if(!((Double)value).isInfinite() && !((Double)value).isNaN()) {
                out.write(value.toString());
            } else {
                out.write("null");
            }

        } else if(!(value instanceof Float)) {
            if(value instanceof Number) {
                out.write(value.toString());
            } else if(value instanceof Boolean) {
                out.write(value.toString());
            } else if(value instanceof JSONStreamAware) {
                ((JSONStreamAware)value).writeJSONString(out);
            } else if(value instanceof JSONAware) {
                out.write(((JSONAware)value).toJSONString());
            } else if(value instanceof Map) {
                JSONObject.writeJSONString((Map)value, out);
            } else if(value instanceof List) {
                JSONArray.writeJSONString((List)value, out);
            } else {
                out.write(value.toString());
            }
        } else {
            if(!((Float)value).isInfinite() && !((Float)value).isNaN()) {
                out.write(value.toString());
            } else {
                out.write("null");
            }

        }
    }

    public static String toJSONString(Object value) {
        return value == null?"null":(value instanceof String?"\"" + escape((String)value) + "\"":(value instanceof Double?(!((Double)value).isInfinite() && !((Double)value).isNaN()?value.toString():"null"):(value instanceof Float?(!((Float)value).isInfinite() && !((Float)value).isNaN()?value.toString():"null"):(value instanceof Number?value.toString():(value instanceof Boolean?value.toString():(value instanceof JSONAware?((JSONAware)value).toJSONString():(value instanceof Map?JSONObject.toJSONString((Map)value):(value instanceof List?JSONArray.toJSONString((List)value):value.toString()))))))));
    }

    public static String escape(String s) {
        if(s == null) {
            return null;
        } else {
            StringBuffer sb = new StringBuffer();
            escape(s, sb);
            return sb.toString();
        }
    }

    static void escape(String s, StringBuffer sb) {
        for(int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch(ch) {
            case '\b':
                sb.append("\\b");
                continue;
            case '\t':
                sb.append("\\t");
                continue;
            case '\n':
                sb.append("\\n");
                continue;
            case '\f':
                sb.append("\\f");
                continue;
            case '\r':
                sb.append("\\r");
                continue;
            case '\"':
                sb.append("\\\"");
                continue;
            case '/':
                sb.append("\\/");
                continue;
            case '\\':
                sb.append("\\\\");
                continue;
            }

            if(ch >= 0 && ch <= 31 || ch >= 127 && ch <= 159 || ch >= 8192 && ch <= 8447) {
                String ss = Integer.toHexString(ch);
                sb.append("\\u");

                for(int k = 0; k < 4 - ss.length(); ++k) {
                    sb.append('0');
                }

                sb.append(ss.toUpperCase());
            } else {
                sb.append(ch);
            }
        }

    }
}
