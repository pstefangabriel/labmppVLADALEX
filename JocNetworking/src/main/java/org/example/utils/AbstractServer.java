package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public abstract class AbstractServer {
    private int port;
    private ServerSocket server;
    private static final Logger logger = LogManager.getLogger(AbstractServer.class);

    public AbstractServer(int port) {
        this.port = port;
    }

    public void start() throws ServerException {
        try {
            server = new ServerSocket(port);
            logger.info("Server started, listening on port " + port);
            while(true) {
                logger.info("Waiting for clients...");
                Socket client = server.accept();
                logger.info("Client connected: " + client.getInetAddress());
                // procesează fiecare conexiune (sincron sau concurent, în funcție de implementare)
                processRequest(client);
            }
        } catch (IOException e) {
            throw new ServerException("Error starting server: " + e.getMessage(), e);
        } finally {
            stop();
        }
    }

    public void stop() throws ServerException {
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            throw new ServerException("Error stopping server: " + e.getMessage(), e);
        }
    }

    protected abstract void processRequest(Socket client);
}
