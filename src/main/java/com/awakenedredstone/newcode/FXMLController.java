package com.awakenedredstone.newcode;

import com.awakenedredstone.newcode.util.UrlQuery;
import com.awakenedredstone.newcode.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

public class FXMLController implements Initializable {
    public static FXMLController INSTANCE;

    public static List<String> stableMinecraftVersions = new ArrayList<>();
    public static List<String> minecraftVersions = new ArrayList<>();
    public static List<String> apiVersions = new ArrayList<>();
    public static List<String> loomVersions = new ArrayList<>();
    public static List<String> loaderVersions = new ArrayList<>();

    public String licenseContent;
    public String gradleVersion;
    public String javaVersion;
    public String yarnVersion;
    public String generationPath;

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
    public TextField modDescriptionTextField;
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
    public void initialize(URL location, ResourceBundle resources) {
        INSTANCE = this;
        minecraftVersionComboBox.setValue("Loading...");
        apiVersionComboBox.setValue("Loading...");
        loomVersionComboBox.setValue("Loading...");
        loaderVersionComboBox.setValue("Loading...");
        licenseComboBox.setValue("Loading...");
        getGameVersions();
        getLoomVersions();
        getLicenses();
        getVersions("https://meta.fabricmc.net/v2/versions/loader", "version", loaderVersions, loaderVersionComboBox);
        getVersions("https://api.modrinth.com/v2/project/fabric-api/version", "version_number", apiVersions, apiVersionComboBox);
    }

    public void onGenerateProjectOnAction(ActionEvent event) {
        Thread thread = new Thread(this::generateTemplate);
        thread.setDaemon(true);
        thread.start();
    }

    private void generateTemplate() {
        setMessage("");

        if (parseErrors()) return;
        if (!parseData()) return;

        AtomicBoolean hasError = new AtomicBoolean(false);
        setMessage("");

        if (kotlinTemplate.isSelected()) {/**/}
    }

    public boolean parseErrors() {
        if (!minecraftVersions.contains(minecraftVersionComboBox.getValue())) {
            setError("Invalid Minecraft version!");
            requestFocus(minecraftVersionComboBox);
            return true;
        }

        if (!isEmpty(apiVersionComboBox.getEditor()) && !apiVersions.contains(apiVersionComboBox.getValue())) {
            setError("Invalid Fabric API version!");
            requestFocus(apiVersionComboBox);
            return true;
        }

        if (!requireValue(modVersionTextField, "Please insert the mod version!")) {
            if (!Constants.SEMVER_REGEX.matcher(modVersionTextField.getText()).matches()) {
                setError("Please use a version that complies with Semantic Versions!");
                return true;
            }
        } else return true;

        return requireValue(minecraftVersionComboBox, "Please insert the Minecraft version!") ||
                requireValue(basePackageNameTextField, "Please insert the maven group!") ||
                requireValue(archivesBaseNameTextField, "Please insert the archives base name!") ||
                requireValue(modIdTextField, "Please insert the mod ID!") ||
                requireValue(modNameTextField, "Please insert the mod name!") ||
                requireValue(mainClassNameTextField, "Please insert the main class!") ||
                requireValue(licenseComboBox, "Please insert a license!");
    }

