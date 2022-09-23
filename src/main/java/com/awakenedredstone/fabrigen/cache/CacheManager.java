package com.awakenedredstone.fabrigen.cache;

import com.awakenedredstone.fabrigen.Constants;
import com.awakenedredstone.fabrigen.util.JsonHelper;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;

public class CacheManager {
    private final Path dir;
    private final File cacheFile;

    public CacheManager(Path dir) {
        this.dir = dir;
        this.cacheFile = dir.resolve("cache.json").toFile();
    }

    private PersistentCache persistentCache;

    public boolean safeLoadOrCreateCache() {
        try {
            return loadOrCreateCache();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadOrCreateCache() throws Exception {
        try {
            if ((dir.toFile().exists() && dir.toFile().isDirectory()) || dir.toFile().mkdirs()) {
                if (cacheFile.exists() && cacheFile.isFile() && cacheFile.canRead()) {
                    persistentCache = Constants.GSON.fromJson(new FileReader(cacheFile), PersistentCache.class);
                } else if (!cacheFile.exists()) {
                    JsonHelper.writeJsonToFile(defaultConfig(), getCacheFile());
                    persistentCache = new PersistentCache();
                }
                System.out.println("Fabrigen cache loaded");
                return true;
            } else {
                System.err.println("Failed to load the cache!");
                return false;
            }
        } catch (Exception exception) {
            System.err.println("An error occurred when trying to load the cache!");
            throw exception;
        }
    }

    public JsonObject defaultConfig() {
        return Constants.GSON.toJsonTree(new PersistentCache()).getAsJsonObject();
    }

    public PersistentCache getPersistentCache() {
        return persistentCache;
    }

    public void save() {
        if (!dir.toFile().exists()) dir.toFile().mkdirs();
        JsonHelper.writeJsonToFile(Constants.GSON.toJsonTree(persistentCache).getAsJsonObject(), cacheFile);
    }

    public File getCacheFile() {
        return cacheFile;
    }
}

