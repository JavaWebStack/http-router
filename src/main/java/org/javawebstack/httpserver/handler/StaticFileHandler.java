package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.helper.MimeType;
import org.javawebstack.httpserver.util.FileProvider;

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
        String path = (String) exchange.pathVariables.get("path");
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
