package org.example;

public interface IGameServices {
    void login(String playerAlias, IGameObserver client) throws GameException;
    void startGame() throws GameException;
    /**
     * Procesează alegerea unei poziții (mutare) pe tablă de către jucător.
     * @param row linia aleasă (1-4)
     * @param col coloana aleasă (1-4)
     * @return un obiect GameDTO dacă jocul s-a încheiat cu succes (ajuns la linia 4),
     *         sau null dacă mutarea a fost sigură și jocul continuă.
     * @throws GameException dacă mutarea duce într-o groapă (joc pierdut) sau este invalidă.
     */
    GameDTO makeGuess(int row, int col) throws GameException;
    GameDTO[] getLeaderboard() throws GameException;
    void logout(Player player, IGameObserver client) throws GameException;
}
