package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.IGameServices;
import org.example.jsonprotocol.GameClientJsonWorker;

import java.net.Socket;

public class GameJsonConcurrentServer extends AbsConcurrentServer {
    private static final Logger logger = LogManager.getLogger(GameJsonConcurrentServer.class);
    private IGameServices gameService;

    public GameJsonConcurrentServer(int port, IGameServices service) {
        super(port);
        this.gameService = service;
        logger.info("GameJsonConcurrentServer initialized");
    }

    @Override
    protected Thread createWorker(Socket client) {
        // Creează un worker (fir de execuție) pentru clientul conectat
        GameClientJsonWorker worker = new GameClientJsonWorker(gameService, client);
        return new Thread(worker);
    }
}
