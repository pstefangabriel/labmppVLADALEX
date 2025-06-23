package org.example.jsonprotocol;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameClientJsonWorker implements Runnable, IGameObserver {
    private static final Logger logger = LogManager.getLogger(GameClientJsonWorker.class);
    private IGameServices server;
    private Socket connection;
    private BufferedReader input;
    private PrintWriter output;
    private Gson gson = new Gson();
    private volatile boolean connected;
    private BlockingQueue<Response> responseQueue;

    public GameClientJsonWorker(IGameServices server, Socket connection) {
        this.server = server;
        this.connection = connection;
        this.responseQueue = new LinkedBlockingQueue<>();
        try {
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            output = new PrintWriter(connection.getOutputStream(), true);
            connected = true;
        } catch(IOException e) {
            logger.error("Error initializing client worker: ", e);
        }
    }

    @Override
    public void run() {
        try {
            while(connected) {
                String requestJson = input.readLine();
                if (requestJson == null) {
                    // client disconnected
                    logger.info("Client disconnected.");
                    connected = false;
                    break;
                }
                logger.debug("Request received: {}", requestJson);
                Request request = gson.fromJson(requestJson, Request.class);
                Response resp = handleRequest(request);
                if (resp != null) {
                    String responseJson = gson.toJson(resp);
                    logger.debug("Sending response: {}", responseJson);
                    output.println(responseJson);
                }
            }
        } catch(IOException e) {
            logger.error("Communication error: ", e);
        } finally {
            try { input.close(); output.close(); connection.close(); } catch(IOException ex) { logger.error(ex); }
        }
    }

    private Response handleRequest(Request request) {
        Response resp = null;
        try {
            switch(request.getType()) {
                case LOGIN -> {
                    String alias = request.getPlayer().getName();
                    // Perform login and retrieve Player info with ID
                    PlayerDTO playerDto = server.login(alias, this);
                    resp = JsonProtocolUtils.createOkResponse();
                    resp.setPlayer(playerDto);  // include player ID and alias in response
                }
                case START_GAME -> {
                    Long playerId = request.getPlayer().getId();
                    server.startGame(playerId);
                    resp = JsonProtocolUtils.createOkResponse();
                }
                case GUESS -> {
                    Long playerId = request.getPlayer().getId();
                    int row = request.getRow();
                    int col = request.getCol();
                    GameDTO resultGame = server.makeGuess(playerId, row, col);
                    if (resultGame != null) {
                        // Game won – send final game results (points, duration, rank)
                        resp = JsonProtocolUtils.createGameFinishedResponse(resultGame);
                    } else {
                        // Safe move, game continues – just send OK
                        resp = JsonProtocolUtils.createOkResponse();
                    }
                }
                case GET_LEADERBOARD -> {
                    GameDTO[] games = server.getLeaderboard();
                    resp = JsonProtocolUtils.createLeaderboardResponse(games);
                }
                case LOGOUT -> {
                    Long playerId = request.getPlayer().getId();
                    server.logout(playerId, this);
                    connected = false;
                    resp = JsonProtocolUtils.createOkResponse();
                }
            }
        } catch(GameException e) {
            // On exception, send an ERROR response with the message
            resp = JsonProtocolUtils.createErrorResponse(e.getMessage());
        }
        return resp;
    }

    @Override
    public void scoreboardUpdated() throws GameException {
        // Notify client about updated leaderboard (no change needed here)
        try {
            Response update = JsonProtocolUtils.createLeaderboardUpdateResponse();
            String updateJson = gson.toJson(update);
            logger.debug("Sending update notification to client: {}", updateJson);
            output.println(updateJson);
        } catch(Exception e) {
            throw new GameException("Error sending update to client: " + e.getMessage(), e);
        }
    }
}
