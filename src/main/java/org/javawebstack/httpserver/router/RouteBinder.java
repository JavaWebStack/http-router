package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.handler.AfterRequestHandler;
import org.javawebstack.httpserver.handler.RequestHandler;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.router.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RouteBinder {

    private final HTTPServer server;

    public RouteBinder(HTTPServer server){
        this.server = server;
    }

    public void bind(String globalPrefix, Object controller){
        List<String> prefixes = new ArrayList<>(Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(PathPrefix.class)).map(PathPrefix::value).collect(Collectors.toList()));
        if(prefixes.size() == 0)
            prefixes.add("");
        With with = Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(With.class)).findFirst().orElse(null);
        class Bind {
            final HttpMethod method;
            final String path;
            public Bind(HttpMethod method, String path){
                this.method = method;
                this.path = path;
            }
        }
        for(Method method : controller.getClass().getDeclaredMethods()){
            List<Bind> binds = new ArrayList<>();
            With methodWith = getAnnotations(With.class, method).stream().findFirst().orElse(null);
            List<String> middlewares = new ArrayList<>();
            if(with != null)
                middlewares.addAll(Arrays.asList(with.value()));
            if(methodWith != null)
                middlewares.addAll(Arrays.asList(methodWith.value()));
            for(Get a : getAnnotations(Get.class, method)){
                for(String name : middlewares){
                    RequestHandler before = server.getBeforeMiddleware(name);
                    if(before != null)
                        server.beforeGet(a.value(), before);
                    AfterRequestHandler after = server.getAfterMiddleware(name);
                    if(after != null)
                        server.afterGet(a.value(), after);
                }
                binds.add(new Bind(HttpMethod.GET, a.value()));
            }
            for(Post a : getAnnotations(Post.class, method)){
                for(String name : middlewares){
                    RequestHandler before = server.getBeforeMiddleware(name);
                    if(before != null)
                        server.beforePost(a.value(), before);
                    AfterRequestHandler after = server.getAfterMiddleware(name);
                    if(after != null)
                        server.afterPost(a.value(), after);
                }
                binds.add(new Bind(HttpMethod.POST, a.value()));
            }
            for(Put a : getAnnotations(Put.class, method)){
                for(String name : middlewares){
                    RequestHandler before = server.getBeforeMiddleware(name);
                    if(before != null)
                        server.beforePut(a.value(), before);
                    AfterRequestHandler after = server.getAfterMiddleware(name);
                    if(after != null)
                        server.afterPut(a.value(), after);
                }
                binds.add(new Bind(HttpMethod.PUT, a.value()));
            }
            for(Delete a : getAnnotations(Delete.class, method)){
                for(String name : middlewares){
                    RequestHandler before = server.getBeforeMiddleware(name);
                    if(before != null)
                        server.beforeDelete(a.value(), before);
                    AfterRequestHandler after = server.getAfterMiddleware(name);
                    if(after != null)
                        server.afterDelete(a.value(), after);
                }
                binds.add(new Bind(HttpMethod.DELETE, a.value()));
            }
            if(binds.size() > 0){
                BindHandler handler = new BindHandler(server, controller, method);
                for(String prefix : prefixes){
                    for(Bind bind : binds){
                        server.route(bind.method, buildPattern(globalPrefix, prefix, bind.path), handler);
                    }
                }
            }
        }
    }

    private static String buildPattern(String globalPrefix, String prefix, String path){
        String pattern = globalPrefix != null ? globalPrefix : "";
        if(pattern.endsWith("/"))
            pattern = pattern.substring(0, pattern.length()-1);
        if(prefix.length() > 0){
            if(!prefix.startsWith("/"))
                pattern+="/";
            pattern += prefix;
            if(pattern.endsWith("/"))
                pattern = pattern.substring(0, pattern.length()-1);
        }
        if(path.length() > 0){
            if(!path.startsWith("/"))
                pattern+="/";
            pattern += path;
            if(pattern.endsWith("/"))
                pattern = pattern.substring(0, pattern.length()-1);
        }
        return pattern;
    }

    private static <T extends Annotation> List<T> getAnnotations(Class<T> type, Method method){
        return Arrays.asList(method.getDeclaredAnnotationsByType(type));
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, Method method, int param){
        if(param < 0)
            return null;
        Parameter[] parameters = method.getParameters();
        if(param >= parameters.length)
            return null;
        T[] annotations = parameters[param].getDeclaredAnnotationsByType(type);
        return annotations.length == 0 ? null : annotations[0];
    }

    private static class BindHandler implements RequestHandler {

        private final HTTPServer service;
        private final Object controller;
        private final Method method;
        private final Object[] parameterTypes;

        public BindHandler(HTTPServer service, Object controller, Method method){
            this.service = service;
            this.controller = controller;
            this.method = method;
            method.setAccessible(true);
            Class<?>[] types = method.getParameterTypes();
            parameterTypes = new Object[types.length];
            for(int i=0; i<parameterTypes.length; i++) {
                Attrib attrib = getAnnotation(Attrib.class, method, i);
                if(attrib != null){
                    parameterTypes[i] = attrib;
                    continue;
                }
                Query query = getAnnotation(Query.class, method, i);
                if(query != null){
                    parameterTypes[i] = query;
                    continue;
                }
                Body body = getAnnotation(Body.class, method, i);
                if(body != null){
                    parameterTypes[i] = body;
                    continue;
                }
                Path pathParam = getAnnotation(Path.class, method, i);
                if(pathParam != null){
                    parameterTypes[i] = pathParam;
                    continue;
                }
                parameterTypes[i] = types[i];
            }
        }

        public Object handle(Exchange exchange) {
            Object[] args = new Object[parameterTypes.length];
            for(int i=0; i<args.length; i++){
                if(parameterTypes[i] == null)
                    continue;
                if(parameterTypes[i] instanceof Body){
                    args[i] = exchange.body(method.getParameterTypes()[i]);
                    continue;
                }
                if(parameterTypes[i] instanceof Attrib){
                    Attrib attrib = (Attrib) parameterTypes[i];
                    args[i] = exchange.attrib(attrib.value());
                    continue;
                }
                if(parameterTypes[i] instanceof Query){
                    Query query = (Query) parameterTypes[i];
                    args[i] = exchange.parameters.get(query.value());
                    continue;
                }
                if(parameterTypes[i] instanceof Path){
                    Path path = (Path) parameterTypes[i];
                    args[i] = exchange.pathVariables.get(path.value().toLowerCase(Locale.ROOT));
                    continue;
                }
                for(RouteAutoInjector autoInjector : service.getRouteAutoInjectors()){
                    args[i] = autoInjector.getValue(exchange, (Class<?>) parameterTypes[i]);
                    if(args[i] != null)
                        break;
                }
                if(args[i] == null && service.getInjector() != null)
                    args[i] = service.getInjector().getInstance((Class<?>) parameterTypes[i]);
            }
            try {
                return method.invoke(controller, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

}
