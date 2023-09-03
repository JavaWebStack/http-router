package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;
import org.javawebstack.http.router.util.MimeType;
import org.javawebstack.http.router.util.FileProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StaticFileHandler implements RequestHandler {

    private final List<FileProvider> providers = new ArrayList<>();

    public StaticFileHandler add(FileProvider provider) {
        providers.add(provider);
        return this;
    }

    public Object handle(Exchange exchange) {
        String path = exchange.path("path");
        InputStream stream = null;
        for (FileProvider provider : providers) {
            stream = provider.getFile(path);
            if (stream != null)
                break;
        }
        if (stream == null)
            return null;
        exchange.contentType(MimeType.byFileName(path));
        try {
            exchange.write(stream);
        } catch (IOException exception) {
            return null;
        }
        return "";
    }

}
