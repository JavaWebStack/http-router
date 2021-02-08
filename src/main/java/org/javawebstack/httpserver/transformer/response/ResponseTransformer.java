package org.javawebstack.httpserver.transformer.response;

import java.nio.charset.StandardCharsets;

public interface ResponseTransformer {
    String transform(Object object);

    default byte[] transformBytes(Object object) {
        if (object instanceof byte[])
            return (byte[]) object;
        String str = transform(object);
        if (str == null)
            return null;
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
