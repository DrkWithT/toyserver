package com.drkwitht;

/**
 * This is a data class encapsulating a single HTTP request header's information. Supports basic, multi-part header values.
 */
public class HTTPHeader {
    private String name;
    private String[] data;

    public HTTPHeader(String rawName, String rawValue) {
        name = rawName;
        data = rawValue.split(",");
    }

    public String fetchName() {
        return name;
    }

    public String fetchValueAt(int index) {
        if (index < 0 || index >= data.length)
            return "";
        
        return data[index];
    }
}
