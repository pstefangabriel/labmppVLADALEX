package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GameServicesImpl implements IGameServices {
    private static final Logger logger = LogManager.getLogger(GameServicesImpl.class);

    private PlayerRepo playerRepo;
    private GameRepo gameRepo;

    // Clienți conectați (observeri)
    private final Map<String, IGameObserver> loggedClients = new ConcurrentHashMap<>();
    // Jocuri active per jucător (alias)
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    // Pentru notificări async (leaderboard)
    private final int defaultThreadsNo = 3;

    // Inner class pentru o sesiune de joc
    private static class GameSession {
        boolean[][] holes;          // Matricea gropilor
        int nextRow;                // Următoarea linie de jucat (1-based)
        int currentPoints;
        Instant startTime;
        List<Move> moves;           // Mutări efectuate (rând, coloană)

        GameSession(boolean[][] holes) {
            this.holes = holes;
            this.nextRow = 1;
            this.currentPoints = 0;
            this.startTime = Instant.now();
            this.moves = new ArrayList<>();
        }
    }

    public GameServicesImpl(PlayerRepo playerRepo, GameRepo gameRepo) {
        this.playerRepo = playerRepo;
        this.gameRepo = gameRepo;
    }

    @Override
    public synchronized void login(String playerAlias, IGameObserver client) throws GameException {
        if (playerAlias == null || playerAlias.isEmpty()) {
            throw new GameException("Alias cannot be empty.");
        }
        Player player = playerRepo.findByName(playerAlias);
        if (player == null) {
            player = new Player(playerAlias);
            playerRepo.save(player);
            logger.info("New player registered: {}", playerAlias);
        }
        if (loggedClients.containsKey(playerAlias)) {
            throw new GameException("Player '" + playerAlias + "' is already logged in.");
        }
        loggedClients.put(playerAlias, client);
        logger.info("Player {} logged in.", playerAlias);
    }

    @Override
    public synchronized void startGame() throws GameException {
        String playerAlias = getCurrentPlayerAlias();
        boolean[][] holes = generateHoles();
        // Asigură linia 1 să aibă măcar o poziție sigură
//        if (Arrays.stream(holes[0]).allMatch(b -> b)) {
//            holes[0][0] = false;
//        }
        GameSession session = new GameSession(holes);
        activeGames.put(playerAlias, session);
        logger.info("Game started for {}: holes generated.", playerAlias);
    }

    @Override
    public synchronized GameDTO makeGuess(int row, int col) throws GameException {
        String playerAlias = getCurrentPlayerAlias();
        GameSession session = activeGames.get(playerAlias);
        if (session == null) {
            throw new GameException("No active game for player.");
        }
        if (row != session.nextRow) {
            throw new GameException("Invalid move: it's not time for row " + row);
        }
        logger.info("Player {} chose position ({}, {})", playerAlias, row, col);

        // Înregistrăm mutarea
        session.moves.add(new Move(null, row, col)); // Game-ul va fi setat ulterior la persistare

        if (session.holes[row-1][col-1]) {
            int finalPoints = session.currentPoints;
            Player player = playerRepo.findByName(playerAlias);
            int duration = (int) Duration.between(session.startTime, Instant.now()).getSeconds();
            Game game = new Game(player, finalPoints, duration);

            // Persistăm toate mutările făcute
            for (Move m : session.moves) {
                game.addMove(new Move(game, m.getRow(), m.getCol()));
            }
            // Persistăm toate gropile
            addHolesToGame(game, session.holes);

            gameRepo.add(game);
            long rank = gameRepo.getRankForGame(game);

            notifyLeaderboardUpdatedAsync();
            activeGames.remove(playerAlias);

            throw new GameException("Game over! Ai căzut într-o groapă la linia " + row +
                    ". Scor: " + finalPoints + " puncte. " +
                    "Loc în clasament: " + rank);
        } else {
            session.currentPoints += row;
            if (row == 4) {
                int finalPoints = session.currentPoints;
                int duration = (int) Duration.between(session.startTime, Instant.now()).getSeconds();
                Player player = playerRepo.findByName(playerAlias);
                Game game = new Game(player, finalPoints, duration);

                // Persistăm toate mutările
                for (Move m : session.moves) {
                    game.addMove(new Move(game, m.getRow(), m.getCol()));
                }
                // Persistăm toate gropile
                addHolesToGame(game, session.holes);

                gameRepo.add(game);
                long rank = gameRepo.getRankForGame(game);

                notifyLeaderboardUpdatedAsync();
                activeGames.remove(playerAlias);

                logger.info("Player {} WON the game! Points={}, Duration={}s, Rank={}",
                        playerAlias, finalPoints, duration, rank);

                return new GameDTO(game.getId(), playerAlias, finalPoints, duration, (int) rank);
            } else {
                session.nextRow = row + 1;
                return null; // Jocul continuă
            }
        }
    }

    @Override
    public synchronized GameDTO[] getLeaderboard() throws GameException {
        List<Game> games = gameRepo.findAllByScore();
        return games.stream()
                .map(g -> new GameDTO(g.getId(), g.getPlayer().getName(), g.getPoints(), g.getDuration(), null))
                .toArray(GameDTO[]::new);
    }

    @Override
    public synchronized void logout(Player player, IGameObserver client) throws GameException {
        String alias = player.getName();
        loggedClients.remove(alias);
        activeGames.remove(alias);
        logger.info("Player {} logged out.", alias);
    }

    // --------- Helper methods ----------
    private String getCurrentPlayerAlias() throws GameException {
        if (loggedClients.isEmpty())
            throw new GameException("No player logged in for this session.");
        // Într-un server real, ai avea user/thread map. Acum e un demo cu un user per worker.
        return loggedClients.keySet().iterator().next();
    }

    private boolean[][] generateHoles() {
        boolean[][] holes = new boolean[4][4];
        Random rand = new Random();
        // Un hole pe fiecare linie
        for (int row = 0; row < 4; row++) {
            int col = rand.nextInt(4);
            holes[row][col] = true;
        }
        // Al 5-lea random (poate să dubleze o linie)
//        int extraRow = rand.nextInt(4);
//        int extraCol = rand.nextInt(4);
//        holes[extraRow][extraCol] = true;
        return holes;
    }

    private void addHolesToGame(Game game, boolean[][] holes) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                if (holes[i][j])
                    game.addHole(new Hole(game, i+1, j+1));
    }

    private void notifyLeaderboardUpdatedAsync() {
        ExecutorService executor = Executors.newFixedThreadPool(defaultThreadsNo);
        for (IGameObserver obs : loggedClients.values()) {
            if (obs != null) {
                executor.execute(() -> {
                    try {
                        obs.scoreboardUpdated();
                    } catch (GameException e) {
                        logger.error("Error notifying client: ", e);
                    }
                });
            }
        }
        executor.shutdown();
    }
}
