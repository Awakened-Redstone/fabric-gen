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

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Main extends Application {
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path TEMPLATE_PATH = Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "template");
    public static final Path CACHE_PATH = Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "cache");
    public static final Pattern SEMVER = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("scene.fxml")));

        Scene scene = new Scene(root);
        //scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        /*String styles = """
                -fx-background-color: #16181c;
                -fx-text-color: #b0bac5;
        """;

        root.setStyle(styles);*/

        stage.setOnCloseRequest(event -> System.exit(0));

        stage.setTitle("Fabric mod generator");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

