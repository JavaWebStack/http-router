package eu.bebendorf.ajwf.transformer.route;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class RouteParamTransformer {
    private final Map<String, String> regex = new HashMap<>();
    private final Map<String, Function<Object, Object>> transformers = new HashMap<>();
    protected void add(String name, String regex, Function<Object, Object> transformer){
        if(name.contains("|")){
            for(String s : name.split("\\|")){
                if(s.length() == 0)
                    continue;
                add(s, regex, transformer);
            }
            return;
        }
        this.regex.put(name, regex);
        transformers.put(name, transformer);
    }
    protected void extend(String parent, String name, Function<Object, Object> transformer){
        extend(this, parent, name, transformer);
    }
    protected void extend(RouteParamTransformerProvider parentTransformerProvider, String parent, String name, Function<Object, Object> transformer){
        extend(parentTransformerProvider.getRouteParamTransformer(parent), parent, name, transformer);
    }
    protected void extend(RouteParamTransformer parentTransformer, String parent, String name, Function<Object, Object> transformer){
        add(name, regex(parent), s -> transformer.apply(parentTransformer.transform(parent, (String) s)));
    }
    public boolean canTransform(String name){
        return regex.containsKey(name);
    }
    public String regex(String name){
        return regex.get(name);
    }
    public Object transform(String name, String source){
        if(transformers.containsKey(name)){
            return transformers.get(name).apply(source);
        }
        return source;
    }
}