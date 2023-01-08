package com.drkwitht;

/**
 * Encapsulates HTTP response text data. The internal text can be built before serving as Unicode bytes.
 * @implNote This does not support chunked responses yet. This may be added later for HTTP/1.1 compliance.
 * @author Derek Tan
 */
public class SimpleResponse {
    private StringBuilder msgBuilder;

    public SimpleResponse() {
        msgBuilder = new StringBuilder();
    }

    public void addBody(String content) {
        msgBuilder.append("\r\n").append(content);
    }

    public void addTop(String httpVersion, int statusCode, String statusMessage) {
        msgBuilder.append(httpVersion).append(' ').append(statusCode).append(' ').append(statusMessage).append("\r\n");
    }

    public void addHeader(String name, String value) {
        msgBuilder.append(name).append(": ").append(value).append("\r\n");
    }

    public byte[] asBytes() {
        return msgBuilder.toString().getBytes();
    }
}
