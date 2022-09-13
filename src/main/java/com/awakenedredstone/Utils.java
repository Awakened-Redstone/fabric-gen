package com.awakenedredstone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    public static void unzip(InputStream stream, Path destDir) throws IOException {
        File dir = destDir.toFile();
        // create output directory if it doesn't exist
        if (!dir.exists()) {
            Files.delete(destDir);
            dir.mkdirs();
        }
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(destDir + File.separator + fileName);
            System.out.println("Unzipping to " + newFile.getAbsolutePath());
            if (ze.isDirectory()) {
                newFile.mkdirs();
            } else {
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
            }
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        //close last ZipEntry
        zis.closeEntry();
        zis.close();
    }
}
