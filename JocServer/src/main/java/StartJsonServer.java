package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.jdbc.PlayerHibernateRepository;
import org.example.jdbc.GameHibernateRepository;
import org.example.utils.AbstractServer;
import org.example.utils.GameJsonConcurrentServer;
import org.example.utils.ServerException;

import java.io.IOException;
import java.util.Properties;

public class StartJsonServer {
    private static final Logger logger = LogManager.getLogger(StartJsonServer.class);
    private static int defaultPort = 55555;

    public static void main(String[] args) {
        // Încarcă configurațiile serverului
        Properties serverProps = new Properties();
        try {
            serverProps.load(StartJsonServer.class.getResourceAsStream("/gameserver.properties"));
            logger.info("Server properties loaded: {}", serverProps);
        } catch(IOException e) {
            logger.error("Cannot find gameserver.properties: " + e);
            return;
        }

        int port = defaultPort;
        String portProp = serverProps.getProperty("server.port");
        if (portProp != null) {
            port = Integer.parseInt(portProp);
        }

        // Inițializează repository-urile și implementarea serviciilor
        PlayerRepo playerRepo = new PlayerHibernateRepository();
        GameRepo gameRepo = new GameHibernateRepository();
        IGameServices gameService = new GameServicesImpl(playerRepo, gameRepo);

        // Pornește serverul concurent de sockets
        AbstractServer server = new GameJsonConcurrentServer(port, gameService);
        try {
            server.start();
        } catch(ServerException e) {
            logger.error("Error starting the game server: " + e.getMessage());
        }
    }
}
