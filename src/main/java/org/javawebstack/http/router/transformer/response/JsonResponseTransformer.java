package org.javawebstack.http.router.transformer.response;

import org.javawebstack.abstractdata.mapper.Mapper;
import org.javawebstack.abstractdata.mapper.naming.NamingPolicy;
import org.javawebstack.http.router.Exchange;

public class JsonResponseTransformer implements ResponseTransformer {

    private final Mapper mapper;
    private boolean ignoreStrings = false;

    public JsonResponseTransformer() {
        this(new Mapper().namingPolicy(NamingPolicy.SNAKE_CASE));
    }

    public JsonResponseTransformer(Mapper mapper) {
        this.mapper = mapper;
    }

    public JsonResponseTransformer ignoreStrings() {
        this.ignoreStrings = true;
        return this;
    }

    public String transform(Exchange exchange, Object object) {
        if (object instanceof byte[])
            return null;
        if (ignoreStrings && object instanceof String)
            return null;
        return mapper.map(object).toJsonString();
    }

}
