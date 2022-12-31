package com.drkwitht;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import com.drkwitht.util.HTTPBody;
import com.drkwitht.util.HTTPContentType;
import com.drkwitht.util.HTTPHeader;
import com.drkwitht.util.HTTPHeading;
import com.drkwitht.util.HTTPMethod;

/**
 * Defines an interface to a raw input stream that reads raw HTTP request text. This dynamically reads request lines and parses them with methods <code>fetchHeading</code>, <code>fetchHeader</code>, and <code>fetchBody</code>.
 */
public class SimpleRequest {
    private HashMap<String, HTTPMethod> methodMap;
    private BufferedReader requestStream;

    public SimpleRequest(BufferedReader rawStream) {
        requestStream = rawStream;

        methodMap = new HashMap<String, HTTPMethod>();
        methodMap.put("HEAD", HTTPMethod.HEAD);
        methodMap.put("GET", HTTPMethod.GET);
        methodMap.put("POST", HTTPMethod.POST);
        methodMap.put("UNKNOWN", HTTPMethod.UNKNOWN);
    }

    private HTTPMethod decodeMethod(String name) {
        if (name == null) {
            return HTTPMethod.UNKNOWN;
        }
        
        if (!methodMap.containsKey(name)) {
            return HTTPMethod.UNKNOWN;
        }
        
        return methodMap.get(name);
    }

    public HTTPHeading fetchHeading() throws IOException {
        String rawLine = requestStream.readLine();

        if (rawLine == null) {
            return new HTTPHeading(HTTPMethod.HEAD, "/", "HTTP/1.1"); // default as HEAD request!
        }

        String[] tokens = rawLine.split(" ");

        return new HTTPHeading(decodeMethod(tokens[0]), tokens[1], tokens[2]);
    }

    public HTTPHeader fetchHeader() throws IOException {
        String rawLine = requestStream.readLine();

        if (rawLine == null) {
            return null;
        }

        if (rawLine.isBlank()) {
            return new HTTPHeader("foo", "none");
        }

        String[] tokens = rawLine.trim().split(":");

        return new HTTPHeader(tokens[0], tokens[1]);
    }

    public HTTPBody fetchBody(HTTPContentType type, int contentLength) throws IOException, Exception {
        if (contentLength < 0) {
            throw new Exception("Invalid content length: " + contentLength);
        }

        char[] rawData = new char[contentLength];
        
        if(requestStream.read(rawData, 0, contentLength) == -1) {
            return new HTTPBody(type, null);
        }
        
        return new HTTPBody(type, String.valueOf(rawData));
    }
}
