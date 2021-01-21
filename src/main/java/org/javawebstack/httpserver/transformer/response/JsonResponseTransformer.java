package org.javawebstack.httpserver.transformer.response;

import org.javawebstack.abstractdata.AbstractMapper;
import org.javawebstack.abstractdata.NamingPolicy;

public class JsonResponseTransformer implements ResponseTransformer {

    private final AbstractMapper mapper;
    private boolean ignoreStrings = false;

    public JsonResponseTransformer(){
        this(new AbstractMapper().setNamingPolicy(NamingPolicy.SNAKE_CASE));
    }

    public JsonResponseTransformer(AbstractMapper mapper){
        this.mapper = mapper;
    }

    public JsonResponseTransformer ignoreStrings(){
        this.ignoreStrings = true;
        return this;
    }

    public String transform(Object object) {
        if(object instanceof byte[])
            return null;
        if(ignoreStrings && object instanceof String)
            return null;
        return mapper.toAbstract(object).toJsonString();
    }
}
