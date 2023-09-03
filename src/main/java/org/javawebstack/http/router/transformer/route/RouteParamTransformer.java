package org.javawebstack.http.router.transformer.route;

import org.javawebstack.http.router.Exchange;

import java.util.HashMap;
import java.util.Map;

public abstract class RouteParamTransformer {
    private final Map<String, String> regex = new HashMap<>();
    private final Map<String, RouteParamTransformFunction> transformers = new HashMap<>();

    protected void add(String name, String regex, RouteParamTransformFunction transformer) {
        if (name.contains("|")) {
            for (String s : name.split("\\|")) {
                if (s.length() == 0)
                    continue;
                add(s, regex, transformer);
            }
            return;
        }
        this.regex.put(name, regex);
        transformers.put(name, transformer);
    }

    protected void extend(String parent, String name, RouteParamTransformFunction transformer) {
        extend(this, parent, name, transformer);
    }

    protected void extend(RouteParamTransformerProvider parentTransformerProvider, String parent, String name, RouteParamTransformFunction transformer) {
        extend(parentTransformerProvider.getRouteParamTransformer(parent), parent, name, transformer);
    }

    protected void extend(RouteParamTransformer parentTransformer, String parent, String name, RouteParamTransformFunction transformer) {
        add(name, regex(parent), (e, s) -> transformer.transform(e, parentTransformer.transform(parent, e, (String) s)));
    }

    public boolean canTransform(String name) {
        return regex.containsKey(name);
    }

    public String regex(String name) {
        return regex.get(name);
    }

    public Object transform(String name, Exchange exchange, String source) {
        if (transformers.containsKey(name)) {
            return transformers.get(name).transform(exchange, source);
        }
        return source;
    }
}