    public boolean parseData() {
        AtomicBoolean hasError = new AtomicBoolean(false);
        
        setMessage("Getting license...");
        UrlQuery.requestJsonSync("https://api.github.com/licenses/" + licenseComboBox.getValue(), JsonObject.class, (jsonObject, code) -> {
            if (code != 200) {
                setError("Could not get the license!");
                hasError.set(true);
                return;
            } else {
                setMessage("");
            }
            licenseContent = jsonObject.get("body").getAsString();
            Platform.runLater(() -> licenseComboBox.setValue(jsonObject.get("spdx_id").getAsString()));
        });

        if (hasError.get()) return false;
        setMessage("Getting Java version...");
        UrlQuery.requestJsonSync("http://piston-meta.mojang.com/mc/game/version_manifest_v2.json", JsonObject.class, (jsonObject, code) -> {
            if (code != 200) {
                setError("Failed to get PistonMeta data!");
                hasError.set(true);
                return;
            }

            Optional<JsonObject> versionInfo = StreamSupport.stream(jsonObject.get("versions").getAsJsonArray().spliterator(), true).map(JsonElement::getAsJsonObject)
                    .filter(object -> object.get("id").getAsString().equalsIgnoreCase(minecraftVersionComboBox.getValue()))
                    .findFirst();

            if (versionInfo.isEmpty()) {
                setError("Failed to get PistonMeta url!");
                hasError.set(true);
                return;
            }

            UrlQuery.requestJsonSync(versionInfo.get().get("url").getAsString(), JsonObject.class, (jsonObject2, code2) -> {
                if (code2 != 200) {
                    setError("Failed to get Minecraft version data!");
                    hasError.set(true);
                    return;
                }

                javaVersion = jsonObject2.get("javaVersion").getAsJsonObject().get("majorVersion").getAsString();
                Constants.getPersistentCache().javaVersions.put();
                setMessage("");
            });
        });

        if (hasError.get()) return false;
        setMessage("Getting Gradle version...");
        UrlQuery.requestJsonSync("https://services.gradle.org/versions/current", JsonObject.class, (jsonObject, code) -> {
            if (code != 200) {
                setError("Failed to get Gradle version!");
                hasError.set(true);
                return;
            } else setMessage("");
            gradleVersion = jsonObject.get("version").getAsString();
        });

        if (hasError.get()) return false;
        setMessage("Getting Yarn version...");
        UrlQuery.requestJsonSync("https://meta.fabricmc.net/v2/versions/yarn/" + minecraftVersionComboBox.getValue(), JsonArray.class, (jsonArray, code) -> {
            if (code != 200) {
                setError("Failed to get Yarn version!");
                hasError.set(true);
                return;
            } else setMessage("");
            yarnVersion = jsonArray.get(0).getAsJsonObject().get("version").getAsString();
        });

        if (hasError.get()) return false;
        if (!Constants.TEMPLATE_PATH.toFile().exists()) {
            setMessage("Downloading template");
            UrlQuery.requestJsonSync("https://api.github.com/repos/Awakened-Redstone/fabric-mod-template/releases/latest", JsonObject.class, (jsonObject, code1) -> {
                if (code1 != 200) {
                    setError("Failed to get the template info!");
                    hasError.set(true);
                    return;
                }

                UrlQuery.requestJsonSync(jsonObject.get("assets_url").getAsString(), JsonArray.class, (jsonArray, code2) -> {
                    if (code2 != 200) {
                        setError("Failed to get the template info!");
                        hasError.set(true);
                        return;
                    }

                    try {
                        UrlQuery.requestStreamSync(jsonArray.get(0).getAsJsonObject().get("browser_download_url").getAsString(), (response, code) -> {
                            if (code != 200) {
                                setError("Failed to download the template!");
                                hasError.set(true);
                                return;
                            }

                            setMessage("Decompressing template...");

                            try {
                                Utils.unzip(response, Constants.TEMPLATE_PATH);
                                setMessage("");
                            } catch (IOException e) {
                                setError("Failed to unzip the template!");
                                e.printStackTrace();
                                hasError.set(true);
                            } finally {
                                try {
                                    response.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        setError("Failed to download the template!");
                        hasError.set(true);
                    }
                });
            });
        }

        return true;
    }

    private void getVersions(String url, String name, List<String> list, ComboBox<String> comboBox) {
        UrlQuery.requestJson(url, JsonArray.class, (jsonArray, code) -> {
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                list.add(jsonObject.get("version").getAsString());
            }

            comboBox.getItems().addAll(loaderVersions);
            comboBox.setValue(loaderVersions.get(0));
        });
    }

    private void getLicenses() {
        UrlQuery.requestJson("https://api.github.com/licenses", JsonArray.class, (jsonArray, code) -> {
            licenseComboBox.setValue("");
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                licenseComboBox.getItems().add(jsonObject.get("spdx_id").getAsString());
            }
        });
    }

    private void getGameVersions() {
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

    private void getLoomVersions() {
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

    public boolean requireValue(ComboBox<String> comboBox, String error) {
        return requireValue(comboBox.getEditor(), error);
    }

    public boolean requireValue(TextInputControl node, String error) {
        if (isEmpty(node)) {
            setError(error);
            requestFocus(node);
            return true;
        }
        return false;
    }

    public boolean isEmpty(TextInputControl node) {
        return StringUtils.isBlank(node.getText());
    }

    public void requestFocus(javafx.scene.Node node) {
        Runnable task = node::requestFocus;

        if (!Platform.isFxApplicationThread()) Platform.runLater(task);
        else task.run();
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
            if (StringUtils.isNotBlank(message)) System.out.println(message);
        };

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(task);
        } else task.run();
    }

    public void generateJavaMod() {}

    public void generateKotlinMod() {}

    public static class LocationPrompt implements Callable<String> {
        private final DirectoryChooser directoryChooser = new DirectoryChooser();
        private final Window parent;
        private final String modName;

        public LocationPrompt(Window parent, String modName) {
            this.parent = parent;
            this.modName = modName;
        }

        @Override
        public String call() throws Exception {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("locationPrompt.fxml")));
            Scene scene = new Scene(root);

            final Stage stage = new Stage();
            stage.setTitle("Fabric mod generator");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
            stage.initOwner(parent);
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.WINDOW_MODAL);

            VBox rootVBox = (VBox) scene.getRoot();
            VBox vBox = (VBox) rootVBox.getChildren().get(0);
            HBox hBox = (HBox) vBox.getChildren().get(0);
            Label label = (Label) vBox.getChildren().get(1);
            Button submit = (Button) vBox.getChildren().get(2);

            TextField input = (TextField) hBox.getChildren().get(0);
            Button openChooser = (Button) hBox.getChildren().get(1);

            Runnable updateLabel = () -> label.setText(String.format("Generating at: %s%s%s", parsePath(input.getText()), File.separator, modName));

            input.setText(Constants.getPersistentCache().generationPath);
            updateLabel.run();

            submit.setOnAction(event -> stage.close());
            openChooser.setOnAction(event -> {
                Optional<File> file = Optional.ofNullable(directoryChooser.showDialog(scene.getWindow()));
                file.ifPresent(file1 -> {
                    input.setText(file1.getAbsolutePath());
                    updateLabel.run();
                });
            });
            input.setOnKeyReleased(event -> updateLabel.run());
            input.setOnKeyPressed(event -> updateLabel.run());
            input.setOnKeyTyped(event -> updateLabel.run());
            input.setOnAction(event -> updateLabel.run());

            stage.setOnCloseRequest(event -> cancelled.set(true));

            stage.setScene(scene);
            stage.showAndWait();

            if (!cancelled.get()) {
                Constants.getPersistentCache().generationPath = parsePath(input.getText());
                Constants.CACHE_CONTROLLER.save();
            }

            return cancelled.get() ? null : parsePath(input.getText() + File.separator + modName);
        }

