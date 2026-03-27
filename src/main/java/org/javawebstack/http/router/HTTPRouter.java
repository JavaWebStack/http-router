package org.javawebstack.http.router;

import org.javawebstack.abstractdata.mapper.Mapper;
import org.javawebstack.abstractdata.mapper.naming.NamingPolicy;
import org.javawebstack.commons.inject.Injector;
import org.javawebstack.http.router.adapter.IHTTPSocketServer;
import org.javawebstack.http.router.handler.*;
import org.javawebstack.http.router.multipart.content.PartContentCache;
import org.javawebstack.http.router.router.DefaultRouteAutoInjector;
import org.javawebstack.http.router.router.Route;
import org.javawebstack.http.router.router.RouteAutoInjector;
import org.javawebstack.http.router.router.RouteBinder;
import org.javawebstack.http.router.transformer.response.ResponseTransformer;
import org.javawebstack.http.router.transformer.route.DefaultRouteParamTransformer;
import org.javawebstack.http.router.transformer.route.RouteParamTransformer;
import org.javawebstack.http.router.transformer.route.RouteParamTransformerProvider;
import org.javawebstack.http.router.util.DirectoryFileProvider;
import org.javawebstack.http.router.util.ResourceFileProvider;
import org.javawebstack.http.router.websocket.InternalWebSocketRequestHandler;
import org.reflections.Reflections;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HTTPRouter implements RouteParamTransformerProvider {

    private Logger logger = Logger.getLogger("HTTP-Router");
    private final List<Route> routes = new ArrayList<>();
    private final RouteBinder routeBinder = new RouteBinder(this);
    private final List<RouteParamTransformer> routeParamTransformers = new ArrayList<>();
    private final List<ResponseTransformer> responseTransformers = new ArrayList<>();
    private RequestHandler notFoundHandler = new DefaultNotFoundHandler();
    private ExceptionHandler exceptionHandler = new ExceptionHandler.DefaultExceptionHandler();
    private final List<Route> beforeRoutes = new ArrayList<>();
    private final List<Route> afterRoutes = new ArrayList<>();
    private final IHTTPSocketServer server;
    private final List<RequestInterceptor> beforeInterceptors = new ArrayList<>();
    private Mapper mapper = new Mapper().namingPolicy(NamingPolicy.SNAKE_CASE);
    private final List<RouteAutoInjector> routeAutoInjectors = new ArrayList<>();
    private final Map<String, RequestHandler> beforeMiddleware = new HashMap<>();
    private final Map<String, AfterRequestHandler> afterMiddleware = new HashMap<>();
    private Injector injector;
    private final HTTPRoutingOptions routingOptions = new HTTPRoutingOptions();
    private PartContentCache multipartContentCache;

    public HTTPRouter(IHTTPSocketServer server) {
        this.server = server;
        routeParamTransformers.add(DefaultRouteParamTransformer.INSTANCE);
        routeAutoInjectors.add(DefaultRouteAutoInjector.INSTANCE);
    }

    public HTTPRouter maxThreads(int maxThreads) {
        this.server.setMaxThreads(maxThreads);
        return this;
    }

    public HTTPRouter logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public HTTPRouter mapper(Mapper mapper) {
        this.mapper = mapper;
        return this;
    }

    public Mapper getMapper() {
        return mapper;
    }

    public Logger getLogger() {
        return logger;
    }

    public HTTPRouter beforeInterceptor(RequestInterceptor handler) {
        if(injector != null)
            injector.inject(handler);
        beforeInterceptors.add(handler);
        return this;
    }

    public HTTPRouter routeAutoInjector(RouteAutoInjector injector) {
        if(this.injector != null)
            this.injector.inject(injector);
        routeAutoInjectors.add(injector);
        return this;
    }

    public HTTPRouter get(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPRouter beforeGet(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPRouter afterGet(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPRouter post(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPRouter beforePost(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPRouter afterPost(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPRouter put(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPRouter beforePut(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPRouter afterPut(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPRouter delete(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPRouter staticDirectory(String pathPrefix, File directory) {
        return staticHandler(pathPrefix, new StaticFileHandler().add(new DirectoryFileProvider(directory)));
    }

    public HTTPRouter staticDirectory(String pathPrefix, String directory) {
        return staticDirectory(pathPrefix, new File(directory));
    }

    public HTTPRouter staticResourceDirectory(String pathPrefix, String prefix) {
        return staticResourceDirectory(pathPrefix, null, prefix);
    }

    public HTTPRouter staticResourceDirectory(String pathPrefix, ClassLoader classLoader, String prefix) {
        return staticHandler(pathPrefix, new StaticFileHandler().add(new ResourceFileProvider(classLoader, prefix)));
    }

    public HTTPRouter staticHandler(String pathPrefix, StaticFileHandler handler) {
        if(injector != null)
            injector.inject(handler);
        return get(pathPrefix + (pathPrefix.endsWith("/") ? "" : "/") + "{*:path}", handler);
    }

    public HTTPRouter beforeDelete(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPRouter afterDelete(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPRouter route(HTTPMethod method, String pattern, RequestHandler... handlers) {
        return route(null, method, pattern, handlers);
    }

    public HTTPRouter route(String name, HTTPMethod method, String pattern, RequestHandler... handlers) {
        if(injector != null) {
            for(RequestHandler handler : handlers) {
                injector.inject(handler);
            }
        }
        routes.add(new Route(this, method, pattern, routingOptions, Arrays.asList(handlers)).setName(name));
        return this;
    }

    public HTTPRouter beforeRoute(HTTPMethod method, String pattern, RequestHandler... handlers) {
        if(injector != null) {
            for(RequestHandler handler : handlers) {
                injector.inject(handler);
            }
        }
        beforeRoutes.add(new Route(this, method, pattern, routingOptions, Arrays.asList(handlers)));
        return this;
    }

    public HTTPRouter afterRoute(HTTPMethod method, String pattern, AfterRequestHandler... handlers) {
        if(injector != null) {
            for(AfterRequestHandler handler : handlers) {
                injector.inject(handler);
            }
        }
        afterRoutes.add(new Route(this, method, pattern, routingOptions, null).setAfterHandlers(Arrays.asList(handlers)));
        return this;
    }

    public HTTPRouter route(HTTPMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HTTPMethod method : methods)
            route(method, pattern, handlers);
        return this;
    }

    public HTTPRouter beforeRoute(HTTPMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HTTPMethod method : methods)
            beforeRoute(method, pattern, handlers);
        return this;
    }

    public HTTPRouter afterRoute(HTTPMethod[] methods, String pattern, AfterRequestHandler... handlers) {
        for (HTTPMethod method : methods)
            afterRoute(method, pattern, handlers);
        return this;
    }

    public List<Route> getBeforeRoutes() {
        return beforeRoutes;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Route> getAfterRoutes() {
        return afterRoutes;
    }

    public HTTPRouter any(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPRouter beforeAny(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPRouter afterAny(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPRouter webSocket(String pattern, WebSocketHandler handler) {
        if(injector != null)
            injector.inject(handler);
        if(!server.isWebSocketSupported())
            throw new UnsupportedOperationException(server.getClass().getName() + " does not support websockets!");
        return route(HTTPMethod.WEBSOCKET, pattern, new InternalWebSocketRequestHandler(handler));
    }

    public HTTPRouter middleware(String name, RequestHandler handler) {
        if(injector != null)
            injector.inject(handler);
        beforeMiddleware.put(name, handler);
        return this;
    }

    public HTTPRouter middleware(String name, AfterRequestHandler handler) {
        if(injector != null)
            injector.inject(handler);
        afterMiddleware.put(name, handler);
        return this;
    }

    public HTTPRouter notFound(RequestHandler handler) {
        if(injector != null)
            injector.inject(handler);
        notFoundHandler = handler;
        return this;
    }

    public HTTPRouter routeParamTransformer(RouteParamTransformer transformer) {
        if(injector != null)
            injector.inject(transformer);
        routeParamTransformers.add(transformer);
        return this;
    }

    public HTTPRouter responseTransformer(ResponseTransformer transformer) {
        if(injector != null)
            injector.inject(transformer);
        responseTransformers.add(transformer);
        return this;
    }

    public HTTPRouter exceptionHandler(ExceptionHandler handler) {
        if(injector != null)
            injector.inject(handler);
        exceptionHandler = handler;
        return this;
    }

    public HTTPRouter injector(Injector injector) {
        this.injector = injector;
        return this;
    }

    public Injector getInjector() {
        return injector;
    }

    public HTTPRouter controller(Class<?> parentClass, Package p) {
        return controller("", parentClass, p);
    }

    public HTTPRouter controller(String globalPrefix, Class<?> parentClass, Package p) {
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass)
                .stream()
                .map(t -> {
                    if(injector == null) {
                        try {
                            return t.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return injector.getInstance(t);
                    }
                })
                .forEach(c -> controller(globalPrefix, c));
        return this;
    }

    public HTTPRouter controller(Object controller) {
        return controller("", controller);
    }

    public HTTPRouter controller(String globalPrefix, Object controller) {
        if(injector != null)
            injector.inject(controller);
        routeBinder.bind(globalPrefix, controller);
        return this;
    }

    public HTTPRouter port(int port) {
        server.setPort(port);
        return this;
    }

    public HTTPRouter start() {
        server.setHandler(socket -> execute(new Exchange(this, socket)));
        try {
            server.start();
            logger.info("HTTP-Server started on port " + server.getPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    public void join() {
        server.join();
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void execute(Exchange exchange) {
        Exchange.exchanges.set(exchange);
        try {
            try {
                if(multipartContentCache != null)
                    exchange.enableMultipart(multipartContentCache);
                Object response = null;
                try {
                    for (RequestInterceptor ic : beforeInterceptors) {
                        if (ic.intercept(exchange)) {
                            exchange.close();
                            Exchange.exchanges.remove();
                            return;
                        }
                    }
                    middlewares:
                    for (Route route : beforeRoutes) {
                        Map<String, Object> pathVariables = route.match(exchange);
                        if (pathVariables == null)
                            continue;
                        exchange.getPathVariables().putAll(pathVariables);
                        for (RequestHandler handler : route.getHandlers()) {
                            try {
                                response = handler.handle(exchange);
                            } catch (Throwable ex) {
                                response = exceptionHandler.handle(exchange, ex);
                            }
                            if (response != null)
                                break middlewares;
                        }
                    }
                    exchange.getPathVariables().clear();
                    if (response == null) {
                        routes:
                        for (Route route : routes) {
                            Map<String, Object> pathVariables = route.match(exchange);
                            if (pathVariables == null)
                                continue;
                            exchange.getPathVariables().putAll(pathVariables);
                            for (RequestHandler handler : route.getHandlers()) {
                                exchange.setFinalHandler(handler);
                                response = handler.handle(exchange);
                                if (exchange.getMethod() == HTTPMethod.WEBSOCKET) {
                                    Exchange.exchanges.remove();
                                    return;
                                }
                                if (response != null)
                                    break routes;
                            }
                            exchange.getPathVariables().clear();
                        }
                    }
                } catch (Throwable ex) {
                    response = exceptionHandler.handle(exchange, ex);
                }
                if (response == null)
                    response = notFoundHandler.handle(exchange);
                exchange.getPathVariables().clear();
                for (Route route : afterRoutes) {
                    Map<String, Object> pathVariables = route.match(exchange);
                    if (pathVariables == null)
                        continue;
                    exchange.getPathVariables().putAll(pathVariables);
                    for (AfterRequestHandler handler : route.getAfterHandlers())
                        response = handler.handleAfter(exchange, response);
                    exchange.getPathVariables().clear();
                }
                if (response != null)
                    exchange.write(transformResponse(exchange, response));
                if (exchange.getMethod() != HTTPMethod.WEBSOCKET)
                    exchange.close();
                Exchange.exchanges.remove();
                return;
            } catch (Throwable ex) {
                try {
                    exchange.write(transformResponse(exchange, exceptionHandler.handle(exchange, ex)));
                } catch (Throwable ex2) {
                    exchange.status(500);
                    logger.log(Level.SEVERE, ex2, () -> "An error occured in the exception handler!");
                }
            }
        } catch (Exception ex) {
            // This should never be reached, just added this as a precaution
            logger.log(Level.SEVERE, ex, () -> "An unexpected error occured in the exception handling of the exception handler (probably while setting the status)");
        }
        Exchange.exchanges.remove();
        exchange.close();
    }

    public List<RouteParamTransformer> getRouteParamTransformer() {
        return routeParamTransformers;
    }

    public RouteParamTransformer getRouteParamTransformer(String type) {
        return routeParamTransformers.stream().filter(t -> t.canTransform(type)).findFirst().orElse(null);
    }
    public RequestHandler getBeforeMiddleware(String name) {
        return beforeMiddleware.get(name);
    }

    public AfterRequestHandler getAfterMiddleware(String name) {
        return afterMiddleware.get(name);
    }

    public List<RouteAutoInjector> getRouteAutoInjectors() {
        return routeAutoInjectors;
    }

    public byte[] transformResponse(Exchange exchange, Object object) {
        for (ResponseTransformer t : responseTransformers) {
            byte[] res = t.transformBytes(exchange, object);
            if (res != null)
                return res;
        }
        if(object instanceof byte[])
            return (byte[]) object;
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public HTTPRouter enableMultipart(PartContentCache cache) {
        this.multipartContentCache = cache;
        return this;
    }

    public HTTPRouter caseInsensitiveRouting() {
        return caseInsensitiveRouting(true);
    }

    public HTTPRouter caseInsensitiveRouting(boolean caseInsensitiveRouting) {
        routingOptions.caseInsensitive(caseInsensitiveRouting);
        return this;
    }

    public HTTPRoutingOptions getRoutingOptions() {
        return routingOptions;
    }

    @Deprecated
    public HTTPRouter disableFormMethods() {
        return formMethodParameter(null);
    }

    public HTTPRouter formMethodParameter(String parameter) {
        routingOptions.formMethodParameter(parameter);
        return this;
    }

}
