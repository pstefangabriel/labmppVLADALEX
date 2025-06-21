package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.gui.SignInController;
import org.example.jsonprotocol.GameServicesJsonProxy;

public class StartJsonClientFX extends Application {
    private static final Logger logger = LogManager.getLogger(StartJsonClientFX.class);
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 55555;

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Game Client...");
        // Creează proxy-ul pentru a apela metodele pe server
        IGameServices server = new GameServicesJsonProxy(DEFAULT_HOST, DEFAULT_PORT);
        // Încarcă scena de login
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sign-in-view.fxml"));
        AnchorPane loginLayout = loader.load();
        SignInController signInController = loader.getController();
        signInController.setService(server, primaryStage);

        primaryStage.setTitle("Log In - Game");
        primaryStage.setScene(new Scene(loginLayout));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
