package com.drkwitht.util;

/**
 * This data class encapsulates basic fields of an HTTP/1.0 or 1.1 <em>request</em> heading.
 */
public class HTTPHeading {
    
    private HTTPMethod requestMethod;
    private String requestURL;
    private String httpVersion;

    public HTTPHeading (HTTPMethod method, String url, String version) {
        requestMethod = method;
        requestURL = url;
        httpVersion = version;
    }

    public HTTPMethod fetchMethod() {
        return requestMethod;
    }

    public String fetchURL() {
        return requestURL;
    }

    public String fetchVersion() {
        return httpVersion;
    }
}
