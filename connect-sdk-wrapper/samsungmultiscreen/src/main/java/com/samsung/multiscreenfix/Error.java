//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import com.koushikdutta.async.http.AsyncHttpResponse;
import java.util.Map;

public class Error {
    private final long code;
    private final String name;
    private final String message;

    static Error create(AsyncHttpResponse response) {
        if(response == null) {
            throw new NullPointerException();
        } else {
            return new Error((long)response.code(), "http error", response.message());
        }
    }

    static Error create(String message) {
        return create(-1L, "error", message);
    }

    static Error create(Exception e) {
        if(e == null) {
            throw new NullPointerException();
        } else {
            return new Error(-1L, "error", e.getMessage());
        }
    }

    static Error create(long code, Map<String, Object> data) {
        if(data == null) {
            throw new NullPointerException();
        } else {
            String name = (String)data.get("name");
            if(name == null) {
                name = "error";
            }

            String message = (String)data.get("message");
            return new Error(code, name, message);
        }
    }

    static Error create(long code, String name, String message) {
        if(name != null && message != null) {
            return new Error(code, name, message);
        } else {
            throw new NullPointerException();
        }
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof Error)) {
            return false;
        } else {
            Error other = (Error)o;
            if(!other.canEqual(this)) {
                return false;
            } else if(this.getCode() != other.getCode()) {
                return false;
            } else {
                String this$name = this.getName();
                String other$name = other.getName();
                if(this$name == null) {
                    if(other$name != null) {
                        return false;
                    }
                } else if(!this$name.equals(other$name)) {
                    return false;
                }

                String this$message = this.getMessage();
                String other$message = other.getMessage();
                if(this$message == null) {
                    if(other$message != null) {
                        return false;
                    }
                } else if(!this$message.equals(other$message)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Error;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        long $code = this.getCode();
        int result1 = result * 59 + (int)($code >>> 32 ^ $code);
        String $name = this.getName();
        result1 = result1 * 59 + ($name == null?0:$name.hashCode());
        String $message = this.getMessage();
        result1 = result1 * 59 + ($message == null?0:$message.hashCode());
        return result1;
    }

    public String toString() {
        return "Error(code=" + this.getCode() + ", name=" + this.getName() + ", message=" + this.getMessage() + ")";
    }

    private Error(long code, String name, String message) {
        this.code = code;
        this.name = name;
        this.message = message;
    }

    public long getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public String getMessage() {
        return this.message;
    }
}
