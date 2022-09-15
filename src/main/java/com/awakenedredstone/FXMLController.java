package com.awakenedredstone;

import com.awakenedredstone.util.*;
import com.awakenedredstone.util.version.SemanticVersionImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
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

    @FXML public ComboBox<String> minecraftVersionComboBox;
    @FXML public ComboBox<String> apiVersionComboBox;
    @FXML public ComboBox<String> loomVersionComboBox;
    @FXML public ComboBox<String> loaderVersionComboBox;
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

    public void onGenerateProjectOnAction(ActionEvent event) {
        onButtonClicked();
    }

    public void onButtonClicked() {
        Thread task = new Thread(() -> {
            setMessage("");
            if (parseErrors()) return;
            AtomicBoolean hasError = new AtomicBoolean(false);

            try {
                FutureTask<String> futureTask = new FutureTask<>(new LocationPrompt(message.getScene().getWindow(), modNameTextField.getText()));
                Platform.runLater(futureTask);
                generationPath = futureTask.get();
                if (generationPath == null) {
                    setError("Generation cancelled!");
                    return;
                } else if (StringUtils.isBlank(generationPath)) {
                    setError("Invalid location inserted!");
                    return;
                }

                Path targetPath = Path.of(generationPath);
                if (targetPath.toFile().exists() && !FileUtils.isEmptyDirectory(targetPath.toFile())) {
                    setError("Target folder must be empty!");
                    hasError.set(true);
                    return;
                }
            } catch (ExecutionException | InterruptedException | IOException e) {
                setError("An error occurred when checking the generation location!");
                e.printStackTrace();
                return;
            }

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

            if (hasError.get()) return;
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
                    setMessage("");
                });
            });

            if (hasError.get()) return;
            setMessage("Getting Gradle version...");
            UrlQuery.requestJsonSync("https://services.gradle.org/versions/current", JsonObject.class, (jsonObject, code) -> {
                if (code != 200) {
                    setError("Failed to get Gradle version!");
                    hasError.set(true);
                    return;
                } else setMessage("");
                gradleVersion = jsonObject.get("version").getAsString();
            });

            if (hasError.get()) return;
            setMessage("Getting Yarn version...");
            UrlQuery.requestJsonSync("https://meta.fabricmc.net/v2/versions/yarn/" + minecraftVersionComboBox.getValue(), JsonArray.class, (jsonArray, code) -> {
                if (code != 200) {
                    setError("Failed to get Yarn version!");
                    hasError.set(true);
                    return;
                } else setMessage("");
                yarnVersion = jsonArray.get(0).getAsJsonObject().get("version").getAsString();
            });

            if (hasError.get()) return;
            if (!Main.TEMPLATE_PATH.toFile().exists()) {
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
                                    Utils.unzip(response, Main.TEMPLATE_PATH);
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

            setMessage("");
            if (hasError.get()) return;

            TemplateManager templateManager = TemplateManager.create();
            String s = templateManager.generateTemplate(Map.of("NAME", "World"), "Hello ${NAME}");
            System.out.println(s);

            try {
                setMessage("Generating mod...");
                Path targetPath = Path.of(generationPath);
                Path templates = Main.TEMPLATE_PATH.resolve("template");

                if (targetPath.toFile().exists() && !FileUtils.isEmptyDirectory(targetPath.toFile())) {
                    setError("Target folder must be empty!");
                    hasError.set(true);
                    return;
                }

                targetPath.toFile().mkdirs();
                FileUtils.copyDirectory(Main.TEMPLATE_PATH.resolve("pregen").toFile(), targetPath.toFile());

                SemanticVersionImpl newModIdVersion = new SemanticVersionImpl("0.59.0", false);
                int i = new SemanticVersionImpl(apiVersionComboBox.getValue(), false).compareTo(newModIdVersion);

                Map.Entry<String, String> MINECRAFT_VERSION = MapBuilder.createEntry("MINECRAFT_VERSION", minecraftVersionComboBox.getValue());
                Map.Entry<String, String> API_MOD_ID = MapBuilder.createEntry("API_MOD_ID", i >= 0 ? "fabric-api" : "fabric");
                Map.Entry<String, String> API_VERSION = MapBuilder.createEntry("API_VERSION", apiVersionComboBox.getValue());
                Map.Entry<String, String> LOADER_VERSION = MapBuilder.createEntry("LOADER_VERSION", loaderVersionComboBox.getValue());
                Map.Entry<String, String> LOOM_VERSION = MapBuilder.createEntry("LOOM_VERSION", loomVersionComboBox.getValue());

                Map.Entry<String, String> MOD_VERSION = MapBuilder.createEntry("MOD_VERSION", modVersionTextField.getText());
                Map.Entry<String, String> MAVEN_GROUP = MapBuilder.createEntry("MAVEN_GROUP", basePackageNameTextField.getText());
                Map.Entry<String, String> MAVEN_GROUP_PATH = MapBuilder.createEntry("MAVEN_GROUP", basePackageNameTextField.getText().replace(".", "/"));
                Map.Entry<String, String> ARCHIVES_BASE_NAME = MapBuilder.createEntry("ARCHIVES_BASE_NAME", archivesBaseNameTextField.getText());
                Map.Entry<String, String> MOD_ID = MapBuilder.createEntry("MOD_ID", modIdTextField.getText());
                Map.Entry<String, String> MOD_NAME = MapBuilder.createEntry("MOD_NAME", modNameTextField.getText());
                Map.Entry<String, String> MAIN_CLASS = MapBuilder.createEntry("MAIN_CLASS", mainClassNameTextField.getText());

                Map.Entry<String, String> HOMEPAGE = MapBuilder.createEntry("HOMEPAGE", homepageTextField.getText());
                Map.Entry<String, String> SOURCES = MapBuilder.createEntry("SOURCES", sourcesTextField.getText());

                Map.Entry<String, String> LICENSE = MapBuilder.createEntry("LICENSE", licenseComboBox.getValue());

                Map.Entry<String, String> LICENSE_CONTENT = MapBuilder.createEntry("LICENSE_CONTENT", licenseContent);
                Map.Entry<String, String> YARN_VERSION = MapBuilder.createEntry("YARN_VERSION", yarnVersion);
                Map.Entry<String, String> JAVA_VERSION = MapBuilder.createEntry("JAVA_VERSION", javaVersion);
                Map.Entry<String, String> GRADLE_VERSION = MapBuilder.createEntry("GRADLE_VERSION", gradleVersion);
                Map.Entry<String, String> MOD_DESCRIPTION = MapBuilder.createEntry("MOD_DESCRIPTION", "Placeholder text");

                Path resourcesPath = targetPath.resolve("src/main/resources");
                Path srcPath = targetPath.resolve(templateManager.generateTemplate(new MapBuilder<String, String>()
                        .put(MAVEN_GROUP_PATH, ARCHIVES_BASE_NAME).build(),
                        "src/main/java/${MAVEN_GROUP}/${ARCHIVES_BASE_NAME}"));

                resourcesPath.toFile().mkdirs();
                srcPath.toFile().mkdirs();

                {
                    setMessage("Generating build.gradle");
                    File file = targetPath.resolve("build.gradle").toFile();
                    String template = FileUtil.readFile(templates.resolve("build.gradle.ft").toFile());
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(LOOM_VERSION, JAVA_VERSION);
                    FileUtil.writeFile(file, templateManager.generateTemplate(mapBuilder.build(), template));
                }
                {
                    setMessage("Generating gradle.properties");
                    File file = targetPath.resolve("gradle.properties").toFile();
                    String template = FileUtil.readFile(templates.resolve("gradle.properties.ft").toFile());
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(MINECRAFT_VERSION,
                            LOADER_VERSION, YARN_VERSION, API_VERSION, MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_VERSION);
                    FileUtil.writeFile(file, templateManager.generateTemplate(mapBuilder.build(), template));
                }
                {
                    setMessage("Generating gradle-wrapper.properties");
                    File file = targetPath.resolve("gradle/wrapper/gradle-wrapper.properties").toFile();
                    String template = FileUtil.readFile(templates.resolve("gradle-wrapper.properties.ft").toFile());
                    file.getParentFile().mkdirs();
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(GRADLE_VERSION);
                    FileUtil.writeFile(file, templateManager.generateTemplate(mapBuilder.build(), template));
                }
                {
                    setMessage("Generating fabric.mod.json");
                    File file = resourcesPath.resolve("fabric.mod.json").toFile();
                    String template = FileUtil.readFile(templates.resolve("fabric.mod.json.ft").toFile());
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(API_MOD_ID, API_VERSION, MOD_ID, MAVEN_GROUP, ARCHIVES_BASE_NAME, MAIN_CLASS,
                            LOADER_VERSION, MINECRAFT_VERSION, MOD_DESCRIPTION, MOD_NAME, LICENSE, HOMEPAGE, SOURCES);
                    String generatedTemplate = templateManager.generateTemplate(mapBuilder.build(), template);
                    JsonObject jsonObject = JsonParser.parseString(generatedTemplate).getAsJsonObject();
                    if (StringUtils.isNotBlank(authorsTextField.getText())) {
                        JsonArray jsonArray = new JsonArray();
                        for (String author : authorsTextField.getText().split(", ?")) {
                            jsonArray.add(author);
                        }
                        jsonObject.add("authors", jsonArray);
                    }
                    JsonHelper.writeJsonToFile(jsonObject, file);
                }
                {
                    String path = templateManager.generateTemplate(new MapBuilder<String, String>().put(MOD_ID).build(), "${MOD_ID}.mixins.json");
                    setMessage("Generating " + path);
                    File file = resourcesPath.resolve(path).toFile();
                    String template = FileUtil.readFile(templates.resolve("mixins.json.ft").toFile());
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(MAVEN_GROUP, ARCHIVES_BASE_NAME, JAVA_VERSION);
                    FileUtil.writeFile(file, templateManager.generateTemplate(mapBuilder.build(), template));
                }
                {
                    setMessage(String.format("Generating %s.java", mainClassNameTextField.getText()));
                    String template = FileUtil.readFile(templates.resolve("ModMain.java.ft").toFile());
                    MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_ID);
                    FileUtil.writeFile(srcPath.resolve(mainClassNameTextField.getText() + ".java").toFile(), templateManager.generateTemplate(mapBuilder.build(), template));
                }
                {
                    setMessage("Generating icon.png");
                    String path = templateManager.generateTemplate(new MapBuilder<String, String>().put(MOD_ID).build(), "assets/${MOD_ID}/icon.png");
                    File file = resourcesPath.resolve(path).toFile();
                    FileUtils.copyFile(templates.resolve("icon.png").toFile(), file);
                }
                {
                    setMessage("Generating LICENSE");
                    FileUtil.writeFile(targetPath.resolve("LICENSE").toFile(), licenseContent);
                }
                setMessage("");
            } catch (Exception e) {
                setError("Failed to generate mod!");
                e.printStackTrace();
                hasError.set(true);
                return;
            }

            System.out.println();
            //Cache last generation location
        });
        task.setDaemon(true);
        task.start();
    }

    public boolean parseErrors() {
        if (!minecraftVersions.contains(minecraftVersionComboBox.getValue())) {
            setError("Invalid Minecraft version!");
            requestFocus(minecraftVersionComboBox);
            return true;
        }

        if (!apiVersions.contains(apiVersionComboBox.getValue())) {
            setError("Invalid Fabric API version!");
            requestFocus(apiVersionComboBox);
            return true;
        }

        if (!requireValue(modVersionTextField, "Please insert the mod version!")) {
            if (!Main.SEMVER.matcher(modVersionTextField.getText()).matches()) {
                setError("Please use a version that complies with Semantic Versions!");
                return true;
            }
        } else return true;

        return requireValue(basePackageNameTextField, "Please insert the maven group!") ||
                requireValue(archivesBaseNameTextField, "Please insert the archives base name!") ||
                requireValue(modIdTextField, "Please insert the mod ID!") ||
                requireValue(modNameTextField, "Please insert the mod name!") ||
                requireValue(mainClassNameTextField, "Please insert the main class!") ||
                requireValue(licenseComboBox.getEditor(), "Please insert a license!");
    }

    public boolean requireValue(TextInputControl node, String error) {
        if (StringUtils.isBlank(node.getText())) {
            setError(error);
            requestFocus(node);
            return true;
        }
        return false;
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

            input.setText(Main.getPersistentCache().generationPath);
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
                Main.getPersistentCache().generationPath = parsePath(input.getText());
                Main.CACHE_CONTROLLER.save();
            }

            return cancelled.get() ? null : parsePath(input.getText() + File.separator + modName);
        }

        public String parsePath(String input) {
            return Path.of(input).toFile().getAbsolutePath();
        }
    }
}
