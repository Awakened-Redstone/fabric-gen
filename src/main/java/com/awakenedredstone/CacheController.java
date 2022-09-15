package com.awakenedredstone;

import com.awakenedredstone.util.JsonHelper;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;

public class CacheController {
    private final Path dir;
    private final File cacheFile;

    public CacheController(Path dir) {
        this.dir = dir;
        this.cacheFile = dir.resolve("cache.json").toFile();
    }

    private PersistentCache persistentCache;

    public void loadOrCreateCache() {
        try {
            if ((dir.toFile().exists() && dir.toFile().isDirectory()) || dir.toFile().mkdirs()) {
                if (cacheFile.exists() && cacheFile.isFile() && cacheFile.canRead()) {
                    persistentCache = Main.GSON.fromJson(new FileReader(cacheFile), PersistentCache.class);
                } else if (!cacheFile.exists()) {
                    JsonHelper.writeJsonToFile(defaultConfig(), getCacheFile());
                    persistentCache = new PersistentCache();
                }
                return;
            }
            System.err.println("CubeController configurations loaded");
        } catch (Exception exception) {
            System.err.println("An error occurred when trying to load the configurations!");
            exception.printStackTrace();
            return;
        }

        System.err.println("Failed to load the configurations!");
    }

    public JsonObject defaultConfig() {
        return Main.GSON.toJsonTree(new PersistentCache()).getAsJsonObject();
    }

    public PersistentCache getPersistentCache() {
        return persistentCache;
    }

    public void save() {
        if (!dir.toFile().exists()) dir.toFile().mkdirs();
        JsonHelper.writeJsonToFile(Main.GSON.toJsonTree(persistentCache).getAsJsonObject(), cacheFile);
    }

    public File getCacheFile() {
        return cacheFile;
    }
}

