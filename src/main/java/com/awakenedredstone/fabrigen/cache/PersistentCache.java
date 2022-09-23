package com.awakenedredstone.fabrigen.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentCache {
    public String generationPath = System.getProperty("user.dir");
    public List<String> licenses = new ArrayList<>();
    public Map<String, String> javaVersions = new HashMap<>();
    public String gradleVersion = "";
    public String templateVersion = "";
}
