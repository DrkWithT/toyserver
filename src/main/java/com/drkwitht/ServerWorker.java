package com.drkwitht;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import com.drkwitht.util.HTTPBody;
import com.drkwitht.util.HTTPContentType;
import com.drkwitht.util.HTTPHeader;
import com.drkwitht.util.HTTPHeading;
import com.drkwitht.util.HTTPMethod;
import com.drkwitht.util.ServiceIssue;
import com.drkwitht.util.ServiceState;

/**
 * This is the runnable that is encapsulated in a worker thread for the server. A request is handled over a persistent server socket's connections. Request data is checked for basic HTTP syntax, valid methods and routes, and then for any <code>IOException</code>.
 */
public class ServerWorker implements Runnable {
    private Socket connection;            // connection
    private BufferedReader requestStream; 
    private PrintStream responseStream;
    private DateTimeFormatter timeFormat; // GMT date generator
    private Logger workerLogger;          // debug message printer

    private SimpleRequest request;
    private HTTPHeading reqHeading;
    private HTTPBody reqBody;         // TODO: process later for more advanced requests

    private ServiceState state;       // service state
    private ServiceIssue problemCode; // client or server problem code
    private int penaltyCount;         // deadly server exception count (2+ means stop!)
    boolean hasKeepAlive;             // persistent connection flag
    private String serverName;        // server software name
    private int reqBodyLength;        // req body byte count

    public ServerWorker(String applicationName, Socket connectingSocket) throws IOException {
        connection = connectingSocket;
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;
        workerLogger = Logger.getLogger(this.getClass().getName());

        request = null;
        reqHeading = null;
        reqBody = null;

        state = ServiceState.START;
        problemCode = ServiceIssue.NONE;
        penaltyCount = 0;
        hasKeepAlive = true;
        serverName = applicationName;
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
        
        while (aHeader != null) {
            // stop header scanning at "Foo" (placeholder for empty HTTP line)
            if (aHeader.fetchName() == "foo") {
                break;
            }

            // require Host header
            if (aHeader.fetchName().equals("Host")) {
                hasHostHeader = true;
            } else if (aHeader.fetchName().equals("Content-Length")) {
                reqBodyLength = numberHTTPField(aHeader.fetchValueAt(0));
            } else if (aHeader.fetchName().equals("Connection")) {
                hasKeepAlive = aHeader.fetchValueAt(0).toLowerCase().equals("keep-alive");
            }

            aHeader = request.fetchHeader();
        }

        if (hasHostHeader) {
            problemCode = ServiceIssue.NONE;
            state = ServiceState.GET_BODY;
        } else {
            problemCode = ServiceIssue.BAD_REQUEST;
            state = ServiceState.RESPOND;
        }
    }

    private SimpleResponse respondAbnormal(ServiceIssue issueCode) {
        int statusNumber = 200;
        String statusMsg = "OK";

        switch (issueCode) {
            case BAD_REQUEST:
                statusNumber = 400;
                statusMsg = "Bad Request";
                break;
            case NOT_FOUND:
                statusNumber = 404;
                statusMsg = "Not Found";
                break;
            case NO_SUPPORT:
                statusNumber = 501;
                statusMsg = "Method Not Supported";
                break;
            case UNKNOWN: // generic server error
            default:
                statusNumber = 500;
                statusMsg = "Internal Server Error";
                break;
        }

        SimpleResponse response = new SimpleResponse();

        response.addTop("HTTP/1.1", statusNumber, statusMsg);
        response.addHeader("Server", serverName);
        response.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
        response.addBody("");

        return response;
    }

    private SimpleResponse respondNormal(HTTPMethod method, String routingPath) {
        boolean methodSupported = method == HTTPMethod.GET; // TODO: add HEAD support!
        boolean pathValid = routingPath.charAt(0) == '/' && !routingPath.contains("favicon.ico");

        if (!methodSupported)
            return respondAbnormal(ServiceIssue.NO_SUPPORT);
        
        if (!pathValid)
            return respondAbnormal(ServiceIssue.NOT_FOUND); // TODO: add favicon serving?
        
        final String testHTML = "<html><head><title>A Page</title></head><body><p>Hello!</p></body></html>"; // TODO: refactor HTML into file fetcher class?

        SimpleResponse response = new SimpleResponse();
        response.addTop("HTTP/1.1", 200, "OK");
        response.addHeader("Server", serverName);
        response.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", "" + testHTML.length());
        response.addBody(testHTML);

        return response;
    }

    private void handleRespondState() throws IOException {
        if (problemCode == ServiceIssue.NONE) {
            responseStream.write(respondNormal(reqHeading.fetchMethod(), reqHeading.fetchURL()).asBytes());
        } else {
            responseStream.write(respondAbnormal(problemCode).asBytes());
        }

        if (!hasKeepAlive || penaltyCount > 1) {
            state = ServiceState.STOP;
        } else {
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
                        reqBody = request.fetchBody(HTTPContentType.UNKNOWN, reqBodyLength);
                        break;
                    case RESPOND:
                        handleRespondState();
                        break;
                    case STOP:
                    default:
                        break;
                }
            } catch (IOException ioEx) {
                workerLogger.warning(ioEx.toString());
                problemCode = ServiceIssue.UNKNOWN;
                state = ServiceState.RESPOND;
                penaltyCount++;
            } catch (Exception basicEx) {
                workerLogger.warning(basicEx.toString());
                problemCode = ServiceIssue.UNKNOWN;
                state = ServiceState.RESPOND;
                penaltyCount++; // track server err count for stopping infinite loop
            }

            break;
        }

        // close connection when worker has finish state
        try {
            connection.close();
        } catch (IOException ioEx) {
            workerLogger.warning(ioEx.toString());
        }
    }
}
