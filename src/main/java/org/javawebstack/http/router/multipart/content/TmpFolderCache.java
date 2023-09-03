package org.javawebstack.http.router.multipart.content;

import java.io.*;
import java.util.UUID;
import java.util.function.Consumer;

public class TmpFolderCache implements PartContentCache {

    private final File folder;
    private final long cacheLifetime = 60000;

    public TmpFolderCache(File folder) {
        this.folder = folder;
        if(!folder.exists())
            folder.mkdirs();
    }

    public PartContent store(Consumer<OutputStream> streamConsumer) {
        String id = UUID.randomUUID().toString();
        File file = new File(folder, id + ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            file.deleteOnExit();
            streamConsumer.accept(fos);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FilePartContent(file);
    }

    public void cleanup() {
        for(File file : folder.listFiles()) {
            if(file.isDirectory() || file.getName().endsWith(".tmp"))
                continue;
            long lifetime = System.currentTimeMillis() - file.lastModified();
            if(lifetime < cacheLifetime)
                continue;
            String key = file.getName().substring(0, file.getName().length()-4);
            try {
                UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            file.delete();
        }
    }

}
