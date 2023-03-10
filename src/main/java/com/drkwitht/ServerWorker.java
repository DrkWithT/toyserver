package com.drkwitht;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.drkwitht.resource.StaticResource;
import com.drkwitht.resource.StaticResponder;
import com.drkwitht.util.HTTPBody;
import com.drkwitht.util.HTTPContentType;
import com.drkwitht.util.HTTPHeader;
import com.drkwitht.util.HTTPHeading;
import com.drkwitht.util.HTTPMethod;
import com.drkwitht.util.ServiceIssue;
import com.drkwitht.util.ServiceState;

/**
 * This is the runnable logic class that is created for a server worker thread. A request is handled over a persistent server socket's connections. Request data is checked for basic HTTP syntax, valid methods and routes, and then for any <code>IOException</code>. State advances through request handling stages, but skips to RESPOND on any internal server exception for status-500 responses. Finally, an excess error count or socket timeout will result in the worker closing. This prevents stalling of the worker loop.
 * @author Derek Tan
 */
public class ServerWorker implements Runnable {
    private final int[] STATUS_CODES = {200, 400, 404, 501, 500};
    private final String[] STATUS_MSGS = {"OK", "Bad Request", "Not Found", "Method Not Supported", "Internal Server Error"};

    private String serverName;            // server software name
    private Socket connection;            // connection
    private BufferedReader requestStream; 
    private PrintStream responseStream;

    private DateTimeFormatter timeFormat; // GMT date generator
    private Logger workerLogger;          // debug message printer

    private ArrayList<StaticResponder> routedHandlers; // handlers for requests by route
    private SimpleRequest request;
    private HTTPHeading reqHeading;
    private HTTPBody reqBody;             // TODO: process for POST requests?

    private ServiceState state;           // service state
    private ServiceIssue problemCode;     // client or server problem code
    private int penaltyCount;             // deadly server exception count (2+ means stop!)
    boolean hasKeepAlive;                 // persistent connection flag
    private int reqBodyLength;            // req body byte count

    public ServerWorker(String applicationName, Socket connectingSocket, ArrayList<StaticResponder> handlers) throws IOException {
        serverName = applicationName;
        connection = connectingSocket;
        connection.setSoTimeout(30000); // timeout: 30s is a kludge for stale connections!
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;
        workerLogger = Logger.getLogger(this.getClass().getName());
        workerLogger.setLevel(Level.ALL);

        routedHandlers = handlers;
        request = null;
        reqHeading = null;
        reqBody = null;

        state = ServiceState.START;
        problemCode = ServiceIssue.NONE;
        penaltyCount = 0;
        hasKeepAlive = true;
        reqBodyLength = 0;
    }

    private int numberHTTPField(String field) {
        int result;

        try {
            result = Integer.parseInt(field);
        } catch (NumberFormatException parseError) {
            result = 0;
        }

        return result;
    }

    private void handleHeaderState() throws IOException {
        boolean hasHostHeader = false;
        HTTPHeader aHeader = request.fetchHeader();
        
        while (!aHeader.fetchName().equals("foo")) {
            // require Host header
            if (aHeader.fetchName().equals("Host")) {
                hasHostHeader = true;
            } else if (aHeader.fetchName().equals("Content-Length")) {
                reqBodyLength = numberHTTPField(aHeader.fetchValueAt(0));
            } else if (aHeader.fetchName().equals("Connection")) {
                hasKeepAlive = aHeader.fetchValueAt(0).matches("keep-alive") || aHeader.fetchValueAt(0).equals("Keep-Alive");
            }

            aHeader = request.fetchHeader();
        }

        if (hasHostHeader) {
            problemCode = ServiceIssue.NONE;
            state = ServiceState.GET_BODY;
        } else {
            problemCode = ServiceIssue.BAD_REQUEST;
            penaltyCount++;
            state = ServiceState.RESPOND;
        }
    }

    private SimpleResponse respondAbnormal() {
        int statusIndex = problemCode.ordinal();
        
        if (statusIndex >= STATUS_CODES.length) {
            statusIndex = ServiceIssue.UNKNOWN.ordinal();
        }

        SimpleResponse response = new SimpleResponse();

        response.addTop("HTTP/1.1", STATUS_CODES[statusIndex], STATUS_MSGS[statusIndex]);
        response.addHeader("Server", serverName);
        response.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
        response.addHeader("Connection", "Keep-Alive");
        response.addBody("");

        return response;
    }

