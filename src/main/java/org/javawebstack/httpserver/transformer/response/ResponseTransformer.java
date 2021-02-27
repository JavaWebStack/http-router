package org.javawebstack.httpserver.transformer.response;

import org.javawebstack.httpserver.Exchange;

import java.nio.charset.StandardCharsets;

public interface ResponseTransformer {
    String transform(Exchange exchange, Object object);

    default byte[] transformBytes(Exchange exchange, Object object) {
        if (object instanceof byte[])
            return (byte[]) object;
        String str = transform(exchange, object);
        if (str == null)
            return null;
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
