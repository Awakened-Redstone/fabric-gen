package com.awakenedredstone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class FXMLController implements Initializable {
    public static FXMLController INSTANCE;

    public static List<String> stableMinecraftVersions = new ArrayList<>();
    public static List<String> minecraftVersions = new ArrayList<>();
    public static List<String> apiVersions = new ArrayList<>();

    @FXML public ComboBox<String> minecraftVersionComboBox;
    @FXML public ComboBox<String> apiVersionComboBox;
    @FXML public ComboBox<String> loomVersionComboBox;
    @FXML public TextField modVersionTextField;
    @FXML public TextField basePackageNameTextField;
    @FXML public TextField archivesBaseNameTextField;
    @FXML public TextField modIdTextField;
    @FXML public TextField modNameTextField;
    @FXML public TextField mainClassNameTextField;
    @FXML public TextField authorsTextField;
    @FXML public TextField homepageTextField;
    @FXML public TextField sourcesTextField;
    @FXML public ComboBox<String> licenseComboBox;
    @FXML public CheckBox kotlinTemplate;

    @FXML public Label message;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        INSTANCE = this;

        if (minecraftVersionComboBox != null) {
            minecraftVersionComboBox.setValue("Loading...");
            UrlQuery.requestJson("https://meta.fabricmc.net/v2/versions/game", JsonArray.class, (jsonArray, code) -> {
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject.get("stable").getAsBoolean()) stableMinecraftVersions.add(jsonObject.get("version").getAsString());
                    minecraftVersions.add(jsonObject.get("version").getAsString());
                }

                minecraftVersionComboBox.getItems().addAll(stableMinecraftVersions);
                minecraftVersionComboBox.setValue(stableMinecraftVersions.get(0));
            });
        }

        if (apiVersionComboBox != null) {
            apiVersionComboBox.setValue("Loading...");
            UrlQuery.requestJson("https://api.modrinth.com/v2/project/fabric-api/version", JsonArray.class, (jsonArray, code) -> {
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    apiVersions.add(jsonObject.get("version_number").getAsString());
                }

                apiVersionComboBox.getItems().addAll(apiVersions);
                apiVersionComboBox.setValue(apiVersions.get(0));
            });
        }

        if (licenseComboBox != null) {
            licenseComboBox.setValue("Loading...");
            UrlQuery.requestJson("https://api.github.com/licenses", JsonArray.class, (jsonArray, code) -> {
                licenseComboBox.setValue("");
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    licenseComboBox.getItems().add(jsonObject.get("spdx_id").getAsString());
                }
            });
        }
    }

    public void onMinecraftVersionChange(ActionEvent event) {
        if (!minecraftVersions.contains(minecraftVersionComboBox.getValue())) {
            setError("Invalid Minecraft version!");
            minecraftVersionComboBox.requestFocus();
        } else setMessage("");
    }

    public void onGenerateProjectMouseReleased(MouseEvent event) {
        message.setTextFill(Color.BLACK);
        message.setText("Getting license...");

        if (parseErrors()) return;

        UrlQuery.requestJson("https://api.github.com/licenses/" + licenseComboBox.getValue(), JsonObject.class, (jsonObject, code) -> {
            if (code != 200) {
                setError("Could not get the license!");
                return;
            } else {
                setMessage("Project generated!");
            }
            String body = jsonObject.get("body").getAsString();
            System.out.println(body);
        });

        {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://api.github.com/licenses/" + licenseComboBox.getValue())).newBuilder();
            Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

            Main.OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() != 200) {
                        Platform.runLater(() -> {
                            message.setTextFill(Color.RED);
                            message.setText("Invalid license inserted!");
                        });
                        return;
                    } else {
                        Platform.runLater(() -> {
                            message.setTextFill(Color.BLACK);
                            message.setText("Project generated!");
                        });
                    }

                    String responseBody = Objects.requireNonNull(response.body()).string();
                    JsonObject responseJson = Main.GSON.fromJson(responseBody, JsonObject.class);
                    String body = responseJson.get("body").getAsString();
                    //System.out.println(body);
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {/**/}
            });
        }
    }

    public boolean parseErrors() {
        if (!minecraftVersions.contains(minecraftVersionComboBox.getValue())) {
            setError("Invalid Minecraft version!");
            minecraftVersionComboBox.requestFocus();
            return true;
        }
        if (!apiVersions.contains(apiVersionComboBox.getValue())) {
            setError("Invalid Fabric API version!");
            apiVersionComboBox.requestFocus();
            return true;
        }
        if (StringUtils.isBlank(licenseComboBox.getValue())) {
            setError("Please insert a license!");
            licenseComboBox.requestFocus();
            return true;
        }
        return false;
    }

    public void setMessage(String message) {
        setMessage(message, Color.BLACK);
    }

    public void setError(String message) {
        setMessage(message, Color.RED);
    }

    public void setMessage(String message, Color color) {
        Runnable task = () -> {
            this.message.setTextFill(color);
            this.message.setText(message);
        };

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(task);
        } else task.run();
    }
}
