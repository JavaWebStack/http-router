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

    public RouteBinder(HTTPServer server) {
        this.server = server;
    }

    public void bind(String globalPrefix, Object controller) {
        List<String> prefixes = new ArrayList<>(Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(PathPrefix.class)).map(PathPrefix::value).collect(Collectors.toList()));
        if (prefixes.size() == 0)
            prefixes.add("");
        With with = Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(With.class)).findFirst().orElse(null);
        class Bind {
            final HttpMethod method;
            final String path;

            public Bind(HttpMethod method, String path) {
                this.method = method;
                this.path = path;
            }
        }
        for (Method method : controller.getClass().getDeclaredMethods()) {
            List<Bind> binds = new ArrayList<>();
            With methodWith = getAnnotations(With.class, method).stream().findFirst().orElse(null);
            List<String> middlewares = new ArrayList<>();
            if (with != null)
                middlewares.addAll(Arrays.asList(with.value()));
            if (methodWith != null)
                middlewares.addAll(Arrays.asList(methodWith.value()));
            for (Get a : getAnnotations(Get.class, method)) {
                bindMiddlewares(HttpMethod.GET, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HttpMethod.GET, a.value()));
            }
            for (Post a : getAnnotations(Post.class, method)) {
                bindMiddlewares(HttpMethod.POST, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HttpMethod.POST, a.value()));
            }
            for (Put a : getAnnotations(Put.class, method)) {
                bindMiddlewares(HttpMethod.PUT, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HttpMethod.PUT, a.value()));
            }
            for (Delete a : getAnnotations(Delete.class, method)) {
                bindMiddlewares(HttpMethod.DELETE, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HttpMethod.DELETE, a.value()));
            }
            if (binds.size() > 0) {
                BindHandler handler = new BindHandler(server, controller, method);
                for (String prefix : prefixes) {
                    for (Bind bind : binds) {
                        server.route(bind.method, buildPattern(globalPrefix, prefix, bind.path), handler);
                    }
                }
            }
        }
    }

    private void bindMiddlewares(HttpMethod method, String globalPrefix, List<String> prefixes, String path, List<String> middlewares) {
        for (String name : middlewares) {
            RequestHandler before = server.getBeforeMiddleware(name);
            AfterRequestHandler after = server.getAfterMiddleware(name);
            for (String prefix : prefixes) {
                if (before == null && after == null) {
                    server.getLogger().warning("Middleware \"" + name + "\" not found!");
                    continue;
                }
                if (before != null)
                    server.beforeRoute(method, buildPattern(globalPrefix, prefix, path), before);
                if (after != null)
                    server.afterRoute(method, buildPattern(globalPrefix, prefix, path), after);
            }
        }
    }

    private static String buildPattern(String globalPrefix, String prefix, String path) {
        String pattern = globalPrefix != null ? globalPrefix : "";
        if (pattern.endsWith("/"))
            pattern = pattern.substring(0, pattern.length() - 1);
        if (prefix.length() > 0) {
            if (!prefix.startsWith("/"))
                pattern += "/";
            pattern += prefix;
            if (pattern.endsWith("/"))
                pattern = pattern.substring(0, pattern.length() - 1);
        }
        if (path.length() > 0) {
            if (!path.startsWith("/"))
                pattern += "/";
            pattern += path;
            if (pattern.endsWith("/"))
                pattern = pattern.substring(0, pattern.length() - 1);
        }
        return pattern;
    }

    private static <T extends Annotation> List<T> getAnnotations(Class<T> type, Method method) {
        return Arrays.asList(method.getDeclaredAnnotationsByType(type));
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, Method method, int param) {
        if (param < 0)
            return null;
        Parameter[] parameters = method.getParameters();
        if (param >= parameters.length)
            return null;
        T[] annotations = parameters[param].getDeclaredAnnotationsByType(type);
        return annotations.length == 0 ? null : annotations[0];
    }

    private static class BindHandler implements RequestHandler {

        private final HTTPServer service;
        private final Object controller;
        private final Method method;
        private final Object[] parameterTypes;

        public BindHandler(HTTPServer service, Object controller, Method method) {
            this.service = service;
            this.controller = controller;
            this.method = method;
            method.setAccessible(true);
            Class<?>[] types = method.getParameterTypes();
            parameterTypes = new Object[types.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Attrib attrib = getAnnotation(Attrib.class, method, i);
                if (attrib != null) {
                    parameterTypes[i] = attrib;
                    continue;
                }
                Query query = getAnnotation(Query.class, method, i);
                if (query != null) {
                    parameterTypes[i] = query;
                    continue;
                }
                Body body = getAnnotation(Body.class, method, i);
                if (body != null) {
                    parameterTypes[i] = body;
                    continue;
                }
                Path pathParam = getAnnotation(Path.class, method, i);
                if (pathParam != null) {
                    parameterTypes[i] = pathParam;
                    continue;
                }
                parameterTypes[i] = types[i];
            }
        }

        public Object handle(Exchange exchange) {
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < args.length; i++) {
                if (parameterTypes[i] == null)
                    continue;
                if (parameterTypes[i] instanceof Body) {
                    args[i] = exchange.body(method.getParameterTypes()[i]);
                    continue;
                }
                if (parameterTypes[i] instanceof Attrib) {
                    Attrib attrib = (Attrib) parameterTypes[i];
                    args[i] = exchange.attrib(attrib.value());
                    continue;
                }
                if (parameterTypes[i] instanceof Query) {
                    Query query = (Query) parameterTypes[i];
                    args[i] = exchange.path(query.value());
                    continue;
                }
                if (parameterTypes[i] instanceof Path) {
                    Path path = (Path) parameterTypes[i];
                    args[i] = exchange.path(path.value().toLowerCase(Locale.ROOT));
                    continue;
                }
                for (RouteAutoInjector autoInjector : service.getRouteAutoInjectors()) {
                    args[i] = autoInjector.getValue(exchange, (Class<?>) parameterTypes[i]);
                    if (args[i] != null)
                        break;
                }
                if (args[i] == null && service.getInjector() != null)
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
