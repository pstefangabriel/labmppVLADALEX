package org.example.jsonprotocol;

import org.example.GameDTO;
import org.example.PlayerDTO;
import com.google.gson.annotations.SerializedName;

public class Response {
    private ResponseType type;
    private String errorMessage;
    private GameDTO game;
    private GameDTO[] games;
    private PlayerDTO player;   // Added: player info (id and alias) for login responses

    public ResponseType getType() { return type; }
    public String getErrorMessage() { return errorMessage; }
    public GameDTO getGame() { return game; }
    public GameDTO[] getGames() { return games; }
    public PlayerDTO getPlayer() { return player; }        // Added getter
    public void setType(ResponseType type) { this.type = type; }
    public void setErrorMessage(String msg) { this.errorMessage = msg; }
    public void setGame(GameDTO game) { this.game = game; }
    public void setGames(GameDTO[] games) { this.games = games; }
    public void setPlayer(PlayerDTO player) { this.player = player; }  // Added setter
}
