package org.javawebstack.http.router.transformer.response;

import org.javawebstack.http.router.Exchange;

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
