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
        hasBadRequest = true;
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
            try {
                SimpleRequest request = new SimpleRequest(requestStream);
                
                HTTPHeading heading = request.fetchHeading(); // TODO: use this later for line below!

                System.out.println("Heading: " + heading.fetchMethod() + ";" + heading.fetchURL() + ";" + heading.fetchVersion()); // DEBUG

                // HTTPMethod method = heading.fetchMethod(); // TODO: use this later for supported method checks.
                HTTPHeader aHeader = request.fetchHeader();

                while(aHeader != null) {
                    // stop header scanning at "Foo" (placeholder for empty HTTP line)
                    if (aHeader.fetchName() == "foo") {
                        break;
                    }

                    // require host header
                    System.out.println("Scanning: " + aHeader.fetchName()); // DEBUG

                    if (aHeader.fetchName().equals("Host")) {
                        hasBadRequest = false;
                    } else if (aHeader.fetchName().equals("Content-Length")) {
                        reqBodyLength = numberHTTPField(aHeader.fetchValueAt(0));
                    }
                    
                    aHeader = request.fetchHeader();
                };
                
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

                running = false;
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

        // attempt to close connection on fatal error
        try {
            connection.close();
        } catch (IOException ioError) {
            System.err.println("Server Worker: Closing Err: " + ioError);
        }
    }
}
