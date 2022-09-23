package com.awakenedredstone.fabrigen.settings;

import com.awakenedredstone.fabrigen.Constants;
import com.awakenedredstone.fabrigen.util.JsonHelper;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;

public class SettingsManager {
    private final Path dir;
    private final File settingsFile;

    public SettingsManager(Path dir) {
        this.dir = dir;
        this.settingsFile = dir.resolve("settings.json").toFile();
    }

    private Settings settings;

    public boolean safeLoadOrCreateSetting() {
        try {
            return loadOrCreateSetting();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadOrCreateSetting() throws Exception {
        try {
            if ((dir.toFile().exists() && dir.toFile().isDirectory()) || dir.toFile().mkdirs()) {
                if (settingsFile.exists() && settingsFile.isFile() && settingsFile.canRead()) {
                    settings = Constants.GSON.fromJson(new FileReader(settingsFile), Settings.class);
                } else if (!settingsFile.exists()) {
                    JsonHelper.writeJsonToFile(defaultConfig(), getSettingsFile());
                    settings = new Settings();
                }
                System.out.println("Fabrigen configurations loaded");
                return true;
            } else {
                System.err.println("Failed to load the configurations!");
                return false;
            }
        } catch (Exception exception) {
            System.err.println("An error occurred when trying to load the configurations!");
            throw exception;
        }
    }

    public JsonObject defaultConfig() {
        return Constants.GSON.toJsonTree(new Settings()).getAsJsonObject();
    }

    public Settings getSettings() {
        return settings;
    }

    public void save() {
        if (!dir.toFile().exists()) dir.toFile().mkdirs();
        JsonHelper.writeJsonToFile(Constants.GSON.toJsonTree(settings).getAsJsonObject(), settingsFile);
    }

    public File getSettingsFile() {
        return settingsFile;
    }
}

