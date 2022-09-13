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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class FXMLController implements Initializable {
    public static FXMLController INSTANCE;

    public static List<String> stableMinecraftVersions = new ArrayList<>();
    public static List<String> minecraftVersions = new ArrayList<>();
    public static List<String> apiVersions = new ArrayList<>();
    public static List<String> loomVersions = new ArrayList<>();
    public static List<String> loaderVersions = new ArrayList<>();

    @FXML
    public ComboBox<String> minecraftVersionComboBox;
    @FXML
    public ComboBox<String> apiVersionComboBox;
    @FXML
    public ComboBox<String> loomVersionComboBox;
    @FXML
    public ComboBox<String> loaderVersionComboBox;
    @FXML
    public TextField modVersionTextField;
    @FXML
    public TextField basePackageNameTextField;
    @FXML
    public TextField archivesBaseNameTextField;
    @FXML
    public TextField modIdTextField;
    @FXML
    public TextField modNameTextField;
    @FXML
    public TextField mainClassNameTextField;
    @FXML
    public TextField authorsTextField;
    @FXML
    public TextField homepageTextField;
    @FXML
    public TextField sourcesTextField;
    @FXML
    public ComboBox<String> licenseComboBox;
    @FXML
    public CheckBox kotlinTemplate;

    @FXML
    public Label message;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        INSTANCE = this;

        if (minecraftVersionComboBox != null) {
            minecraftVersionComboBox.setValue("Loading...");
            UrlQuery.requestJson("https://meta.fabricmc.net/v2/versions/game", JsonArray.class, (jsonArray, code) -> {
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject.get("stable").getAsBoolean())
                        stableMinecraftVersions.add(jsonObject.get("version").getAsString());
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

        if (loomVersionComboBox != null) {
            loomVersionComboBox.setValue("Loading...");
            UrlQuery.request("https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml", (response, integer) -> {
                try {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    Document document = builder.parse(new InputSource(new StringReader(response.replace("\n", ""))));
                    Node versioning = document.getDocumentElement().getElementsByTagName("versioning").item(0);
                    Node latest = versioning.getChildNodes().item(1);
                    Node versions = versioning.getChildNodes().item(5);
                    NodeList versionsChildNodes = versions.getChildNodes();
                    loomVersionComboBox.setValue(latest.getTextContent());
                    for (int i = 1; i < versionsChildNodes.getLength(); i += 2) {
                        loomVersions.add(versionsChildNodes.item(i).getTextContent());
                    }
                    Collections.reverse(loomVersions);
                    loomVersionComboBox.getItems().addAll(loomVersions);
                } catch (ParserConfigurationException | IOException | SAXException exception) {
                    exception.printStackTrace();
                }
            });
        }

        if (loaderVersionComboBox != null) {
            loaderVersionComboBox.setValue("Loading...");
            UrlQuery.requestJson("https://meta.fabricmc.net/v2/versions/loader", JsonArray.class, (jsonArray, code) -> {
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    loaderVersions.add(jsonObject.get("version").getAsString());
                }

                loaderVersionComboBox.getItems().addAll(loaderVersions);
                loaderVersionComboBox.setValue(loaderVersions.get(0));
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
        if (event.getButton() != MouseButton.PRIMARY) return;
        message.setTextFill(Color.BLACK);

        if (parseErrors()) return;

        StringBuilder licenseTmp = new StringBuilder();

        message.setText("Getting license...");
        UrlQuery.requestJsonSync("https://api.github.com/licenses/" + licenseComboBox.getValue(), JsonObject.class, (jsonObject, code) -> {
            if (code != 200) {
                setError("Could not get the license!");
                return;
            } else {
                setMessage("");
            }
            licenseTmp.append(jsonObject.get("body").getAsString());
        });

        String license = licenseTmp.toString();

        message.setText("Downloading template");
        UrlQuery.requestStreamSync("https://github.com/Awakened-Redstone/fabric-mod-template/archive/refs/heads/master.zip", (response, code) -> {
            if (code != 200) {
                setError("Failed to download the template!");
                return;
            }
            setMessage("Decompressing template...");

            try {
                Utils.unzip(response, Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "template"));
                setMessage("");
            } catch (IOException e) {
                setError("Failed to unzip the template!");
                e.printStackTrace();
            }
        });
    }

    TemplateManager templateManager = TemplateManager.create();
        templateManager.createVelocityContext();
    String s = templateManager.generateTemplate(Map.of("NAME", "World"), "Hello ${NAME}");
        System.out.println(s);

    //Cache last generation location at
    //Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "template");
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
