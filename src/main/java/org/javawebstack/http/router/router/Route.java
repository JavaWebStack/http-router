package org.javawebstack.http.router.router;

import org.javawebstack.http.router.Exchange;
import org.javawebstack.http.router.HTTPMethod;
import org.javawebstack.http.router.HTTPRoutingOptions;
import org.javawebstack.http.router.handler.AfterRequestHandler;
import org.javawebstack.http.router.handler.RequestHandler;
import org.javawebstack.http.router.transformer.route.RouteParamTransformerProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    private String name;
    private final RouteParamTransformerProvider routeParamTransformerProvider;
    private final HTTPMethod method;
    private final String pattern;
    private final Pattern compiledPattern;
    private final Map<String, String> variables = new HashMap<>();
    private List<RequestHandler> handlers;
    private List<AfterRequestHandler> afterHandlers;

    public Route(RouteParamTransformerProvider routeParamTransformerProvider, HTTPMethod method, String pattern, HTTPRoutingOptions options, List<RequestHandler> handlers) {
        this(routeParamTransformerProvider, method, pattern, options, ":", handlers);
    }

    public Route(RouteParamTransformerProvider routeParamTransformerProvider, HTTPMethod method, String pattern, HTTPRoutingOptions options, String variableDelimiter, List<RequestHandler> handlers) {
        this.handlers = handlers;
        this.method = method;
        this.routeParamTransformerProvider = routeParamTransformerProvider;
        this.pattern = pattern;
        pattern = pattern.toLowerCase(Locale.ENGLISH);
        if(options.isIgnoreTrailingSlash()) {
            if (pattern.endsWith("/"))
                pattern = pattern.substring(0, pattern.length() - 1);
        }
        if (!pattern.startsWith("/"))
            pattern = "/" + pattern;
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        StringBuilder text = new StringBuilder();
        boolean inVar = false;
        while (pos < pattern.length()) {
            char c = pattern.charAt(pos);
            if (c == '{') {
                if (inVar) {
                    throw new RuntimeException("Unexpected character '{' in route at position " + pos);
                }
                if (text.length() > 0) {
                    sb.append("(" + prepareRegex(options, text.toString()) + ")");
                    text = new StringBuilder();
                }
                inVar = true;
                pos++;
                continue;
            }
            if (c == '}') {
                if (!inVar) {
                    throw new RuntimeException("Unexpected character '}' in route at position " + pos);
                }
                if (text.length() > 0) {
                    String variableName = text.toString();
                    String type = "string";
                    int loc = variableName.indexOf(variableDelimiter);
                    if (loc != -1) {
                        String t = variableName.substring(0, loc).toLowerCase(Locale.ENGLISH);
                        variableName = variableName.substring(loc + 1);
                        if (routeParamTransformerProvider.getRouteParamTransformer(t) != null) {
                            type = t;
                        }
                    }
                    sb.append("(?<" + regexEscape(variableName.toLowerCase(Locale.ROOT)) + ">" + routeParamTransformerProvider.getRouteParamTransformer(type).regex(type) + ")");
                    variables.put(variableName, type);
                    text = new StringBuilder();
                }
                inVar = false;
                pos++;
                continue;
            }
            text.append(c);
            pos++;
        }
        if (inVar) {
            throw new RuntimeException("Unexpected end in route");
        }
        if (text.length() > 0) {
            sb.append("(" + prepareRegex(options, text.toString()) + ")");
        }
        if(options.isIgnoreTrailingSlash()) {
            sb.append("/?");
        }
        this.compiledPattern = Pattern.compile(sb.toString());
    }

    public Route setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public Route setAfterHandlers(List<AfterRequestHandler> afterHandlers) {
        this.afterHandlers = afterHandlers;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, Object> match(Exchange exchange) {
        return match(exchange, exchange.getMethod(), exchange.getPath());
    }

    public Map<String, Object> match(Exchange exchange, HTTPMethod method, String path) {
        if (this.method != method)
            return null;
        Matcher matcher = compiledPattern.matcher(path);
        if (matcher.matches()) {
            Map<String, Object> params = new HashMap<>();
            for (String name : variables.keySet()) {
                params.put(name, routeParamTransformerProvider.getRouteParamTransformer(variables.get(name)).transform(variables.get(name), exchange, matcher.group(name)));
            }
            return params;
        }
        return null;
    }

    public List<RequestHandler> getHandlers() {
        return handlers;
    }

    public List<AfterRequestHandler> getAfterHandlers() {
        return afterHandlers;
    }

    private static String prepareRegex(HTTPRoutingOptions options, String text) {
        text = regexEscape(text);
        if(options.isCaseInsensitive())
            text = ignoreCase(text);
        return text;
    }

    private static String ignoreCase(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(Character.isAlphabetic(c)) {
                char inverted = Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c);
                if(c != inverted) {
                    sb.append("[").append(c).append(inverted).append("]");
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String regexEscape(String s) {
        for (char c : "\\<([{^-=$!|]})?*+.>".toCharArray()) {
            s = s.replace(String.valueOf(c), "\\" + c);
        }
        return s;
    }

}
