package com.awakenedredstone.fabrigen;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class JavaFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Exception throwable = null;
        boolean cacheLoaded = false;
        boolean settingsLoaded = false;
        try {
            //noinspection AssignmentUsedAsCondition
            if (cacheLoaded = Constants.CACHE_MANAGER.loadOrCreateCache())
                settingsLoaded = Constants.SETTINGS_MANAGER.loadOrCreateSetting();
        } catch (Exception exception) {
            throwable = exception;
        }

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("scene.fxml")));

        Scene scene = new Scene(root);

        stage.setOnCloseRequest(event -> System.exit(0));

        stage.setTitle("[FabriGen] Fabric mod generator");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
        stage.setScene(scene);
        stage.show();

        if (!cacheLoaded) {
            new ErrorWindow(scene.getWindow(), "Failed to load cache!", throwable).run();
        } else if (!settingsLoaded) {
            new ErrorWindow(scene.getWindow(), "Failed to load settings!", throwable).run();
        }
    }

    public static void init(String[] args) {
        launch(args);
    }

    public static class ErrorWindow implements Runnable {
        private final Window parent;
        private final String message;
        private final @Nullable Throwable throwable;

        public ErrorWindow(Window parent, String message, @Nullable Throwable throwable) {
            this.parent = parent;
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public void run() {
            try {
                Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("error.fxml")));
                Scene scene = new Scene(root);

                final Stage stage = new Stage();
                stage.setTitle("An error occurred!");
                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
                stage.initOwner(parent);
                stage.initStyle(StageStyle.UTILITY);
                stage.initModality(Modality.WINDOW_MODAL);

                VBox rootVBox = (VBox) scene.getRoot();
                VBox vBox = (VBox) rootVBox.getChildren().get(0);
                Label message = (Label) vBox.getChildren().get(0);
                TextArea textArea = (TextArea) vBox.getChildren().get(1);

                message.setText(this.message);
                if (throwable != null) textArea.setText(buildStackTrace(throwable));
                else textArea.setText("");

                stage.setScene(scene);
                stage.showAndWait();
            } catch (IOException ignored) {
            }
        }

        private String buildStackTrace(Throwable throwable) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos);

            throwable.printStackTrace(out);
            ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
            int n = in.available();
            byte[] bytes = new byte[n];
            in.read(bytes, 0, n);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
