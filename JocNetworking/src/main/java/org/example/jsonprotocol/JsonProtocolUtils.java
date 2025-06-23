package org.example.jsonprotocol;

import org.example.GameDTO;
import org.example.PlayerDTO;

public class JsonProtocolUtils {
    // Request creation methods:
    public static Request createLoginRequest(String alias) {
        Request req = new Request();
        req.setType(RequestType.LOGIN);
        req.setPlayer(new PlayerDTO(alias));
        return req;
    }
    public static Request createStartGameRequest() {
        Request req = new Request();
        req.setType(RequestType.START_GAME);
        return req;
    }
    public static Request createStartGameRequest(PlayerDTO player) {
        Request req = new Request();
        req.setType(RequestType.START_GAME);
        req.setPlayer(player);
        return req;
    }
    public static Request createGuessRequest(int row, int col) {
        Request req = new Request();
        req.setType(RequestType.GUESS);
        req.setRow(row);
        req.setCol(col);
        return req;
    }
    public static Request createGuessRequest(PlayerDTO player, int row, int col) {
        Request req = new Request();
        req.setType(RequestType.GUESS);
        req.setPlayer(player);
        req.setRow(row);
        req.setCol(col);
        return req;
    }
    public static Request createGetLeaderboardRequest() {
        Request req = new Request();
        req.setType(RequestType.GET_LEADERBOARD);
        return req;
    }
    public static Request createLogoutRequest(PlayerDTO player) {
        Request req = new Request();
        req.setType(RequestType.LOGOUT);
        req.setPlayer(player);
        return req;
    }

    // Response creation methods:
    public static Response createOkResponse() {
        Response resp = new Response();
        resp.setType(ResponseType.OK);
        return resp;
    }
    public static Response createErrorResponse(String errorMessage) {
        Response resp = new Response();
        resp.setType(ResponseType.ERROR);
        resp.setErrorMessage(errorMessage);
        return resp;
    }
    public static Response createLeaderboardUpdateResponse() {
        Response resp = new Response();
        resp.setType(ResponseType.LEADERBOARD_UPDATED);
        return resp;
    }
    public static Response createLeaderboardResponse(GameDTO[] games) {
        Response resp = new Response();
        resp.setType(ResponseType.OK);
        resp.setGames(games);
        return resp;
    }
    public static Response createGameFinishedResponse(GameDTO game) {
        Response resp = new Response();
        resp.setType(ResponseType.OK);
        resp.setGame(game);
        return resp;
    }
}
