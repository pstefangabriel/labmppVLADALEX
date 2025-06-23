package org.example.jsonprotocol;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameServicesJsonProxy implements IGameServices {
    private String host;
    private int port;
    private IGameObserver clientObserver;
    private BufferedReader input;
    private PrintWriter output;
    private Gson gsonFormatter;
    private Socket connection;
    private BlockingQueue<Response> qresponses;
    private volatile boolean finished;
    private static final Logger logger = LogManager.getLogger(GameServicesJsonProxy.class);

    public GameServicesJsonProxy(String host, int port) {
        this.host = host;
        this.port = port;
        qresponses = new LinkedBlockingQueue<>();
    }

    @Override
    public PlayerDTO login(String playerAlias, IGameObserver client) throws GameException {
        initializeConnection();
        Request req = JsonProtocolUtils.createLoginRequest(playerAlias);
        sendRequest(req);
        Response response = readResponse();
        if (response.getType() == ResponseType.OK) {
            this.clientObserver = client;
            // Return the logged-in player's info (ID and alias) to the client
            return response.getPlayer();
        } else if (response.getType() == ResponseType.ERROR) {
            closeConnection();
            throw new GameException(response.getErrorMessage());
        }
        return null;  // should not reach here
    }

    @Override
    public void startGame(Long playerId) throws GameException {
        Request req = JsonProtocolUtils.createStartGameRequest(new PlayerDTO(playerId, null));
        sendRequest(req);
        Response response = readResponse();
        if (response.getType() == ResponseType.ERROR) {
            throw new GameException(response.getErrorMessage());
        }
    }

    @Override
    public GameDTO makeGuess(Long playerId, int row, int col) throws GameException {
        Request req = JsonProtocolUtils.createGuessRequest(new PlayerDTO(playerId, null), row, col);
        sendRequest(req);
        Response response = readResponse();
        if (response.getType() == ResponseType.ERROR) {
            throw new GameException(response.getErrorMessage());
        }
        // May be null if game continues, or non-null if game ended
        return response.getGame();
    }

    @Override
    public GameDTO[] getLeaderboard() throws GameException {
        Request req = JsonProtocolUtils.createGetLeaderboardRequest();
        sendRequest(req);
        Response response = readResponse();
        if (response.getType() == ResponseType.ERROR) {
            throw new GameException(response.getErrorMessage());
        }
        return response.getGames();
    }

//    @Override
//    public void logout(Long playerId, IGameObserver client) throws GameException {
//        Request req = JsonProtocolUtils.createLogoutRequest(new PlayerDTO(playerId, null));
//        sendRequest(req);
//        Response response = readResponse();
//        closeConnection();  // close socket on logout
//        if (response.getType() == ResponseType.ERROR) {
//            throw new GameException(response.getErrorMessage());
//        }
//    }

    // ========================= Helpers =============================

    private void closeConnection() {
        finished = true;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (connection != null) connection.close();
            clientObserver = null;  // IMPORTANT: elimină referința
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void sendRequest(Request request) throws GameException {
        String reqLine = gsonFormatter.toJson(request);
        try {
            output.println(reqLine);
            output.flush(); // important!
        } catch (Exception e) {
            throw new GameException("Error sending object " + e);
        }
    }

    private Response readResponse() throws GameException {
        Response response = null;
        try {
            response = qresponses.take();
        } catch (InterruptedException e) {
            throw new GameException("Interrupted while waiting for server response.", e);
        }
        return response;
    }

    private void initializeConnection() throws GameException {
        try {
            gsonFormatter = new Gson();
            connection = new Socket(host, port);
            output = new PrintWriter(connection.getOutputStream());
            output.flush();
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            finished = false;
            startReader();
        } catch (IOException e) {
            throw new GameException("Cannot connect to server: " + e.getMessage(), e);
        }
    }

    private void startReader() {
        Thread tw = new Thread(new ReaderThread());
        tw.setDaemon(true);
        tw.start();
    }

    // ========================= UPDATES & THREAD =============================

    private boolean isUpdate(Response response) {
        return response.getType() == ResponseType.LEADERBOARD_UPDATED;
    }

    private void handleUpdate(Response response) {
        if (response.getType() == ResponseType.LEADERBOARD_UPDATED) {
            logger.debug("Leaderboard update received");
            if (clientObserver != null) {
                try {
                    clientObserver.scoreboardUpdated();
                } catch (Exception e) {
                    logger.error("Error notifying observer: ", e);
                }
            } else {
                logger.warn("Leaderboard update received but no observer registered.");
            }
        }
    }

    private class ReaderThread implements Runnable {
        public void run() {
            while (!finished) {
                try {
                    String responseLine = input.readLine();

                    if (responseLine == null) {
                        logger.debug("Stream closed, terminating reader thread.");
                        finished = true;
                        break; // Exit the loop since the connection is closed.
                    }

                    logger.debug("Response received: {}", responseLine);
                    Response response = gsonFormatter.fromJson(responseLine, Response.class);
                    if (isUpdate(response)) {
                        handleUpdate(response);
                    } else {
                        try {
                            qresponses.put(response);
                        } catch (InterruptedException e) {
                            logger.error("Error putting response in queue", e);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Reading error " + e);
                    finished = true;
                }
            }
        }
    }
}
