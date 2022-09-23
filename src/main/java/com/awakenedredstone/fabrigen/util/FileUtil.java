package com.awakenedredstone.fabrigen.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUtil {
    public static String readFile(File file) throws IOException {
        if (file.isDirectory()) throw new IOException("File must not be a directory!");
        FileInputStream inputStream = new FileInputStream(file);
        String string = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        inputStream.close();
        return string;
    }

    public static void writeFile(File file, String fileContent) throws IOException {
        if (file.exists() && file.isDirectory()) throw new IOException("File must not be a directory!");
        FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8);
    }
}
