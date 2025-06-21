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
    private Player currentPlayer;

    // Matricea butoanelor pentru tabla de joc (4x4)
    private Button[][] boardButtons = new Button[4][4];
    private int currentRowIndex = 0;  // indexul (0-based) al rândului curent așteptat

    public void setService(IGameServices service, Stage stage, Player player) throws GameException {
        this.server = service;
        this.primaryStage = stage;
        this.currentPlayer = player;
        // Înregistrează acest controller ca observer pentru update-uri de la server
        // (Serverul a primit this.observer prin login, deci notificările ne vor parveni)
        // Inițializează tabla și clasificarea inițială
        initializeBoard();
        refreshLeaderboard();
        // Pornește un nou joc automat după login
        server.startGame();
    }

    private void initializeBoard() {
        // Construiește dinamically grid-ul de butoane 4x4 și le atașează evenimente
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
        // Ignoră click-urile pe rânduri care nu sunt active (utilizatorul trebuie să aleagă în ordine)
        if (rowIndex != currentRowIndex) {
            return;
        }
        try {
            GameDTO result = server.makeGuess(rowIndex+1, colIndex+1);
            // Mutarea a fost procesată cu succes de server
            if (result == null) {
                // Mutare sigură, dar jocul nu s-a terminat încă
                boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #7fff7f;");  // verde (safe)
                // Dezactivează toate butoanele de pe rândul curent (să nu mai poată fi apasate din nou)
                for (int c = 0; c < 4; c++) {
                    boardButtons[rowIndex][c].setDisable(true);
                }
                // trece la următorul rând
                currentRowIndex++;
            } else {
                // result nu e null -> joc câștigat
                boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #7fff7f;");
                for (int c = 0; c < 4; c++) boardButtons[rowIndex][c].setDisable(true);
                // Afișează mesaj de felicitare cu scor și clasament
                String msg = "Felicitări! Ai traversat toate cele 4 rânduri.\n" +
                        "Scor obținut: " + result.getPoints() + " puncte.\n" +
                        "Loc în clasament: " + result.getRank() + ".";
                MessageAlert.showInfoMessage(null, msg);
                // (Clasamentul se va actualiza prin notificarea scoreboardUpdated, apelată deja de server)
            }
        } catch(GameException e) {
            // Eroare = a căzut în groapă sau altă problemă
            // Dacă e mesaj de pierdere, evidențiem poziția respectivă ca fiind groapă (roșu)
            boardButtons[rowIndex][colIndex].setStyle("-fx-background-color: #ff4c4c;");  // roșu pentru groapă
            for (int c = 0; c < 4; c++) boardButtons[rowIndex][c].setDisable(true);
            MessageAlert.showErrorMessage(null, e.getMessage());
        }
    }

    @Override
    public void scoreboardUpdated() throws GameException {
        // Serverul a notificat că s-a adăugat un joc nou în clasament; actualizăm lista
        Platform.runLater(() -> {
            refreshLeaderboard();
        });
    }

    @FXML
    private void handleNewGameButton() {
        try {
            // Resetează tabla și începe un joc nou
            initializeBoard();
            server.startGame();
        } catch(GameException e) {
            MessageAlert.showErrorMessage(null, "Eroare la pornirea unui nou joc: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogoutButton() {
        try {
            server.logout(currentPlayer, this);
            MessageAlert.showInfoMessage(null, "Te-ai deconectat.");
            // Revenim la fereastra de login
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
