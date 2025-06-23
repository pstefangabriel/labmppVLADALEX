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

    // Connected clients (observers), keyed by player ID
    private final Map<Long, IGameObserver> loggedClients = new ConcurrentHashMap<>();
    // Active games per player, keyed by player ID
    private final Map<Long, GameSession> activeGames = new ConcurrentHashMap<>();

    private final int defaultThreadsNo = 3;

    // Inner class for a game session
//    private static class GameSession {
//        boolean[][] holes;
//        int nextRow;
//        int currentPoints;
//        Instant startTime;
//        List<Move> moves;
//        GameSession(boolean[][] holes) {
//            this.holes = holes;
//            this.nextRow = 1;
//            this.currentPoints = 0;
//            this.startTime = Instant.now();
//            this.moves = new ArrayList<>();
//        }
//    }

    public GameServicesImpl(PlayerRepo playerRepo, GameRepo gameRepo) {
        this.playerRepo = playerRepo;
        this.gameRepo = gameRepo;
    }

    @Override
    public synchronized PlayerDTO login(String playerAlias, IGameObserver client) throws GameException {
        if (playerAlias == null || playerAlias.isEmpty()) {
            throw new GameException("Alias cannot be empty.");
        }
        Player player = playerRepo.findByName(playerAlias);
        if (player == null) {
            player = new Player(playerAlias);
            playerRepo.save(player);
            logger.info("New player registered: {}", playerAlias);
        }
        if (loggedClients.containsKey(player.getId())) {
            throw new GameException("Player '" + playerAlias + "' is already logged in.");
        }
        loggedClients.put(player.getId(), client);
        logger.info("Player {} logged in.", playerAlias);
        // Return player data (ID and alias) so client knows its unique ID
        return new PlayerDTO(player.getId(), playerAlias);
    }

    @Override
    public synchronized void startGame(Long playerId) throws GameException {
        boolean[][] holes = generateHoles();
        GameSession session = new GameSession(holes);
        activeGames.put(playerId, session);
        logger.info("Game started for player ID {}: holes generated.", playerId);
    }

    @Override
    public synchronized GameDTO makeGuess(Long playerId, int row, int col) throws GameException {
        GameSession session = activeGames.get(playerId);
        if (session == null) {
            throw new GameException("No active game for player.");
        }
        if (row != session.nextRow) {
            throw new GameException("Invalid move: it's not time for row " + row);
        }
        logger.info("Player ID {} chose position ({}, {})", playerId, row, col);
        // Record the move
        session.moves.add(new Move(null, row, col));

        if (session.holes[row-1][col-1]) {
            // Hit a hole – game over
            int finalPoints = session.currentPoints;
            Player player = playerRepo.find(playerId);
            int duration = (int) Duration.between(session.startTime, Instant.now()).getSeconds();
            Game game = new Game(player, finalPoints, duration);
            // Save all moves made
            for (Move m : session.moves) {
                game.addMove(new Move(game, m.getRow(), m.getCol()));
            }
            // Save all holes configuration
            addHolesToGame(game, session.holes);
            gameRepo.add(game);
            long rank = gameRepo.getRankForGame(game);
            notifyLeaderboardUpdatedAsync();
            activeGames.remove(playerId);
            throw new GameException("Game over! Ai căzut într-o groapă la linia " + row +
                    ". Scor: " + finalPoints + " puncte. " +
                    "Loc în clasament: " + rank);
        } else {
            // Safe move
            session.currentPoints += row;
            if (row == 4) {
                // Last row completed – player wins
                int finalPoints = session.currentPoints;
                int duration = (int) Duration.between(session.startTime, Instant.now()).getSeconds();
                Player player = playerRepo.find(playerId);
                Game game = new Game(player, finalPoints, duration);
                for (Move m : session.moves) {
                    game.addMove(new Move(game, m.getRow(), m.getCol()));
                }
                addHolesToGame(game, session.holes);
                gameRepo.add(game);
                long rank = gameRepo.getRankForGame(game);
                notifyLeaderboardUpdatedAsync();
                activeGames.remove(playerId);
                logger.info("Player ID {} WON the game! Points={}, Duration={}s, Rank={}",
                        playerId, finalPoints, duration, rank);
                // Return final game stats (player alias, points, duration, rank)
                return new GameDTO(game.getId(), player.getName(), finalPoints, duration, (int) rank);
            } else {
                // Continue to next row
                session.nextRow = row + 1;
                return null;
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
    public synchronized void logout(Long playerId, IGameObserver client) throws GameException {
        loggedClients.remove(playerId);
        activeGames.remove(playerId);
        logger.info("Player with ID {} logged out.", playerId);
    }

    // --------- Helper methods ----------
    private boolean[][] generateHoles() {
        boolean[][] holes = new boolean[4][4];
        Random rand = new Random();
        // One hole per row
        for (int row = 0; row < 4; row++) {
            int col = rand.nextInt(4);
            holes[row][col] = true;
        }
        return holes;
    }

    private void addHolesToGame(Game game, boolean[][] holes) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (holes[i][j]) {
                    game.addHole(new Hole(game, i+1, j+1));
                }
            }
        }
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
