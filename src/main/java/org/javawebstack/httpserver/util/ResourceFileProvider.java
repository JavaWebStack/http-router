package org.javawebstack.httpserver.util;

import java.io.InputStream;

public class ResourceFileProvider implements FileProvider {

    private final ClassLoader classLoader;
    private final String prefix;

    public ResourceFileProvider(ClassLoader classLoader, String prefix) {
        this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
        this.prefix = prefix;
    }

    public ResourceFileProvider(String prefix) {
        this(null, prefix);
    }

    public InputStream getFile(String path) {
        return classLoader.getResourceAsStream(prefix + ((!prefix.endsWith("/") && !path.startsWith("/")) ? "/" : "") + path);
    }

}
