package org.example;

import java.util.List;

public interface GameRepo {
    Game add(Game game);
    Game find(Long id);
    List<Game> findByPlayer(Player player);
    List<Game> findAllByScore();            // toate jocurile ordonate desc după puncte (și asc după durată)
    long getRankForGame(Game game);
    void addMove(Long gameId, int row, int col);
}
