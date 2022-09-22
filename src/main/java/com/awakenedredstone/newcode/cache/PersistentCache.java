package com.awakenedredstone.newcode.cache;

import java.util.HashMap;
import java.util.Map;

public class PersistentCache {
    public String generationPath = System.getProperty("user.dir");
    public Map<String, String> licenses = new HashMap<>();
    public Map<String, String> javaVersions = new HashMap<>();
    public String gradleVersion = "";
    public String templateVersion = "";
}
