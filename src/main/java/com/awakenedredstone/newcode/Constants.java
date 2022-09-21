package com.awakenedredstone.newcode;

import com.awakenedredstone.newcode.cache.CacheController;
import com.awakenedredstone.newcode.cache.PersistentCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Constants {
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final Path TEMPLATE_PATH = Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "template");
    public static final Path CACHE_PATH = Path.of(System.getProperty("java.io.tmpdir"), "fabricmodgen", "cache");
    public static final Pattern SEMVER_REGEX = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");
    public static final CacheController CACHE_CONTROLLER = new CacheController(CACHE_PATH);

    public static PersistentCache getPersistentCache() {
        return CACHE_CONTROLLER.getPersistentCache();
    }
}
