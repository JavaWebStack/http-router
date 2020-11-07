package org.javawebstack.httpserver.transformer.response;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

public class JsonResponseTransformer implements ResponseTransformer {

    private final Gson gson;
    private boolean ignoreStrings = false;

    public JsonResponseTransformer(){
        this(new GsonBuilder().disableHtmlEscaping()
                .setExclusionStrategies(new ExclusionStrategy() {
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        if (fieldAttributes.getAnnotation(Expose.class) != null && !fieldAttributes.getAnnotation(Expose.class).serialize())
                            return true;
                        return false;
                    }
                    public boolean shouldSkipClass(Class<?> aClass) { return false;  }
                })
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create());
    }

    public JsonResponseTransformer(Gson gson){
        this.gson = gson;
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
        return gson.toJson(object);
    }
}
