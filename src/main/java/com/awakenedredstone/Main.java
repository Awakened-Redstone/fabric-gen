package com.awakenedredstone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class Main extends Application {
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));

        Scene scene = new Scene(root);
        //scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        /*String styles = """
                -fx-background-color: #16181c;
                -fx-text-color: #b0bac5;
        """;

        root.setStyle(styles);*/

        stage.setTitle("Fabric mod generator");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

