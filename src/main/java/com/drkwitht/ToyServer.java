package com.drkwitht;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.drkwitht.resource.StaticResource;
import com.drkwitht.resource.StaticResponder;
import com.drkwitht.util.HTTPContentType;

import java.io.IOException;

/**
 * ToyServer.java
 * This is the driver class containing the static HTTP server's main logic. The server will launch a parallel worker thread for each (likely persistent) connection. Each worker thread is a finite state machine handling requests up to a responding. See the README for more details.
 * @author Derek Tan
 */
public class ToyServer
{
    private static final String APP_NAME = "ToyServer/0.4";

    // Objects
    ArrayList<StaticResponder> handlers;

    /// Data
    private int port;
    private int backlog; 
    private boolean isListening;
    
    /**
     * This special runnable class spawns 1 worker thread per successful connection. However, the conection queue is low to prevent excess worker thread count since this server is a toy.
     * @author Derek Tan
     */
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

                    new Thread(new ServerWorker(APP_NAME, connection, handlers)).start();
                } catch (IOException ioError) {
                    connectionLogger.warning("Connect err: " + ioError);
                }
            }

            closeSelf(); // TODO: this never executes during no-debug. I could add a penalty count to close the server from bad connection requests.
        }
    }

    public ToyServer(int portNumber, int backlogCount, ArrayList<StaticResponder> contentHandlers) {
        if (port >= 0 && port < 65536)
            port = portNumber;
        else
            this.port = 80;
        
        if (backlogCount >= 1)
            backlog = backlogCount;
        else
            backlog = 5;
        
        isListening = false;

        handlers = contentHandlers;
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
            ArrayList<StaticResponder> responders = new ArrayList<>();
            responders.add(new StaticResponder(new String[]{"/"}, new StaticResource(HTTPContentType.TEXT_HTML, "./static/test1.html")));

            ToyServer app = new ToyServer(8080, 5, responders);
            app.listen();
        } catch (IOException listenEx) {
            System.err.println("Failed to launch server by IOException: " + listenEx);
        } catch (Exception otherEx) {
            System.err.println("Failed to launch server by Exception: " + otherEx);
        }
    }
}
