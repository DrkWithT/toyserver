package com.drkwitht.util;

import java.net.MalformedURLException;
// import java.net.URL;

/**
 * This data class encapsulates basic fields of an HTTP/1.0 or 1.1 <em>request</em> heading.
 */
public class HTTPHeading {
    private HTTPMethod requestMethod;
    private String requestURL;
    private String httpVersion;

    public HTTPHeading (HTTPMethod method, String rawURL, String version) throws MalformedURLException {
        requestMethod = method;
        requestURL = rawURL;
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
