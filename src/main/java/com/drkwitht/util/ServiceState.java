package com.drkwitht.util;

/**
 * Enumerates <code>ServerWorker</code> state codes. These state codes represent stages of processing a web request with a response.
 * @author Derek Tan
 */
public enum ServiceState {
    START,
    GET_REQUEST,
    GET_HEADING,
    GET_HEADERS,
    GET_BODY,
    RESPOND,
    STOP
}
