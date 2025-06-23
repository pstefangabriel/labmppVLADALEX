package org.example.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.*;

public class GameController implements IGameObserver {
    @FXML private GridPane boardGrid;
    @FXML private ListView<String> leaderboardList;

    private IGameServices server;
    private Stage primaryStage;
    private PlayerDTO currentPlayer;

    // 4x4 grid of buttons for the game board
    private Button[][] boardButtons = new Button[4][4];
    private int currentRowIndex = 0;  // 0-based row the player is playing
    private boolean gameFinished = false; // blocks clicks after finish

    public void setService(IGameServices service, Stage stage, PlayerDTO player) throws GameException {
        this.server = service;
        this.primaryStage = stage;
        this.currentPlayer = player;
        initializeBoard();
        refreshLeaderboard();
        server.startGame(currentPlayer.getId());
        gameFinished = false;
    }

    private void initializeBoard() {
        boardGrid.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Button btn = new Button();
                btn.setPrefSize(50, 50);
                final int rowIndex = i;
                final int colIndex = j;
                btn.setOnAction(e -> handleCellClick(rowIndex, colIndex));
                boardButtons[i][j] = btn;
                boardGrid.add(btn, j, i);
            }
        }
        currentRowIndex = 0;
        gameFinished = false;
    }

    private void refreshLeaderboard() {
        try {
            GameDTO[] games = server.getLeaderboard();
            leaderboardList.getItems().clear();
            int rank = 1;
            for (GameDTO game : games) {
                String entry = rank + ". " + game.getPlayerAlias() + " - " +
                        game.getPoints() + " pct - " + game.getDuration() + " sec";
                leaderboardList.getItems().add(entry);
                rank++;
            }
        } catch(GameException e) {
            MessageAlert.showErrorMessage(null, "Eroare la obținerea clasamentului: " + e.getMessage());
        }
    }

    private void handleCellClick(int rowIndex, int colIndex) {
        // Ignore clicks after game is finished or on rows that are not the current row
        if (gameFinished || rowIndex != currentRowIndex) {
            return;
        }
        try {
            GameDTO result = server.makeGuess(currentPlayer.getId(), rowIndex+1, colIndex+1);
            // Move processed: safe move or win
            boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #7fff7f;");
            for (int c = 0; c < 4; c++) {
                boardButtons[rowIndex][c].setDisable(true);
            }
            if (result == null) {
                // Safe move, game continues
                currentRowIndex++;
            } else {
                // Win: all rows traversed
                gameFinished = true;
                disableAllBoard();
                String msg = "Felicitări! Ai traversat toate cele 4 rânduri.\n" +
                        "Scor obținut: " + result.getPoints() + " puncte.\n" +
                        "Loc în clasament: " + result.getRank() + ".";
                MessageAlert.showInfoMessage(null, msg);
            }
        } catch(GameException e) {
            // Hit a hole, game over!
            boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #ff4c4c;"); // red
            for (int c = 0; c < 4; c++) {
                boardButtons[rowIndex][c].setDisable(true);
            }
            gameFinished = true;
            disableAllBoard();
            MessageAlert.showErrorMessage(null, e.getMessage());
        }
    }

    private void disableAllBoard() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                boardButtons[i][j].setDisable(true);
            }
        }
    }

    @Override
    public void scoreboardUpdated() throws GameException {
        Platform.runLater(this::refreshLeaderboard);
    }
}
