package org.example.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.*;

public class GameController implements IGameObserver {
    @FXML private GridPane boardGrid;
    @FXML private ListView<String> leaderboardList;

    private IGameServices server;
    private Stage primaryStage;
    private PlayerDTO currentPlayer;  // store current player's ID and alias

    // 4x4 grid of buttons for the game board
    private Button[][] boardButtons = new Button[4][4];
    private int currentRowIndex = 0;  // current row (0-based) that the player is attempting

    public void setService(IGameServices service, Stage stage, PlayerDTO player) throws GameException {
        this.server = service;
        this.primaryStage = stage;
        this.currentPlayer = player;
        // Initialize board and leaderboard, then start a new game for this player
        initializeBoard();
        refreshLeaderboard();
        server.startGame(currentPlayer.getId());
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
        // Ignore clicks on rows that are not the current active row
        if (rowIndex != currentRowIndex) {
            return;
        }
        try {
            // Attempt the chosen cell (rowIndex+1, colIndex+1 because server expects 1-4)
            GameDTO result = server.makeGuess(currentPlayer.getId(), rowIndex+1, colIndex+1);
            // Move was processed successfully by server
            if (result == null) {
                // Safe move, game continues
                boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #7fff7f;");
                // Disable all buttons in this row (prevent repeated clicks on the same row)
                for (int c = 0; c < 4; c++) {
                    boardButtons[rowIndex][c].setDisable(true);
                }
                // advance to next row
                currentRowIndex++;
            } else {
                // result not null -> game won
                boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #7fff7f;");
                for (int c = 0; c < 4; c++) {
                    boardButtons[rowIndex][c].setDisable(true);
                }
                // Show congratulations with score and rank
                String msg = "Felicitări! Ai traversat toate cele 4 rânduri.\n" +
                        "Scor obținut: " + result.getPoints() + " puncte.\n" +
                        "Loc în clasament: " + result.getRank() + ".";
                MessageAlert.showInfoMessage(null, msg);
                // Leaderboard will update via scoreboardUpdated notification from server
            }
        } catch(GameException e) {
            // Error means the player fell in a hole or other issue
            boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #ff4c4c;");  // red for hole
            for (int c = 0; c < 4; c++) boardButtons[rowIndex][c].setDisable(true);
            MessageAlert.showErrorMessage(null, e.getMessage());
        }
    }

    @Override
    public void scoreboardUpdated() throws GameException {
        // Server notified that a new game was added to leaderboard; refresh the list on the UI thread
        Platform.runLater(this::refreshLeaderboard);
    }

    @FXML
    private void handleNewGameButton() {
        try {
            // Reset board and start a new game for the current player
            initializeBoard();
            server.startGame(currentPlayer.getId());
        } catch(GameException e) {
            MessageAlert.showErrorMessage(null, "Eroare la pornirea unui nou joc: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogoutButton() {
        try {
            server.logout(currentPlayer.getId(), this);
            MessageAlert.showInfoMessage(null, "Te-ai deconectat.");
            // Return to login window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sign-in-view.fxml"));
            AnchorPane loginLayout = loader.load();
            SignInController signInCtrl = loader.getController();
            signInCtrl.setService(server, primaryStage);
            primaryStage.setTitle("Log In - Game");
            primaryStage.setScene(new Scene(loginLayout));
        } catch(Exception e) {
            MessageAlert.showErrorMessage(null, "Logout failed: " + e.getMessage());
        }
    }
}
