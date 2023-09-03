package org.javawebstack.http.router.transformer.response;

import org.javawebstack.abstractdata.mapper.Mapper;
import org.javawebstack.abstractdata.mapper.naming.NamingPolicy;
import org.javawebstack.http.router.Exchange;

public class SerializedResponseTransformer implements ResponseTransformer {

    private final Mapper mapper;
    private boolean ignoreStrings;

    public SerializedResponseTransformer() {
        this((new Mapper()).namingPolicy(NamingPolicy.SNAKE_CASE));
    }

    public SerializedResponseTransformer(Mapper mapper) {
        this.ignoreStrings = false;
        this.mapper = mapper;
    }

    public SerializedResponseTransformer ignoreStrings() {
        this.ignoreStrings = true;
        return this;
    }

    public String transform(Exchange exchange, Object object) {
        if (object instanceof byte[]) {
            return null;
        } else {
            if (this.ignoreStrings && object instanceof String)
                return null;
            String accept = exchange.header("Accept");

            if (accept != null) {
                switch (accept.toLowerCase()) {
                    case "application/x-yaml":
                    case "application/yaml":
                    case "text/yaml":
                    case "text/x-yaml":
                        exchange.contentType(accept);
                        return this.mapper.map(object).toYaml();
                    case "application/x-www-form-urlencoded":
                        exchange.contentType(accept);
                        return this.mapper.map(object).toFormDataString();
                }
            }

            exchange.contentType("application/json");
            return this.mapper.map(object).toJsonString();
        }
    }
}