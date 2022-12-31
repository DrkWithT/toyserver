package com.drkwitht;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * ToyServer.java
 * This is the driver class containing the static HTTP server's main logic. The server will launch a parallel worker thread for each connection request by TCP. Each HTTP response is handled over the same server socket connection. See the README for more details.
 * @author Derek Tan
 */
public class ToyServer
{
    private static final String APP_NAME = "ToyServer/0.3";

    /// Data
    private int port;
    private int backlog; 
    private boolean isListening;
    
    // Acceptor
    private class ConnectionEntry implements Runnable {
        // connection entry point
        private Logger connectionLogger;
        ServerSocket entrySocket;

        public ConnectionEntry(int portNumber, int backlogCount) throws IOException {
            connectionLogger = Logger.getLogger(this.getClass().getName());
            entrySocket = new ServerSocket(portNumber, backlogCount);
        }

        private void closeSelf() {
            try {
                entrySocket.close();
            } catch (IOException ioError) {
                connectionLogger.warning("Close err: " + ioError);
            }
        }

        @Override
        public void run() {
            while (isListening) {
                try {
                    Socket connection = entrySocket.accept();

                    new Thread(new ServerWorker(APP_NAME, connection)).start();
                } catch (IOException ioError) {
                    connectionLogger.warning("Connect err: " + ioError);
                }
            }

            closeSelf();
        }
    }

    public ToyServer(int portNumber, int backlogCount) {
        if (port >= 0 && port < 65536)
            port = portNumber;
        else
            this.port = 80;
        
        if (backlogCount >= 1)
            backlog = backlogCount;
        else
            backlog = 5;
        
        isListening = false;
    }

    public void listen() throws IOException {
        if (isListening)
            return;
        
        isListening = true;
        new Thread(new ConnectionEntry(port, backlog)).start();

        System.out.println("Started server at port: " + port);
    }

    public static void main( String[] args )
    {
        try {
            ToyServer app = new ToyServer(5000, 5);
            app.listen();
        } catch (IOException listenError) {
            System.err.println("Failed to launch server.");
        }
    }
}
