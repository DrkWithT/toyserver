package com.drkwitht;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * ToyServer.java
 * This is the driver class containing the static HTTP server's main logic.
 * @author Derek Tan
 */
public class ToyServer 
{
    /// Constants
    private static final int BACKLOG = 10;

    /// Data
    private int port;
    private int backlog; 
    private boolean isListening;

    /// Objects
    /// todo: put collection of worker objects!
    /// todo: put collection of dead worker locations!
    
    // Acceptor
    private class ConnectionEntry implements Runnable {
        // connection entry point
        ServerSocket entrySocket;

        public ConnectionEntry(int portNumber, int backlogCount) throws IOException {
            entrySocket = new ServerSocket(portNumber, backlogCount);
        }

        private void closeSelf() {
            try {
                entrySocket.close();
            } catch (IOException ioError) {
                System.err.println("Closing Err:" + ioError);
            }
        }

        @Override
        public void run() {
            while (isListening) {
                try {
                    Socket connection = entrySocket.accept();

                    // new Thread(new ServerWorker(connection)).start();
                } catch (IOException ioError) {
                    System.err.println("Connection Err: " + ioError);
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
    }

    public static void main( String[] args )
    {
        // try {
        // ToyServer app = new ToyServer(5000, 5);
        // app.listen();
        // } catch (IOException listenError) {
        // System.err.println("Failed to launch server.");
        // }
    }
}
