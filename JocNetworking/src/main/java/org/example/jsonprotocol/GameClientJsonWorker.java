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
    private IGameServices server;      // referință la logica de joc de pe server
    private Socket connection;
    private BufferedReader input;
    private PrintWriter output;
    private Gson gson = new Gson();
    private volatile boolean connected;

    private BlockingQueue<Response> responseQueue;  // coadă internă pentru răspunsurile sincronizate

    private Player currentPlayer;  // jucătorul asociat acestui client (după login)

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
        // Buclează cât timp conexiunea este activă și primește mesaje de la client
        try {
            while(connected) {
                String requestJson = input.readLine();
                if (requestJson == null) {
                    // stream închis - client deconectat
                    logger.info("Client disconnected.");
                    connected = false;
                    break;
                }
                logger.debug("Request received: {}", requestJson);
                Request request = gson.fromJson(requestJson, Request.class);
                Response response = handleRequest(request);
                if (response != null) {
                    String responseJson = gson.toJson(response);
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
                    //String pass = request.getPassword();
                    // Apelează logica de login din serviciu
                    server.login(alias, this);
                    // După login reușit, stocăm entitatea Player curentă
                    currentPlayer = new Player(alias);
                    resp = JsonProtocolUtils.createOkResponse();
                }
                case START_GAME -> {
                    server.startGame();
                    resp = JsonProtocolUtils.createOkResponse();
                }
                case GUESS -> {
                    int row = request.getRow();
                    int col = request.getCol();
                    GameDTO resultGame = server.makeGuess(row, col);
                    if (resultGame != null) {
                        // Joc câștigat – trimite înapoi detaliile jocului finalizat (punctaj, durată, rank)
                        resp = JsonProtocolUtils.createGameFinishedResponse(resultGame);
                    } else {
                        // Mutare sigură, jocul continuă – răspuns OK fără date
                        resp = JsonProtocolUtils.createOkResponse();
                    }
                }
                case GET_LEADERBOARD -> {
                    GameDTO[] games = server.getLeaderboard();
                    resp = JsonProtocolUtils.createLeaderboardResponse(games);
                }
                case LOGOUT -> {
                    server.logout(currentPlayer, this);
                    connected = false;
                    resp = JsonProtocolUtils.createOkResponse();
                }
            }
        } catch(GameException e) {
            // În caz de excepție, trimitem un răspuns de eroare cu mesajul
            resp = JsonProtocolUtils.createErrorResponse(e.getMessage());
        }
        return resp;
    }

    @Override
    public void scoreboardUpdated() throws GameException {
        // Metoda apelată de server când trebuie notificat clientul despre actualizarea clasamentului
        try {
            // Construim un răspuns special de notificare
            Response update = JsonProtocolUtils.createLeaderboardUpdateResponse();
            // Trimitem direct răspunsul pe fluxul către client (fără a aștepta cerere)
            String updateJson = gson.toJson(update);
            logger.debug("Sending update notification to client: {}", updateJson);
            output.println(updateJson);
        } catch(Exception e) {
            throw new GameException("Error sending update to client: " + e.getMessage(), e);
        }
    }
}
