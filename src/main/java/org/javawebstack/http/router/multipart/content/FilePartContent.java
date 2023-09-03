package org.javawebstack.http.router.multipart.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilePartContent implements PartContent {

    private final File file;

    public FilePartContent(File file) {
        this.file = file;
    }

    public InputStream read() {
        if(!file.exists())
            throw new RuntimeException("File does not exist anymore");
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void discard() {
        if(file.exists())
            file.delete();
    }

}
