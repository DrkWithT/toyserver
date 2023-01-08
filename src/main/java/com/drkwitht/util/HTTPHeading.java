package com.drkwitht.util;

/**
 * This data class encapsulates basic fields of an HTTP/1.0 or 1.1 <em>request</em> heading.
 * @author Derek Tan
 */
public class HTTPHeading {
    private HTTPMethod requestMethod;
    private String requestURL;
    private String httpVersion;

    public HTTPHeading (HTTPMethod method, String rawURL, String version) {
        requestMethod = method;
        requestURL = rawURL;
        httpVersion = version;
    }

    public HTTPMethod fetchMethod() {
        return requestMethod;
    }

    public String fetchURI() {
        return requestURL;
    }

    public String fetchVersion() {
        return httpVersion;
    }
}
