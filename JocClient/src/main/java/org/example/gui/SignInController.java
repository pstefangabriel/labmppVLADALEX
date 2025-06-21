package org.example.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.IGameServices;
import org.example.GameException;
import org.example.Player;

public class SignInController {
    @FXML private TextField aliasField;
    @FXML private TextField passwordField;

    private IGameServices server;
    private Stage primaryStage;

    public void setService(IGameServices service, Stage stage) {
        this.server = service;
        this.primaryStage = stage;
    }

    @FXML
    private void handleStartGameButton() {
        String alias = aliasField.getText().trim();
        if (alias.isEmpty()) {
            MessageAlert.showErrorMessage(null, "Introdu un alias!");
            return;
        }
        try {
            // Autentificare la server (apel login)
            //server.login(alias, null);
            // Login reușit – trecem la scena principală de joc
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game-view.fxml"));
            Parent gameLayout = loader.load();  // FIX: Use Parent or BorderPane, NOT AnchorPane
            GameController gameController = loader.getController();
            // Setează serviciul și jucătorul curent în controllerul de joc
            server.login(alias, gameController);
            gameController.setService(server, primaryStage, new Player(alias));

            primaryStage.setTitle("Game - Player: " + alias);
            primaryStage.setScene(new Scene(gameLayout));
            primaryStage.show();
        } catch(GameException e) {
            MessageAlert.showErrorMessage(null, e.getMessage());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
