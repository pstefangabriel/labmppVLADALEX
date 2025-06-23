package org.example.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.IGameServices;
import org.example.GameException;
import org.example.PlayerDTO;

public class SignInController {
    @FXML private TextField aliasField;
    @FXML private TextField passwordField;  // (password not used in this context)

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
            // Authenticate with server (login) using the provided alias
            //PlayerDTO playerData = server.login(alias, null);  // pass null as no UI observer for login
            // If login succeeds, switch to the main game scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game-view.fxml"));
            Parent gameLayout = loader.load();
            GameController gameController = loader.getController();
            PlayerDTO playerData = server.login(alias, gameController);
            // Set the service and current player info (including ID) in the game controller
            gameController.setService(server, primaryStage, playerData);
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
