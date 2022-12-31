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

import com.drkwitht.util.HTTPContentType;
import com.drkwitht.util.HTTPHeader;
import com.drkwitht.util.HTTPHeading;
import com.drkwitht.util.HTTPMethod;
import com.drkwitht.util.ServiceIssue;

/**
 * This is the runnable that is encapsulated in a worker thread for the server. A request is handled over a persistent server socket's connections. Request data is checked for basic HTTP syntax, valid methods and routes, and then for any <code>IOException</code>.
 */
public class ServerWorker implements Runnable {
    private Socket connection;
    private BufferedReader requestStream;
    private PrintStream responseStream;

    private DateTimeFormatter timeFormat;
    private Logger workerLogger;

    private ServiceIssue problemCode;
    private String serverName;
    private int reqBodyLength; // todo: use for req handling later

    public ServerWorker(String applicationName, Socket connectingSocket) throws IOException {
        connection = connectingSocket;
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;
        workerLogger = Logger.getLogger(this.getClass().getName());

        serverName = applicationName;
        problemCode = ServiceIssue.BAD_REQUEST;
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

    private SimpleResponse respondToIssues(ServiceIssue issueCode) {
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
            return respondToIssues(ServiceIssue.NO_SUPPORT);
        
        if (!pathValid)
            return respondToIssues(ServiceIssue.NOT_FOUND); // TODO: add favicon serving?
        
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

    @Override
    public void run() {
        try {
            SimpleRequest request = new SimpleRequest(requestStream);

            HTTPHeading heading = request.fetchHeading();
            
            HTTPMethod method = heading.fetchMethod();
            String relURL = heading.fetchURL();
            //String httpName = heading.fetchVersion();

            HTTPHeader aHeader = request.fetchHeader();

            while (aHeader != null) {
                // stop header scanning at "Foo" (placeholder for empty HTTP line)
                if (aHeader.fetchName() == "foo") {
                    break;
                }

                // require Host header
                if (aHeader.fetchName().equals("Host")) {
                    problemCode = ServiceIssue.NONE;
                } else if (aHeader.fetchName().equals("Content-Length")) {
                    reqBodyLength = numberHTTPField(aHeader.fetchValueAt(0));
                }

                aHeader = request.fetchHeader();
            }

            // skip body for now
            request.fetchBody(HTTPContentType.UNKNOWN, reqBodyLength);

            // handle good or bad requests here
            SimpleResponse res;

            if (problemCode == ServiceIssue.NONE) {
                res = respondNormal(method, relURL);
            } else {
                res = respondToIssues(problemCode);
            }

            responseStream.write(res.asBytes());
            responseStream.flush();

        } catch (IOException ioError) {
            workerLogger.warning(ioError.toString());
        } catch (Exception genError) {
            workerLogger.warning(genError.toString());
        }
        
        // attempt to send final ACK by closing anyways...
        try {
            connection.close();
        } catch (IOException ioError) {
            workerLogger.warning(ioError.toString());
        }
    }
}
