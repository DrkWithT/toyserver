package com.drkwitht;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;

/**
 * ToyServer.java
 * This is the driver class containing the static HTTP server's main logic.
 * @author Derek Tan
 */
public class ToyServer
{
    private static final String APP_NAME = "ToyServer/0.1";

    /// Data
    private int port;
    private int backlog; 
    private boolean isListening;
    
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

                    new Thread(new ServerWorker(APP_NAME, connection)).start();

                    System.out.println("Started worker.");
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

    public void toggleListenFlag() {
        isListening = !isListening;
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

            System.out.println("Enter \"close\" to close.");
            Scanner confirm = new Scanner(System.in);
            
            while (true) {
                String userInput = confirm.nextLine();

                if (userInput == "close") {
                    app.toggleListenFlag();
                    confirm.close();
                    break;
                }
            }
        } catch (IOException listenError) {
            System.err.println("Failed to launch server.");
        }
    }
}
