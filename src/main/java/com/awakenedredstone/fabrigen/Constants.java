package com.awakenedredstone.fabrigen;

import com.awakenedredstone.fabrigen.cache.CacheManager;
import com.awakenedredstone.fabrigen.cache.PersistentCache;
import com.awakenedredstone.fabrigen.settings.Settings;
import com.awakenedredstone.fabrigen.settings.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Constants {
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final Path MAIN_PATH = Path.of(FileUtils.getUserDirectoryPath(), "fabricmodgen");
    public static final Path TEMPLATE_PATH = MAIN_PATH.resolve("template");
    public static final Path CACHE_PATH = MAIN_PATH.resolve("cache");
    public static final Pattern SEMVER_REGEX = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");
    public static final CacheManager CACHE_MANAGER = new CacheManager(CACHE_PATH);
    public static final SettingsManager SETTINGS_MANAGER = new SettingsManager(MAIN_PATH);

    public static PersistentCache getPersistentCache() {
        return CACHE_MANAGER.getPersistentCache();
    }

    public static Settings getSettings() {
        return SETTINGS_MANAGER.getSettings();
    }
}