    private SimpleResponse respondNormal(HTTPMethod method, String routingPath) throws IOException {
        boolean hasGET = method == HTTPMethod.GET;
        boolean hasHEAD = method == HTTPMethod.HEAD;
        boolean pathMatched = false;
        StaticResource resource = null;

        if (!hasGET && !hasHEAD) {
            problemCode = ServiceIssue.NO_SUPPORT;
            return respondAbnormal();
        }
        
        for (StaticResponder staticResponder : routedHandlers) {
            if (staticResponder.hasRoute(routingPath)) {
                pathMatched = true;
                resource = staticResponder.yieldResource();
                break;
            }
        }
        
        if (!pathMatched) {
            problemCode = ServiceIssue.NOT_FOUND;
            return respondAbnormal();
        }

        SimpleResponse response = new SimpleResponse();
        response.addTop("HTTP/1.1", 200, "OK");
        response.addHeader("Server", serverName);
        response.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
        response.addHeader("Connection", "Keep-Alive");
        response.addHeader("Content-Type", resource.fetchMIMEType());
        response.addHeader("Content-Length", "" + resource.fetchLength());

        if (!hasHEAD) {
            response.addBody(resource.fetchText()); // include body for responding to GET!
        } else {
            response.addBody(""); // no body for responding to HEAD!
        }

        return response;
    }

    private void handleRespondState() throws IOException {
        if (penaltyCount > 1) {
            workerLogger.info("Ending from excess penalties."); // debug
            state = ServiceState.STOP;
            return;
        }

        if (problemCode == ServiceIssue.NONE) {
            responseStream.writeBytes(respondNormal(reqHeading.fetchMethod(), reqHeading.fetchURI()).asBytes());
        } else {
            responseStream.writeBytes(respondAbnormal().asBytes());
        }

        if (responseStream.checkError()) {
            throw new IOException("Data write failed.");
        } else {
            penaltyCount--; // successful HTTP exchanges reduce penalty count
        }

        if (!hasKeepAlive) {
            workerLogger.info("Ending normally."); // debug
            state = ServiceState.STOP;
        } else {
            workerLogger.info("Continuing normally."); // debug
            state = ServiceState.GET_REQUEST;
        }
    }

    @Override
    public void run() {
        // advance through states for processing requests -> responses
        while (state != ServiceState.STOP) {
            try {
                switch (state) {
                    case START:
                        state = ServiceState.GET_REQUEST;
                        break;
                    case GET_REQUEST:
                        request = new SimpleRequest(requestStream);
                        state = ServiceState.GET_HEADING;
                        problemCode = ServiceIssue.NONE;
                        break;
                    case GET_HEADING:
                        reqHeading = request.fetchHeading();
                        state = ServiceState.GET_HEADERS;
                        break;
                    case GET_HEADERS:
                        handleHeaderState();
                        break;
                    case GET_BODY:
                        reqBody = request.fetchBody(HTTPContentType.TEXT_PLAIN, reqBodyLength);
                        state = ServiceState.RESPOND;
                        break;
                    case RESPOND:
                        handleRespondState();
                        break;
                    case STOP:
                        break;
                    default:
                        state = ServiceState.STOP;
                        break;
                }
            } catch(SocketTimeoutException timeoutEx) {
                workerLogger.info(timeoutEx.toString());
                problemCode = ServiceIssue.NONE;
                state = ServiceState.STOP;
                penaltyCount++;
            } catch (IOException ioEx) {
                workerLogger.warning(ioEx.toString());
                problemCode = ServiceIssue.UNKNOWN;
                state = ServiceState.RESPOND;
                penaltyCount++; // see next comment
            } catch (Exception basicEx) {
                workerLogger.warning(basicEx.toString());
                problemCode = ServiceIssue.UNKNOWN;
                state = ServiceState.RESPOND;
                penaltyCount++; // track server err count for stopping infinite loop
            }
        }

        // close connection when worker has finish state
        try {
            workerLogger.info("Stopped worker.");
            responseStream.flush();
            connection.close();
        } catch (IOException ioEx) {
            workerLogger.warning(ioEx.toString());
        }
    }
}
