package org.example;

public interface IGameServices {
    PlayerDTO login(String playerAlias, IGameObserver client) throws GameException;
    void startGame(Long playerId) throws GameException;
    GameDTO makeGuess(Long playerId, int row, int col) throws GameException;
    GameDTO[] getLeaderboard() throws GameException;
    //void logout(Long playerId, IGameObserver client) throws GameException;
}