        public String parsePath(String input) {
            return Path.of(input).toFile().getAbsolutePath();
        }
    }

    public static class KotlinPrompt implements Callable<KotlinData> {
        private final Window parent;
        private final FXMLController controller;

    public KotlinPrompt(Window parent, FXMLController controller) {
            this.parent = parent;
            this.controller = controller;
        }

        @Override
        public KotlinData call() throws Exception {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("kotlinPrompt.fxml")));
            Scene scene = new Scene(root);

            final Stage stage = new Stage();
            stage.setTitle("Fabric mod generator");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
            stage.initOwner(parent);
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.WINDOW_MODAL);

            VBox rootVBox = (VBox) scene.getRoot();
            VBox vBox = (VBox) rootVBox.getChildren().get(0);
            HBox hBox = (HBox) vBox.getChildren().get(0);
            Label label = (Label) vBox.getChildren().get(1);
            Button submit = (Button) vBox.getChildren().get(2);

            TextField input = (TextField) hBox.getChildren().get(0);
            Button openChooser = (Button) hBox.getChildren().get(1);

            Runnable updateLabel = () -> label.setText(String.format("Generating at: %s%s%s", parsePath(input.getText()), File.separator, modName));

            input.setText(Constants.getPersistentCache().generationPath);
            updateLabel.run();

            submit.setOnAction(event -> stage.close());
            input.setOnKeyReleased(event -> updateLabel.run());
            input.setOnKeyPressed(event -> updateLabel.run());
            input.setOnKeyTyped(event -> updateLabel.run());
            input.setOnAction(event -> updateLabel.run());

            stage.setOnCloseRequest(event -> cancelled.set(true));

            stage.setScene(scene);
            stage.showAndWait();

            if (!cancelled.get()) {
                Constants.getPersistentCache().generationPath = parsePath(input.getText());
                Constants.CACHE_CONTROLLER.save();
            }

            return //stuff;
        }

        public String parsePath(String input) {
            return Path.of(input).toFile().getAbsolutePath();
        }
    }
}
