package org.javawebstack.http.router.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class DirectoryFileProvider implements FileProvider {

    private final File directory;

    public DirectoryFileProvider(File directory) {
        this.directory = directory;
    }

    public InputStream getFile(String path) {
        File file = new File(directory, path);
        if (!file.exists() || !file.isFile() || !isInside(directory, file))
            return null;
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException ignored) {
        }
        return null;
    }

    private static boolean isInside(File directory, File file) {
        if (file.getParentFile().equals(directory))
            return true;
        if (directory.getParentFile() == null)
            return false;
        return isInside(directory.getParentFile(), file);
    }

}
