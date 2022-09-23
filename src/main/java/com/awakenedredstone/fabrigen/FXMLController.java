package com.awakenedredstone.fabrigen;

import com.awakenedredstone.fabrigen.util.*;
import com.awakenedredstone.fabrigen.util.version.SemanticVersionImpl;
import com.awakenedredstone.fabrigen.util.TemplateManager;
import com.awakenedredstone.fabrigen.util.UrlQuery;
import com.awakenedredstone.fabrigen.util.Utils;
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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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
    @FXML public TextField modDescriptionTextField;
    @FXML public TextField mainClassNameTextField;

    @FXML public TextField authorsTextField;
    @FXML public TextField homepageTextField;
    @FXML public TextField sourcesTextField;
    @FXML public ComboBox<String> licenseComboBox;
    @FXML public CheckBox kotlinTemplate;

    @FXML public Label message;

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

    public void onMinecraftVersionComboBoxAction(ActionEvent event) {
        getYarn(false);
    }

    public void onLicenseComboBoxAction(ActionEvent event) {
        setMessage("");
        if (Constants.getPersistentCache().licenses.contains(licenseComboBox.getValue())) {
            licenseContent = findLicense(licenseComboBox.getValue());
            if (licenseContent == null) {
                setError("Invalid license! Removing from cache");
                Constants.getPersistentCache().licenses.remove(licenseComboBox.getValue());
                Constants.CACHE_MANAGER.save();
            }
        } else getLicense(false);
    }

    public void onGenerateProjectOnAction(ActionEvent event) {
        Thread thread = new Thread(this::generateTemplate);
        thread.setDaemon(true);
        thread.start();
    }

    private void generateTemplate() {
        Constants.SETTINGS_MANAGER.safeLoadOrCreateSetting();
        Constants.CACHE_MANAGER.safeLoadOrCreateCache();
        AtomicBoolean hasError = new AtomicBoolean(false);
        setMessage("");

        if (parseErrors()) return;

        KotlinData kotlinData = null;
        if (kotlinTemplate.isSelected()) {
            try {
                FutureTask<KotlinData> futureTask = new FutureTask<>(new KotlinPrompt(message.getScene().getWindow()));
                Platform.runLater(futureTask);
                kotlinData = futureTask.get();
                if (kotlinData == null) {
                    setError("Generation cancelled!");
                    hasError.set(true);
                    return;
                }
            } catch (ExecutionException | InterruptedException e) {
                setError("An unknown error occurred when getting the kotlin info!");
                e.printStackTrace();
                return;
            }
        }

        if (hasError.get()) return;

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
            setError("An unknown error occurred when checking the generation location!");
            e.printStackTrace();
            return;
        }

        if (hasError.get()) return;
        if (!parseData()) {
            Constants.CACHE_MANAGER.save();
            return;
        } else Constants.CACHE_MANAGER.save();

        Constants.CACHE_MANAGER.save();
        setMessage("");

        boolean success = kotlinTemplate.isSelected() ? generateKotlinMod(kotlinData) : generateJavaMod();
        if (!success) return;

        setMessage("Mod generated!");
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
        setMessage("");
        AtomicBoolean hasError = new AtomicBoolean(false);

        if (Constants.getPersistentCache().licenses.contains(licenseComboBox.getValue())) {
            licenseContent = findLicense(licenseComboBox.getValue());
            if (licenseContent == null) {
                setError("Invalid license! Removing from cache");
                Constants.getPersistentCache().licenses.remove(licenseComboBox.getValue());
                Constants.CACHE_MANAGER.save();
            }
        } else {
            if (StringUtils.isBlank(licenseContent) && !getLicense(true)) return false;
        }

        if (Constants.getPersistentCache().javaVersions.containsKey(minecraftVersionComboBox.getValue())) {
            javaVersion = Constants.getPersistentCache().javaVersions.get(minecraftVersionComboBox.getValue());
        } else {
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
                    Constants.getPersistentCache().javaVersions.put(minecraftVersionComboBox.getValue(), javaVersion);
                    setMessage("");
                });
            });
        }

        if (hasError.get()) return false;
        if (StringUtils.isNotBlank(Constants.getPersistentCache().gradleVersion)) {
            gradleVersion = Constants.getPersistentCache().gradleVersion;
        } else {
            setMessage("Getting Gradle version...");
            UrlQuery.requestJsonSync("https://services.gradle.org/versions/current", JsonObject.class, (jsonObject, code) -> {
                if (code != 200) {
                    setError("Failed to get Gradle version!");
                    hasError.set(true);
                    return;
                } else setMessage("");
                gradleVersion = jsonObject.get("version").getAsString();
                Constants.getPersistentCache().gradleVersion = gradleVersion;
            });
        }

        if (hasError.get()) return false;
        if (StringUtils.isBlank(yarnVersion) && !getYarn(true)) return false;

        if (Constants.getSettings().updateTemplate || !Constants.TEMPLATE_PATH.toFile().exists()) {
            setMessage("Updating template");
            String ending = StringUtils.isBlank(Constants.getSettings().templateVersion) ? "latest" : "tags/" + Constants.getSettings().templateVersion;
            String url = String.format("https://api.github.com/repos/Awakened-Redstone/fabric-mod-template/releases/%s", ending);
            UrlQuery.requestJsonSync(url, JsonObject.class, (jsonObject, code1) -> {
                if (code1 != 200) {
                    setError("Failed to get the template info!");
                    hasError.set(true);
                    return;
                }

                String tag_name = jsonObject.get("tag_name").getAsString();
                if (Constants.getSettings().updateTemplate) {
                    if (Constants.getPersistentCache().templateVersion.equals(tag_name)) return;
                    else {
                        try {
                            FileUtils.deleteDirectory(Constants.TEMPLATE_PATH.toFile());
                        } catch (IOException ignored) {
                        }
                    }
                }

                JsonArray assets = jsonObject.get("assets").getAsJsonArray();
                String downloadUrl = assets.get(0).getAsJsonObject().get("browser_download_url").getAsString();

                try {
                    setMessage("Downloading template");
                    UrlQuery.requestStreamSync(downloadUrl, (response, code) -> {
                        if (code != 200) {
                            setError("Failed to download the template!");
                            hasError.set(true);
                            return;
                        }

                        setMessage("Decompressing template...");

                        try {
                            Utils.unzip(response, Constants.TEMPLATE_PATH);
                            setMessage("");

                            Constants.getPersistentCache().templateVersion = tag_name;
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
        }

        setMessage("");

        return !hasError.get();
    }

    private boolean getYarn(boolean sync) {
        AtomicBoolean hasError = new AtomicBoolean(false);
        setMessage("Getting Yarn version...");
        String url = "https://meta.fabricmc.net/v2/versions/yarn/" + minecraftVersionComboBox.getValue();
        BiConsumer<JsonArray, Integer> consumer =  (jsonArray, code) -> {
            if (code != 200) {
                setError("Failed to get Yarn version!");
                hasError.set(true);
                return;
            } else setMessage("");
            yarnVersion = jsonArray.get(0).getAsJsonObject().get("version").getAsString();
        };
        if (sync) UrlQuery.requestJsonSync(url, JsonArray.class, consumer);
        else UrlQuery.requestJson(url, JsonArray.class, consumer);
        return !hasError.get();
    }

    private boolean getLicense(boolean sync) {
        AtomicBoolean hasError = new AtomicBoolean(false);
        setMessage("Getting license...");
        String url = "https://api.github.com/licenses/" + licenseComboBox.getValue();
        Path licensesPath = Constants.CACHE_PATH.resolve("licenses");
        BiConsumer<JsonObject, Integer> consumer = (jsonObject, code) -> {
            if (code != 200) {
                setError("Could not get the license!");
                hasError.set(true);
                return;
            }
            setMessage("");
            licenseContent = jsonObject.get("body").getAsString();
            String spdx_id = jsonObject.get("spdx_id").getAsString();
            Platform.runLater(() -> licenseComboBox.setValue(spdx_id));
            try {
                if (!licensesPath.toFile().exists()) licensesPath.toFile().mkdirs();
                Constants.getPersistentCache().licenses.add(spdx_id);
                FileUtil.writeFile(licensesPath.resolve(spdx_id).toFile(), licenseContent);
            } catch (IOException e) {
                e.printStackTrace();
                setError("Failed to cache license!");
            }
        };
        if (sync) UrlQuery.requestJsonSync(url, JsonObject.class, consumer);
        else UrlQuery.requestJson(url, JsonObject.class, consumer);

        Constants.CACHE_MANAGER.save();
        return !hasError.get();
    }

    private String findLicense(String license) {
        try {
            Path licensesPath = Constants.CACHE_PATH.resolve("licenses");
            return FileUtil.readFile(licensesPath.resolve(license).toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void getVersions(String url, String name, List<String> list, ComboBox<String> comboBox) {
        UrlQuery.requestJson(url, JsonArray.class, (jsonArray, code) -> {
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                list.add(jsonObject.get(name).getAsString());
            }

            comboBox.getItems().addAll(list);
            comboBox.setValue(list.get(0));
        });
    }

    private void getLicenses() {
        Path licensesPath = Constants.CACHE_PATH.resolve("licenses");
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

    public boolean generateJavaMod() {
        try {
            setMessage("Generating mod...");
            Path targetPath = Path.of(generationPath);

            if (targetPath.toFile().exists() && !FileUtils.isEmptyDirectory(targetPath.toFile())) {
                setError("Target folder must be empty!");
                return false;
            }

            targetPath.toFile().mkdirs();
            FileUtils.copyDirectory(parsePregenPath(GenerationType.JAVA).toFile(), targetPath.toFile());

            int i = 0;
            if (StringUtils.isNotBlank(apiVersionComboBox.getValue())) {
                SemanticVersionImpl newModIdVersion = new SemanticVersionImpl("0.59.0", false);
                i = new SemanticVersionImpl(apiVersionComboBox.getValue(), false).compareTo(newModIdVersion);
            }

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
            Map.Entry<String, String> MOD_DESCRIPTION = MapBuilder.createEntry("MOD_DESCRIPTION", modDescriptionTextField.getText());
            Map.Entry<String, String> MAIN_CLASS = MapBuilder.createEntry("MAIN_CLASS", mainClassNameTextField.getText());

            Map.Entry<String, String> HOMEPAGE = MapBuilder.createEntry("HOMEPAGE", homepageTextField.getText());
            Map.Entry<String, String> SOURCES = MapBuilder.createEntry("SOURCES", sourcesTextField.getText());

            Map.Entry<String, String> LICENSE = MapBuilder.createEntry("LICENSE", licenseComboBox.getValue());

            Map.Entry<String, String> YARN_VERSION = MapBuilder.createEntry("YARN_VERSION", yarnVersion);
            Map.Entry<String, String> JAVA_VERSION = MapBuilder.createEntry("JAVA_VERSION", javaVersion);
            Map.Entry<String, String> GRADLE_VERSION = MapBuilder.createEntry("GRADLE_VERSION", gradleVersion);

            Path resourcesPath = targetPath.resolve("src/main/resources");
            Path srcPath = parseSrc(GenerationType.JAVA, targetPath, MAVEN_GROUP_PATH, ARCHIVES_BASE_NAME);

            resourcesPath.toFile().mkdirs();
            srcPath.toFile().mkdirs();

            generateFile(GenerationType.JAVA, "build.gradle.ft", targetPath.resolve("build.gradle"), LOOM_VERSION, JAVA_VERSION);
            generateFile(GenerationType.JAVA, "gradle.properties.ft", targetPath.resolve("gradle.properties"), MINECRAFT_VERSION, LOADER_VERSION, YARN_VERSION, API_VERSION, MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_VERSION);
            generateFile(GenerationType.JAVA, "gradle-wrapper.properties.ft", targetPath.resolve("gradle/wrapper/gradle-wrapper.properties"), GRADLE_VERSION);
            String mixinPath = TemplateManager.generateTemplate(new MapBuilder<String, String>().put(MOD_ID).build(), "${MOD_ID}.mixins.json");
            generateFile(GenerationType.JAVA, "mixins.json.ft", resourcesPath.resolve(mixinPath), MAVEN_GROUP, ARCHIVES_BASE_NAME, JAVA_VERSION);
            generateFile(GenerationType.JAVA, "ModMain.java.ft", srcPath.resolve(mainClassNameTextField.getText() + ".java"), MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_ID);
            generateIcon(GenerationType.JAVA, modIdTextField.getText(), resourcesPath);

            {
                setMessage("Generating fabric.mod.json");
                File file = resourcesPath.resolve("fabric.mod.json").toFile();
                String template = FileUtil.readFile(parseTemplatePath(GenerationType.JAVA).resolve("fabric.mod.json.ft").toFile());
                MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(MOD_ID, MAVEN_GROUP, ARCHIVES_BASE_NAME, MAIN_CLASS, LOADER_VERSION, MINECRAFT_VERSION, MOD_DESCRIPTION, MOD_NAME, LICENSE, HOMEPAGE, SOURCES);
                if (StringUtils.isNotBlank(apiVersionComboBox.getValue())) mapBuilder.put(API_MOD_ID, API_VERSION);
                String generatedTemplate = TemplateManager.generateTemplate(mapBuilder.build(), template);
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
                setMessage("Generating LICENSE");
                FileUtil.writeFile(targetPath.resolve("LICENSE").toFile(), licenseContent);
            }
            setMessage("");
        } catch (Exception e) {
            setError("Failed to generate mod!");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean generateKotlinMod(KotlinData kotlinData) {
        try {
            if (kotlinData == null) return false;

            setMessage("Generating mod...");
            Path targetPath = Path.of(generationPath);

            if (targetPath.toFile().exists() && !FileUtils.isEmptyDirectory(targetPath.toFile())) {
                setError("Target folder must be empty!");
                return false;
            }

            targetPath.toFile().mkdirs();
            FileUtils.copyDirectory(parsePregenPath(GenerationType.KOTLIN).toFile(), targetPath.toFile());

            int i = 0;
            if (StringUtils.isNotBlank(apiVersionComboBox.getValue())) {
                SemanticVersionImpl newModIdVersion = new SemanticVersionImpl("0.59.0", false);
                i = new SemanticVersionImpl(apiVersionComboBox.getValue(), false).compareTo(newModIdVersion);
            }

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
            Map.Entry<String, String> MOD_DESCRIPTION = MapBuilder.createEntry("MOD_DESCRIPTION", modDescriptionTextField.getText());
            Map.Entry<String, String> MAIN_CLASS = MapBuilder.createEntry("MAIN_CLASS", mainClassNameTextField.getText());

            Map.Entry<String, String> HOMEPAGE = MapBuilder.createEntry("HOMEPAGE", homepageTextField.getText());
            Map.Entry<String, String> SOURCES = MapBuilder.createEntry("SOURCES", sourcesTextField.getText());

            Map.Entry<String, String> LICENSE = MapBuilder.createEntry("LICENSE", licenseComboBox.getValue());

            Map.Entry<String, String> YARN_VERSION = MapBuilder.createEntry("YARN_VERSION", yarnVersion);
            Map.Entry<String, String> JAVA_VERSION = MapBuilder.createEntry("JAVA_VERSION", javaVersion);
            Map.Entry<String, String> GRADLE_VERSION = MapBuilder.createEntry("GRADLE_VERSION", gradleVersion);

            Map.Entry<String, String> FABRIC_KOTLIN_VERSION = MapBuilder.createEntry("FABRIC_KOTLIN_VERSION", kotlinData.fabricKotlinVersion());
            Map.Entry<String, String> KOTLIN_VERSION = MapBuilder.createEntry("KOTLIN_VERSION", kotlinData.kotlinVersion());

            Path resourcesPath = targetPath.resolve("src/main/resources");
            Path srcPath = parseSrc(GenerationType.KOTLIN, targetPath, MAVEN_GROUP_PATH, ARCHIVES_BASE_NAME);

            resourcesPath.toFile().mkdirs();
            srcPath.toFile().mkdirs();

            generateFile(GenerationType.KOTLIN, "build.gradle.kts.ft", targetPath.resolve("build.gradle.kts"), LOOM_VERSION, JAVA_VERSION);
            generateFile(GenerationType.KOTLIN, "gradle.properties.ft", targetPath.resolve("gradle.properties"), MINECRAFT_VERSION, LOADER_VERSION, YARN_VERSION, API_VERSION, MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_VERSION, KOTLIN_VERSION, FABRIC_KOTLIN_VERSION);
            generateFile(GenerationType.KOTLIN, "gradle-wrapper.properties.ft", targetPath.resolve("gradle/wrapper/gradle-wrapper.properties"), GRADLE_VERSION);
            String mixinPath = TemplateManager.generateTemplate(new MapBuilder<String, String>().put(MOD_ID).build(), "${MOD_ID}.mixins.json");
            generateFile(GenerationType.KOTLIN, "mixins.json.ft", resourcesPath.resolve(mixinPath), MAVEN_GROUP, ARCHIVES_BASE_NAME, JAVA_VERSION);
            generateFile(GenerationType.KOTLIN, "ModMain.kt.ft", srcPath.resolve(mainClassNameTextField.getText() + ".kt"), MAVEN_GROUP, ARCHIVES_BASE_NAME, MOD_ID);
            generateIcon(GenerationType.KOTLIN, modIdTextField.getText(), resourcesPath);

            {
                setMessage("Generating fabric.mod.json");
                File file = resourcesPath.resolve("fabric.mod.json").toFile();
                String template = FileUtil.readFile(parseTemplatePath(GenerationType.KOTLIN).resolve("fabric.mod.json.ft").toFile());
                MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(MOD_ID, MAVEN_GROUP, ARCHIVES_BASE_NAME, MAIN_CLASS, LOADER_VERSION, MINECRAFT_VERSION, MOD_DESCRIPTION, MOD_NAME, LICENSE, HOMEPAGE, SOURCES, KOTLIN_VERSION, FABRIC_KOTLIN_VERSION);
                if (StringUtils.isNotBlank(apiVersionComboBox.getValue())) mapBuilder.put(API_MOD_ID, API_VERSION);
                String generatedTemplate = TemplateManager.generateTemplate(mapBuilder.build(), template);
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
                setMessage("Generating LICENSE");
                FileUtil.writeFile(targetPath.resolve("LICENSE").toFile(), licenseContent);
            }

        }  catch (Exception e) {
            setError("Failed to generate mod!");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @SafeVarargs
    public final Path parseSrc(GenerationType type, Path targetPath, Map.Entry<String, String>... entries) {
        String ending = TemplateManager.generateTemplate(new MapBuilder<String, String>().put(entries).build(), "/${MAVEN_GROUP}/${ARCHIVES_BASE_NAME}");
        return targetPath.resolve("src/main/" + type.name().toLowerCase() + ending);
    }

    public Path parseTemplatePath(GenerationType type) {
        return Constants.TEMPLATE_PATH.resolve(type.name().toLowerCase()).resolve("template");
    }

    public Path parsePregenPath(GenerationType type) {
        return Constants.TEMPLATE_PATH.resolve(type.name().toLowerCase()).resolve("pregen");
    }

    @SafeVarargs
    public final void generateFile(GenerationType type, String templateFile, Path targetPath, Map.Entry<String, String>... entries) throws IOException {
        File file = targetPath.toFile();
        file.getParentFile().mkdirs();
        setMessage("Generating " + file.getName());
        String template = FileUtil.readFile(parseTemplatePath(type).resolve(templateFile).toFile());
        MapBuilder<String, String> mapBuilder = new MapBuilder<String, String>().put(entries);
        FileUtil.writeFile(file, TemplateManager.generateTemplate(mapBuilder.build(), template));
    }

    public void generateIcon(GenerationType type, String modId, Path resourcesPath) throws IOException {
        Map.Entry<String, String> MOD_ID = MapBuilder.createEntry("MOD_ID", modId);

        setMessage("Generating icon.png");
        String path = TemplateManager.generateTemplate(new MapBuilder<String, String>().put(MOD_ID).build(), "assets/${MOD_ID}/icon.png");
        File file = resourcesPath.resolve(path).toFile();
        FileUtils.copyFile(parseTemplatePath(type).resolve("icon.png").toFile(), file);
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

            Runnable updateLabel = () -> {
                label.setText(String.format("Generating at: %s%s%s", parsePath(input.getText()), File.separator, modName));
                try {
                    Path path = Path.of(parsePath(input.getText() + File.separator + modName));
                    if (path.toFile().exists() && !FileUtils.isEmptyDirectory(path.toFile())) label.setTextFill(Color.RED);
                    else label.setTextFill(Color.BLACK);
                } catch (Exception ignored) {}
            };

            input.setText(Constants.getPersistentCache().generationPath);
            updateLabel.run();

            submit.setOnAction(event -> {
                try {
                    Path path = Path.of(parsePath(input.getText() + File.separator + modName));
                    if (path.toFile().exists() && !FileUtils.isEmptyDirectory(path.toFile())) {
                        label.setText("Folder must be empty!");
                        label.setTextFill(Color.RED);
                        input.requestFocus();
                    } else stage.close();
                } catch (Exception ignored) {
                    stage.close();
                }
            });
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
                Constants.CACHE_MANAGER.save();
            }

            return cancelled.get() ? null : parsePath(input.getText() + File.separator + modName);
        }

        public String parsePath(String input) {
            return Path.of(input).toFile().getAbsolutePath();
        }
    }

    public static class KotlinPrompt implements Callable<KotlinData> {
        private final Window parent;

        public KotlinPrompt(Window parent) {
            this.parent = parent;
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
            Label label = (Label) vBox.getChildren().get(0);
            HBox hBox = (HBox) vBox.getChildren().get(1);
            Button submit = (Button) vBox.getChildren().get(3);

            ComboBox<String> input = (ComboBox<String>) hBox.getChildren().get(0);

            parseFabricKotlinVersions(input);

            submit.setOnAction(event -> stage.close());

            stage.setOnCloseRequest(event -> cancelled.set(true));

            stage.setScene(scene);
            stage.showAndWait();

            if (cancelled.get()) return null;

            String[] versions = input.getValue().split("\\+kotlin\\.");

            return new KotlinData(versions[0], versions[1]);
        }

        public void parseFabricKotlinVersions(ComboBox<String> node) {
            UrlQuery.requestJson("https://api.modrinth.com/v2/project/fabric-language-kotlin/version", JsonArray.class, (jsonArray, code) -> {
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    node.getItems().add(jsonObject.get("version_number").getAsString());
                }

                Platform.runLater(() -> node.setValue(jsonArray.get(0).getAsJsonObject().get("version_number").getAsString()));
            });
        }
    }

    public enum GenerationType {
        JAVA,
        KOTLIN
    }
}
