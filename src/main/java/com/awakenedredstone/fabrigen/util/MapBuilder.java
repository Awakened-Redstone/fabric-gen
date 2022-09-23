package com.awakenedredstone.fabrigen.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<>();

    public static <K, V> Map.Entry<K, V> createEntry(K key, V value) {
        return new MapBuilder<K, V>().put(key, value).build().entrySet().iterator().next();
    }

    public MapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public MapBuilder<K, V> put(Map.Entry<K, V> entry) {
        map.put(entry.getKey(), entry.getValue());
        return this;
    }

    @SafeVarargs
    public final MapBuilder<K, V> put(Map.Entry<K, V>... entries) {
        for (Map.Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Map<K, V> build() {
        return map;
    }
}
