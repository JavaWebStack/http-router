package org.javawebstack.http.router.multipart;

import org.apache.commons.fileupload.MultipartStream;
import org.javawebstack.http.router.Exchange;
import org.javawebstack.http.router.multipart.content.PartContent;
import org.javawebstack.http.router.multipart.content.PartContentCache;
import org.javawebstack.http.router.util.HeaderValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Part {

    final Map<String, String> headers;
    final PartContent content;
    String name;
    String contentType;

    public Part(Map<String, String> headers, PartContent content) {
        this.headers = headers;
        this.content = content;
        if(headers.containsKey("content-disposition")) {
            HeaderValue contentDisposition = new HeaderValue(headers.get("content-disposition"));
            if(contentDisposition.getDirectives().containsKey("name"))
                name = contentDisposition.getDirectives().get("name");
        }
        if(headers.containsKey("content-type")) {
            contentType = new HeaderValue(headers.get("content-type")).getValue();
        }
    }

    public static List<Part> parse(InputStream inputStream, byte[] boundary, PartContentCache cache) throws IOException {
        MultipartStream stream = new MultipartStream(inputStream, boundary, 1024, null);
        List<Part> parts = new ArrayList<>();
        if(stream.skipPreamble()) {
            do {
                Map<String, String> headers = Stream.of(
                                stream.readHeaders().split("\r?\n")
                        )
                        .filter(l -> !l.trim().isEmpty())
                        .map(l -> l.split(": ", 2))
                        .collect(Collectors.toMap(
                                a -> a[0].toLowerCase(Locale.ROOT),
                                a -> a.length == 2 ? a[1] : ""
                        ));
                PartContent c = cache.store(os -> {
                    try {
                        stream.readBodyData(os);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                parts.add(new Part(headers, c));
            } while (stream.readBoundary());
        }
        return parts;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getContentStream() {
        return content.read();
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContentBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream stream = getContentStream();
        int b;
        try {
            while ((b = stream.read()) != -1)
                baos.write(b);
            stream.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }

    public void discard() {
        content.discard();
    }

}
