package org.javawebstack.http.router.multipart.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class InMemoryCache implements PartContentCache {

    public PartContent store(InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        try {
            while ((b = stream.read()) != -1)
                baos.write(b);
            stream.close();
            return new ByteArrayContent(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PartContent store(Consumer<OutputStream> streamConsumer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamConsumer.accept(baos);
        return new ByteArrayContent(baos.toByteArray());
    }

    public void cleanup() {
        // Nothing to cleanup
    }

}
