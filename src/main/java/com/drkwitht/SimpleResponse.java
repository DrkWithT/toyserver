package com.drkwitht;

/**
 * Encapsulates HTTP response text data. The internal text can be built before serving as Unicode bytes.
 * @implNote This does not support chunked responses yet. This may be added later for HTTP/1.1 compliance.
 */
public class SimpleResponse {
    StringBuilder msgBuilder;

    public SimpleResponse() {
        msgBuilder = new StringBuilder();
    }

    private void addBody(String content) {
        msgBuilder.append("\r\n\r\n").append(content);
    }

    public void addTop(String httpVersion, int statusCode, String statusMessage) {
        msgBuilder.append(httpVersion).append(' ').append(statusCode).append(' ').append(statusMessage);
    }

    public void addHeader(String name, String value) {
        msgBuilder.append(name).append(": ").append(value);
    }

    public byte[] asBytes() {
        return msgBuilder.toString().getBytes();
    }
}
