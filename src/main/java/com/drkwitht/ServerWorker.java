package com.drkwitht;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.drkwitht.util.HTTPContentType;
import com.drkwitht.util.HTTPHeader;
import com.drkwitht.util.HTTPHeading;
import com.drkwitht.util.HTTPMethod;

public class ServerWorker implements Runnable {
    private Socket connection;
    private BufferedReader requestStream;
    private PrintStream responseStream;
    
    private DateTimeFormatter timeFormat;

    private boolean running;
    private boolean hasInternalError;
    private boolean hasBadRequest;
    private String serverName;
    private int reqBodyLength; // todo: use for req handling later

    public ServerWorker(String applicationName, Socket connectingSocket) throws IOException {
        connection = connectingSocket;
        requestStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        responseStream = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));

        timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME;

        serverName = applicationName;
        running = true;
        hasInternalError = false;
        hasBadRequest = false;
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

    @Override
    public void run() {
        while (running) {
            hasBadRequest = true;
            
            try {
                SimpleRequest request = new SimpleRequest(requestStream);
                
                HTTPHeading heading = request.fetchHeading();

                HTTPMethod method = heading.fetchMethod(); // TODO: use this later for supported method checks.
                HTTPHeader aHeader = request.fetchHeader();

                do {
                    aHeader = request.fetchHeader();

                    // require host header
                    if (aHeader.fetchName() == "host") {
                        hasBadRequest = false;
                    } else if (aHeader.fetchName() == "content-length") {
                        reqBodyLength = numberHTTPField(aHeader.fetchValueAt(0));
                    }
                    
                } while(aHeader != null);
                
                // skip body for now
                request.fetchBody(HTTPContentType.UNKNOWN, reqBodyLength);

                // handle malformed requests with 400: Bad Request    
                if (hasBadRequest) {
                    SimpleResponse res = new SimpleResponse();

                    res.addTop("HTTP/1.1", 400, "Bad Request");
                    res.addHeader("Server", serverName);
                    res.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
                    res.addBody("");

                    responseStream.write(res.asBytes());
                    responseStream.flush();
                } else {
                    // otherwise, reply with HTTP/1.1 200 OK
                    SimpleResponse res = new SimpleResponse();
                
                    res.addTop("HTTP/1.1", 200, "OK");
                    res.addHeader("Server", serverName);
                    res.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
                    res.addHeader("Content-Type", "text/plain");
                    res.addHeader("Content-Length", "11");
                    res.addBody("Hello World");
    
                    responseStream.write(res.asBytes());
                    responseStream.flush();
                }
            
                connection.close();
            } catch (IOException ioError) {
                System.err.println("ServerWorker: I/O Err: " + ioError);
                hasInternalError = true;
            } catch (Exception genError) {
                System.err.println("ServerWorker: Err: " + genError);
                hasInternalError = true;
            } finally {
                if (!hasInternalError) {
                    continue;
                }

                try {
                    SimpleResponse resFinal = new SimpleResponse();

                    resFinal.addTop("HTTP/1.1", 500, "Internal Server Error");
                    resFinal.addHeader("Server", serverName);
                    resFinal.addHeader("Date", timeFormat.format(ZonedDateTime.now()));
                    resFinal.addBody("");

                    responseStream.write(resFinal.asBytes());
                    responseStream.flush();
                    responseStream.close();
                } catch (IOException ioError) {
                    running = false;
                }
            }
        }
    }
}
