package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.handler.AfterRequestHandler;
import org.javawebstack.httpserver.handler.RequestHandler;
import org.javawebstack.httpserver.handler.WebSocketHandler;
import org.javawebstack.httpserver.router.annotation.PathPrefix;
import org.javawebstack.httpserver.router.annotation.With;
import org.javawebstack.httpserver.router.annotation.params.*;
import org.javawebstack.httpserver.router.annotation.verbs.*;
import org.javawebstack.httpserver.websocket.WebSocket;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
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
            final HTTPMethod method;
            final String path;

            public Bind(HTTPMethod method, String path) {
                this.method = method;
                this.path = path;
            }
        }
        Map<String, WebSocketBindHandler> websocketHandlers = new HashMap<>();
        for (Method method : getMethodsRecursive(controller.getClass())) {
            List<Bind> binds = new ArrayList<>();
            With methodWith = getAnnotations(With.class, method).stream().findFirst().orElse(null);
            List<String> middlewares = new ArrayList<>();
            if (with != null)
                middlewares.addAll(Arrays.asList(with.value()));
            if (methodWith != null)
                middlewares.addAll(Arrays.asList(methodWith.value()));

            // Registering HTTP-Method annotations.
            //region Registering HTTP-Method Annotations
            for (Get a : getAnnotations(Get.class, method)) {
                bindMiddlewares(HTTPMethod.GET, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.GET, a.value()));
            }
            for (Post a : getAnnotations(Post.class, method)) {
                bindMiddlewares(HTTPMethod.POST, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.POST, a.value()));
            }
            for (Put a : getAnnotations(Put.class, method)) {
                bindMiddlewares(HTTPMethod.PUT, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.PUT, a.value()));
            }
            for (Delete a : getAnnotations(Delete.class, method)) {
                bindMiddlewares(HTTPMethod.DELETE, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.DELETE, a.value()));
            }
            for (Patch a : getAnnotations(Patch.class, method)) {
                bindMiddlewares(HTTPMethod.PATCH, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.PATCH, a.value()));
            }
            for (Trace a : getAnnotations(Trace.class, method)) {
                bindMiddlewares(HTTPMethod.TRACE, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.TRACE, a.value()));
            }
            for (Options a : getAnnotations(Options.class, method)) {
                bindMiddlewares(HTTPMethod.OPTIONS, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.OPTIONS, a.value()));
            }
            for (Head a : getAnnotations(Head.class, method)) {
                bindMiddlewares(HTTPMethod.HEAD, globalPrefix, prefixes, a.value(), middlewares);
                binds.add(new Bind(HTTPMethod.HEAD, a.value()));
            }
            for (WebSocketMessage a : getAnnotations(WebSocketMessage.class, method)) {
                WebSocketBindHandler handler = websocketHandlers.get(a.name());
                if (handler == null) {
                    bindMiddlewares(HTTPMethod.GET, globalPrefix, prefixes, a.value(), middlewares);
                    handler = new WebSocketBindHandler();
                    for (String prefix : prefixes)
                        server.webSocket(buildPattern(globalPrefix, prefix, a.value()), handler);
                    websocketHandlers.put(a.name(), handler);
                }
                handler.messageHandler = new BindMapper(server, controller, method);
            }
            for (WebSocketConnect a : getAnnotations(WebSocketConnect.class, method)) {
                WebSocketBindHandler handler = websocketHandlers.get(a.name());
                if (handler == null) {
                    bindMiddlewares(HTTPMethod.GET, globalPrefix, prefixes, a.value(), middlewares);
                    handler = new WebSocketBindHandler();
                    for (String prefix : prefixes)
                        server.webSocket(buildPattern(globalPrefix, prefix, a.value()), handler);
                    websocketHandlers.put(a.name(), handler);
                }
                handler.connectHandler = new BindMapper(server, controller, method);
            }
            for (WebSocketClose a : getAnnotations(WebSocketClose.class, method)) {
                WebSocketBindHandler handler = websocketHandlers.get(a.name());
                if (handler == null) {
                    bindMiddlewares(HTTPMethod.GET, globalPrefix, prefixes, a.value(), middlewares);
                    handler = new WebSocketBindHandler();
                    for (String prefix : prefixes)
                        server.webSocket(buildPattern(globalPrefix, prefix, a.value()), handler);
                    websocketHandlers.put(a.name(), handler);
                }
                handler.closeHandler = new BindMapper(server, controller, method);
            }
            //endregion

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

    private void bindMiddlewares(HTTPMethod method, String globalPrefix, List<String> prefixes, String path, List<String> middlewares) {
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

    private static List<Method> getMethodsRecursive(Class<?> type) {
        List<Method> methods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
        if (type.getSuperclass() != null && type.getSuperclass() != Object.class)
            methods.addAll(getMethodsRecursive(type.getSuperclass()));
        return methods.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static class BindMapper {

        private final HTTPServer server;
        private final Object controller;
        private final Method method;
        private final Object[] parameterAnnotations;
        private final Class<?>[] parameterTypes;
        private final String[] defaultValues;

        public BindMapper(HTTPServer server, Object controller, Method method) {
            this.server = server;
            this.controller = controller;
            this.method = method;
            method.setAccessible(true);
            parameterTypes = method.getParameterTypes();
            parameterAnnotations = new Object[parameterTypes.length];
            defaultValues = new String[parameterTypes.length];
            for (int i = 0; i < parameterAnnotations.length; i++) {
                DefaultValue defaultValue = getAnnotation(DefaultValue.class, method, i);
                if (defaultValue != null)
                    defaultValues[i] = defaultValue.value();

                for (Class<? extends Annotation> annotation : new Class[]{Attrib.class, Query.class, Body.class, Path.class, WSMessage.class, WSCode.class, WSReason.class}) {
                    Annotation annotation1 = getAnnotation(annotation, method, i);
                    if (annotation1 != null) {
                        parameterAnnotations[i] = annotation1;
                    }
                }

                if (parameterAnnotations[i] == null)
                    parameterAnnotations[i] = parameterTypes[i];
            }
        }

        public Object invoke(Exchange exchange, Map<String, Object> extraArgs) {
            Object[] args = new Object[parameterAnnotations.length];
            for (int i = 0; i < args.length; i++) {
                Object a = parameterAnnotations[i];
                if (a == null)
                    continue;


                if (a instanceof Body) {
                    args[i] = exchange.body(method.getParameterTypes()[i]);
                } else if (a instanceof Attrib) {
                    Attrib attrib = (Attrib) parameterAnnotations[i];
                    args[i] = exchange.attrib(attrib.value());
                } else if (a instanceof Query) {
                    Query query = (Query) parameterAnnotations[i];
                    args[i] = exchange.query(query.value(), (Class) parameterTypes[i], defaultValues[i]);
                } else if (a instanceof Path) {
                    Path path = (Path) parameterAnnotations[i];
                    args[i] = exchange.path(path.value().toLowerCase(Locale.ROOT));
                } else if (a instanceof WSMessage) {
                    args[i] = extraArgs.get("websocketMessage");
                } else if (a instanceof WSCode) {
                    args[i] = extraArgs.get("websocketCode");
                } else if (a instanceof WSReason) {
                    args[i] = extraArgs.get("websocketReason");
                } else {
                    for (RouteAutoInjector autoInjector : server.getRouteAutoInjectors()) {
                        args[i] = autoInjector.getValue(exchange, extraArgs, (Class<?>) parameterTypes[i]);
                        if (args[i] != null)
                            break;
                    }
                }
            }
            try {
                return method.invoke(controller, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }

    }

    private static class WebSocketBindHandler implements WebSocketHandler {
        BindMapper messageHandler;
        BindMapper connectHandler;
        BindMapper closeHandler;

        public void onConnect(WebSocket socket) {
            if (connectHandler != null)
                connectHandler.invoke(socket.getExchange(), new HashMap<String, Object>() {{
                    put("websocket", socket);
                }});
        }

        public void onMessage(WebSocket socket, String message) {
            if (messageHandler != null)
                messageHandler.invoke(socket.getExchange(), new HashMap<String, Object>() {{
                    put("websocket", socket);
                    put("websocketMessage", message);
                }});
        }

        public void onMessage(WebSocket socket, byte[] message) {
            if (messageHandler != null)
                messageHandler.invoke(socket.getExchange(), new HashMap<String, Object>() {{
                    put("websocket", socket);
                    put("websocketMessage", message);
                }});
        }

        public void onClose(WebSocket socket, Integer code, String reason) {
            if (closeHandler != null)
                closeHandler.invoke(socket.getExchange(), new HashMap<String, Object>() {{
                    put("websocket", socket);
                    put("websocketCode", code);
                    put("websocketReason", reason);
                }});
        }
    }

    private static class BindHandler implements RequestHandler {
        private final BindMapper handler;

        public BindHandler(HTTPServer server, Object controller, Method method) {
            handler = new BindMapper(server, controller, method);
        }

        public Object handle(Exchange exchange) {
            return handler.invoke(exchange, new HashMap<>());
        }
    }

}
