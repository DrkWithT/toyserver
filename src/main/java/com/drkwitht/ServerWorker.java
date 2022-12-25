package com.drkwitht;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ServerWorker implements Runnable {
    // This is the max UTF-8 char count for any http body to skip or process!
    private static final long MAX_BODY_LEN = 16384;

    private Socket connection;
    private BufferedReader requestStream;
    private PrintStream responseStream;
    
    private DateTimeFormatter timeFormat;

    private boolean running;
    private boolean hasBadRequest;
    private String reqHTTPVersion; // todo: check this top request field
    private String reqURL; // todo: check this URL against a mapping of paths to resource files
    private String reqMethod;
    private long reqBodyLength;

    public ServerWorker(Socket connectingSocket) throws IOException {
        connection = connectingSocket;
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;

        running = true;
        hasBadRequest = false;
        reqBodyLength = 0;
    }

    private String readHTTPLine() throws IOException {
        String top = requestStream.readLine();

        if (top == null)
            return "";
        
        return top.trim();
    }

    private long numberHTTPField(String field) {
        long result;

        try {
            result = Long.parseLong(field);
        } catch (NumberFormatException parseError) {
            result = 0;
        }

        return result;
    }

    // private String[] splitHTTPField(String field) {}

    private void ignoreHTTPBody(long length) throws IOException {
        requestStream.skip(length);
    }

    private void scanRemainder() throws IOException {
        String tempLine;
        String[] lineTokens;

        // skip all headers except Content-Length for now!
        do {
            tempLine = readHTTPLine();
            lineTokens = tempLine.trim().split(":");

            if (lineTokens[0].toLowerCase() == "content-length") {
                reqBodyLength = numberHTTPField(lineTokens[1]);
            }
        } while (tempLine != null && tempLine.length() > 0);

        // skip through body bytes for now!
        if (reqBodyLength == 0) {
            reqBodyLength = MAX_BODY_LEN;
        }

        ignoreHTTPBody(reqBodyLength);
    }

    @Override
    public void run() {
        while (running) {
            try {
                String requestStatus = readHTTPLine();
                String[] statusTokens = requestStatus.split(" ");
                
                // ignore other parts of request except (Content-Length: ??)
                scanRemainder();

                // handle malformed or obsolete requests with 400: Bad Request
                hasBadRequest = statusTokens.length != 3;

                if (hasBadRequest) {
                    SimpleResponse res = new SimpleResponse();

                    res.addTop("HTTP/1.1", 400, "Bad Request");
                    res.addHeader("Server: ", "ToyServer/0.1");
                    res.addHeader("Date: ", timeFormat.format(ZonedDateTime.now()));
                    res.addBody("");

                    responseStream.write(res.asBytes());
                    continue;
                }

                reqMethod = statusTokens[0];
                reqURL = statusTokens[1]; // note: for future use
                reqHTTPVersion = statusTokens[2]; // note: for future use

                // handle unsupported HTTP methods with 501: Not Implemented
                hasBadRequest = reqMethod != "GET" && reqMethod != "HEAD";

                if (hasBadRequest) {
                    SimpleResponse res = new SimpleResponse();

                    res.addTop("HTTP/1.1", 501, "Not Implemented");
                    res.addHeader("Server: ", "ToyServer/0.1");
                    res.addHeader("Date: ", timeFormat.format(ZonedDateTime.now()));
                    res.addBody("");

                    responseStream.write(res.asBytes());
                    continue;
                }

                // otherwise, reply with HTTP/1.1 204 No Content
                SimpleResponse res = new SimpleResponse();

                res.addTop("HTTP/1.1", 204, "No Content");
                res.addHeader("Server: ", "ToyServer/0.1");
                res.addHeader("Date: ", timeFormat.format(ZonedDateTime.now()));
                res.addBody("");

                responseStream.write(res.asBytes());

            } catch (IOException ioError) {
                System.err.println("ServerWorker: I/O Err: " + ioError);
                running = false;
            }
        }

        try {
            connection.close();
        } catch (IOException ioError) {
            System.err.println("ServerWorker: Close err: " + ioError);
        }
    }
}
