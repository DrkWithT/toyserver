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
    private Socket connection;
    private BufferedReader requestStream;
    private PrintStream responseStream;
    
    private DateTimeFormatter timeFormat;

    private boolean running;
    private boolean hasBadRequest;
    private String reqHTTPVersion; // todo: check this top request field
    private String reqURL; // todo: check this URL against a mapping of paths to resource files
    private String reqMethod;

    public ServerWorker(Socket connectingSocket) throws IOException {
        connection = connectingSocket;
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;

        running = true;
        hasBadRequest = false;
    }

    private String readHTTPLine() throws IOException {
        String top = requestStream.readLine();

        if (top == null)
            return "";
        
        return top.trim();
    }

    private void ignoreRemainder() throws IOException {
        String tempLine;

        do {
            tempLine = readHTTPLine();
        } while (tempLine != null);
    }

    @Override
    public void run() {
        while (running) {
            try {
                String requestStatus = readHTTPLine();
                String[] statusTokens = requestStatus.split(" ");
                
                // ignore other parts of request
                ignoreRemainder();

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

                // handle unsupported HTTP methods with 405: Method Not Allowed
                hasBadRequest = reqMethod != "GET";

                if (hasBadRequest) {
                    SimpleResponse res = new SimpleResponse();

                    res.addTop("HTTP/1.1", 405, "Method Not Allowed");
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
                System.err.println("Worker err: " + ioError);
                running = false;
            }
        }

        try {
            connection.close();
        } catch (IOException ioError) {
            System.err.println("Worker err: " + ioError);
        }
    }
}
