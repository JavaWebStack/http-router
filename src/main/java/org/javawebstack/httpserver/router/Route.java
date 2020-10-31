package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.handler.RequestHandler;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.transformer.route.RouteParamTransformerProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {
    private final RouteParamTransformerProvider routeParamTransformerProvider;
    private final HttpMethod method;
    private final Pattern pattern;
    private final Map<String, String> variables = new HashMap<>();
    private List<RequestHandler> handlers;
    public Route(RouteParamTransformerProvider routeParamTransformerProvider, HttpMethod method, String pattern, List<RequestHandler> handlers){
        this(routeParamTransformerProvider, method, pattern, ":", handlers);
    }
    public Route(RouteParamTransformerProvider routeParamTransformerProvider, HttpMethod method, String pattern, String variableDelimiter, List<RequestHandler> handlers){
        this.handlers = handlers;
        this.method = method;
        this.routeParamTransformerProvider = routeParamTransformerProvider;
        pattern = pattern.toLowerCase(Locale.ENGLISH);
        if(pattern.endsWith("/"))
            pattern = pattern.substring(0, pattern.length()-1);
        if(!pattern.startsWith("/"))
            pattern = "/" + pattern;
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        StringBuilder text = new StringBuilder();
        boolean inVar = false;
        while (pos < pattern.length()){
            if(pattern.charAt(pos) == '{'){
                if(inVar){
                    throw new RuntimeException("Unexpected character '{' in route at position "+pos);
                }
                if(text.length() > 0){
                    sb.append("("+regexEscape(text.toString())+")");
                    text = new StringBuilder();
                }
                inVar = true;
                pos++;
                continue;
            }
            if(pattern.charAt(pos) == '}'){
                if(!inVar){
                    throw new RuntimeException("Unexpected character '}' in route at position "+pos);
                }
                if(text.length() > 0){
                    String variableName = text.toString();
                    String type = "string";
                    int loc = variableName.indexOf(variableDelimiter);
                    if(loc != -1){
                        String t = variableName.substring(0, loc).toLowerCase(Locale.ENGLISH);
                        if(routeParamTransformerProvider.getRouteParamTransformer(t) != null){
                            type = t;
                            variableName = variableName.substring(loc+1);
                        }
                    }
                    sb.append("(?<"+regexEscape(variableName.toLowerCase(Locale.ROOT))+">"+routeParamTransformerProvider.getRouteParamTransformer(type).regex(type)+")");
                    variables.put(variableName, type);
                    text = new StringBuilder();
                }
                inVar = false;
                pos++;
                continue;
            }
            text.append(pattern.charAt(pos));
            pos++;
        }
        if(inVar){
            throw new RuntimeException("Unexpected end in route");
        }
        if(text.length() > 0){
            sb.append("("+regexEscape(text.toString())+")");
        }
        this.pattern = Pattern.compile(sb.toString());
    }
    public Map<String, Object> match(HttpMethod method, String path){
        if(this.method != method)
            return null;
        Matcher matcher = pattern.matcher(path);
        if(matcher.matches()){
            Map<String, Object> params = new HashMap<>();
            for(String name : variables.keySet()){
                params.put(name, routeParamTransformerProvider.getRouteParamTransformer(variables.get(name)).transform(variables.get(name), matcher.group(name)));
            }
            return params;
        }
        return null;
    }
    public List<RequestHandler> getHandlers(){
        return handlers;
    }
    private static String regexEscape(String s){
        s = s.replace("\\", "\\\\");
        for(char c : "<([{^-=\\$!|]})?*+.>".toCharArray()){
            s = s.replace(String.valueOf(c), "\\"+c);
        }
        return s;
    }

}
