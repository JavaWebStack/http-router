package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.helper.MimeType;

import java.io.*;

public class StaticDirectoryHandler implements RequestHandler {
    private File directory;

    public StaticDirectoryHandler(File directory){
        if (directory.getPath().equals(""))
            directory = new File(".");
        this.directory = directory;
    }

    @Override
    public Object handle(Exchange exchange) {
        String path = (String) exchange.pathVariables.get("path");
        System.out.println(path);

        File file = new File(path);

        if (!file.exists() || path.contains(".."))
            return null;

        String extension = "";

        String[] splitPath = file.getPath().split("\\.");
        if (splitPath.length > 0)
            extension = splitPath[splitPath.length-1];

        try {
            InputStream stream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int r;
            while((r = stream.read(buffer)) != -1)
                exchange.write(buffer, 0, r);
            stream.close();


            exchange.contentType(MimeType.byExtension(extension));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
