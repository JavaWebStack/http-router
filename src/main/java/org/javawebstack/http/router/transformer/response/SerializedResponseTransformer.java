package org.javawebstack.http.router.transformer.response;

import org.javawebstack.abstractdata.mapper.Mapper;
import org.javawebstack.abstractdata.mapper.naming.NamingPolicy;
import org.javawebstack.http.router.Exchange;
import org.javawebstack.http.router.util.HeaderValue;

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
            String rawAccept = exchange.header("Accept");

            if (rawAccept != null) {
                HeaderValue accept = new HeaderValue(rawAccept);

                exchange.contentType(rawAccept);
                switch (accept.getValue().toLowerCase()) {
                    case "application/x-yaml":
                    case "application/yaml":
                    case "text/yaml":
                    case "text/x-yaml":
                        return this.mapper.map(object).toYaml();
                    case "application/x-www-form-urlencoded":
                        return this.mapper.map(object).toFormDataString();
                    case "application/json":
                        return this.mapper.map(object).toJsonString();
                    default:
                        exchange.status(406);
                        return "Not Acceptable";
                }
            }

            exchange.contentType("application/json");
            return this.mapper.map(object).toJsonString();
        }
    }
}