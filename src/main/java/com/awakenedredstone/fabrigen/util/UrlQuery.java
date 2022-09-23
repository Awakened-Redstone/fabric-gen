package com.awakenedredstone.fabrigen.util;

import com.awakenedredstone.fabrigen.Constants;
import com.google.gson.JsonElement;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.BiConsumer;

public class UrlQuery {
    public static void requestJson(String url, BiConsumer<JsonElement, Integer> consumer) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

        Constants.OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = Objects.requireNonNull(response.body()).string();
                consumer.accept(Constants.GSON.fromJson(responseBody, JsonElement.class), response.code());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                consumer.accept(null, -1);
            }
        });
    }

    public static <T extends JsonElement> void requestJson(String url, Class<T> clazz, BiConsumer<T, Integer> consumer) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

        Constants.OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = Objects.requireNonNull(response.body()).string();
                consumer.accept(Constants.GSON.fromJson(responseBody, clazz), response.code());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                consumer.accept(null, -1);
            }
        });
    }

    public static <T extends JsonElement> void requestJsonSync(String url, Class<T> clazz, BiConsumer<T, Integer> consumer) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

        try {
            Response response = Constants.OK_HTTP_CLIENT.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            consumer.accept(Constants.GSON.fromJson(responseBody, clazz), response.code());
        } catch (IOException e) {
            consumer.accept(null, -1);
            e.printStackTrace();
        }

    }

    public static void request(String url, BiConsumer<String, Integer> consumer) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

        Constants.OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                consumer.accept(Objects.requireNonNull(response.body()).string(), response.code());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                consumer.accept(null, -1);
            }
        });
    }

    public static void requestStreamSync(String url, BiConsumer<InputStream, Integer> consumer) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

        Response response = null;
        try {
            response = Constants.OK_HTTP_CLIENT.newCall(request).execute();
            consumer.accept(Objects.requireNonNull(response.body()).byteStream(), response.code());
        } catch (IOException e) {
            consumer.accept(null, -1);
            e.printStackTrace();
        }
    }
}
