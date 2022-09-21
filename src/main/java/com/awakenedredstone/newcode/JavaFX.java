package com.awakenedredstone.newcode;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class JavaFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Constants.CACHE_CONTROLLER.loadOrCreateCache();

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("scene.fxml")));

        Scene scene = new Scene(root);

        stage.setOnCloseRequest(event -> System.exit(0));

        stage.setTitle("Fabric mod generator");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
        stage.setScene(scene);
        stage.show();
    }

    public static void init(String[] args) {
        launch(args);
    }
}
