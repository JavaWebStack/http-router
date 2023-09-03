package org.javawebstack.http.router.multipart.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public interface PartContentCache {

    default PartContent store(InputStream stream) {
        return store(os -> {
            int b;
            try {
                while ((b = stream.read()) != -1)
                    os.write(b);
                os.close();
                stream.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    PartContent store(Consumer<OutputStream> streamConsumer);

    void cleanup();

}
