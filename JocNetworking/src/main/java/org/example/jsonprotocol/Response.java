package org.example.jsonprotocol;

import org.example.GameDTO;
import com.google.gson.annotations.SerializedName;

public class Response {
    private ResponseType type;
    private String errorMessage;
    private GameDTO game;
    private GameDTO[] games;  // pentru clasament (lista de jocuri DTO)

    public ResponseType getType() { return type; }
    public String getErrorMessage() { return errorMessage; }
    public GameDTO getGame() { return game; }
    public GameDTO[] getGames() { return games; }

    public void setType(ResponseType type) { this.type = type; }
    public void setErrorMessage(String msg) { this.errorMessage = msg; }
    public void setGame(GameDTO game) { this.game = game; }
    public void setGames(GameDTO[] games) { this.games = games; }
}
