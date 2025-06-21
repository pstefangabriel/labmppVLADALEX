package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.Socket;

public abstract class AbsConcurrentServer extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(AbsConcurrentServer.class);

    public AbsConcurrentServer(int port) {
        super(port);
        logger.debug("Concurrent server initialized on port {}", port);
    }

    @Override
    protected void processRequest(Socket client) {
        Thread worker = createWorker(client);
        worker.start();  // pornește firul separat pentru client
    }

    protected abstract Thread createWorker(Socket client);
}
