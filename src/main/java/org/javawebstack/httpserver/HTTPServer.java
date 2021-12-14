package org.javawebstack.httpserver;

import org.javawebstack.abstractdata.AbstractMapper;
import org.javawebstack.abstractdata.NamingPolicy;
import org.javawebstack.httpserver.adapter.IHTTPSocketServer;
import org.javawebstack.httpserver.adapter.jetty.JettyHTTPSocketServer;
import org.javawebstack.httpserver.handler.*;
import org.javawebstack.httpserver.router.DefaultRouteAutoInjector;
import org.javawebstack.httpserver.router.Route;
import org.javawebstack.httpserver.router.RouteAutoInjector;
import org.javawebstack.httpserver.router.RouteBinder;
import org.javawebstack.httpserver.transformer.response.ResponseTransformer;
import org.javawebstack.httpserver.transformer.route.DefaultRouteParamTransformer;
import org.javawebstack.httpserver.transformer.route.RouteParamTransformer;
import org.javawebstack.httpserver.transformer.route.RouteParamTransformerProvider;
import org.javawebstack.httpserver.util.DirectoryFileProvider;
import org.javawebstack.httpserver.util.ResourceFileProvider;
import org.javawebstack.httpserver.websocket.InternalWebSocketRequestHandler;
import org.reflections.Reflections;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HTTPServer implements RouteParamTransformerProvider {

    private Logger logger = Logger.getLogger("HTTP-Server");
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
    private AbstractMapper abstractMapper = new AbstractMapper().setNamingPolicy(NamingPolicy.SNAKE_CASE);
    private final List<RouteAutoInjector> routeAutoInjectors = new ArrayList<>();
    private final Map<String, RequestHandler> beforeMiddleware = new HashMap<>();
    private final Map<String, AfterRequestHandler> afterMiddleware = new HashMap<>();
    private Function<Class<?>, Object> controllerInitiator = this::defaultControllerInitiator;

    public HTTPServer() {
        this(new JettyHTTPSocketServer());
    }

    public HTTPServer(IHTTPSocketServer server) {
        this.server = server;
        routeParamTransformers.add(DefaultRouteParamTransformer.INSTANCE);
        routeAutoInjectors.add(DefaultRouteAutoInjector.INSTANCE);
    }

    public HTTPServer logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public HTTPServer abstractMapper(AbstractMapper mapper) {
        this.abstractMapper = mapper;
        return this;
    }

    public AbstractMapper getAbstractMapper() {
        return abstractMapper;
    }

    public Logger getLogger() {
        return logger;
    }

    public HTTPServer beforeInterceptor(RequestInterceptor handler) {
        beforeInterceptors.add(handler);
        return this;
    }

    public HTTPServer routeAutoInjector(RouteAutoInjector injector) {
        routeAutoInjectors.add(injector);
        return this;
    }

    public HTTPServer get(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPServer beforeGet(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPServer afterGet(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.GET, pattern, handlers);
    }

    public HTTPServer post(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPServer beforePost(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPServer afterPost(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.POST, pattern, handlers);
    }

    public HTTPServer put(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPServer beforePut(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPServer afterPut(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.PUT, pattern, handlers);
    }

    public HTTPServer delete(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPServer staticDirectory(String pathPrefix, File directory) {
        return staticHandler(pathPrefix, new StaticFileHandler().add(new DirectoryFileProvider(directory)));
    }

    public HTTPServer staticDirectory(String pathPrefix, String directory) {
        return staticDirectory(pathPrefix, new File(directory));
    }

    public HTTPServer staticResourceDirectory(String pathPrefix, String prefix) {
        return staticResourceDirectory(pathPrefix, null, prefix);
    }

    public HTTPServer staticResourceDirectory(String pathPrefix, ClassLoader classLoader, String prefix) {
        return staticHandler(pathPrefix, new StaticFileHandler().add(new ResourceFileProvider(classLoader, prefix)));
    }

    public HTTPServer staticHandler(String pathPrefix, StaticFileHandler handler) {
        return get(pathPrefix + (pathPrefix.endsWith("/") ? "" : "/") + "{*:path}", handler);
    }

    public HTTPServer beforeDelete(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPServer afterDelete(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.DELETE, pattern, handlers);
    }

    public HTTPServer route(HTTPMethod method, String pattern, RequestHandler... handlers) {
        routes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer beforeRoute(HTTPMethod method, String pattern, RequestHandler... handlers) {
        beforeRoutes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer afterRoute(HTTPMethod method, String pattern, AfterRequestHandler... handlers) {
        afterRoutes.add(new Route(this, method, pattern, null).setAfterHandlers(Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer route(HTTPMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HTTPMethod method : methods)
            route(method, pattern, handlers);
        return this;
    }

    public HTTPServer beforeRoute(HTTPMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HTTPMethod method : methods)
            beforeRoute(method, pattern, handlers);
        return this;
    }

    public HTTPServer afterRoute(HTTPMethod[] methods, String pattern, AfterRequestHandler... handlers) {
        for (HTTPMethod method : methods)
            afterRoute(method, pattern, handlers);
        return this;
    }

    public HTTPServer any(String pattern, RequestHandler... handlers) {
        return route(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPServer beforeAny(String pattern, RequestHandler... handlers) {
        return beforeRoute(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPServer afterAny(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HTTPMethod.values(), pattern, handlers);
    }

    public HTTPServer webSocket(String pattern, WebSocketHandler handler) {
        if(!server.isWebSocketSupported())
            throw new UnsupportedOperationException(server.getClass().getName() + " does not support websockets!");
        return route(HTTPMethod.WEBSOCKET, pattern, new InternalWebSocketRequestHandler(handler));
    }

    public HTTPServer middleware(String name, RequestHandler handler) {
        beforeMiddleware.put(name, handler);
        return this;
    }

    public HTTPServer middleware(String name, AfterRequestHandler handler) {
        afterMiddleware.put(name, handler);
        return this;
    }

    public HTTPServer notFound(RequestHandler handler) {
        notFoundHandler = handler;
        return this;
    }

    public HTTPServer routeParamTransformer(RouteParamTransformer transformer) {
        routeParamTransformers.add(transformer);
        return this;
    }

    public HTTPServer responseTransformer(ResponseTransformer transformer) {
        responseTransformers.add(transformer);
        return this;
    }

    public HTTPServer exceptionHandler(ExceptionHandler handler) {
        exceptionHandler = handler;
        return this;
    }

    private Object defaultControllerInitiator (Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public HTTPServer controllerInitiator (Function<Class<?>, Object> initiator) {
        controllerInitiator = initiator;
        return this;
    }

    public HTTPServer controller(Class<?> parentClass, Package p) {
        return controller("", parentClass, p);
    }

    public HTTPServer controller(String globalPrefix, Class<?> parentClass, Package p) {
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass)
                .stream()
                .map(controllerInitiator)
                .forEach(c -> controller(globalPrefix, c));
        return this;
    }

    public HTTPServer controller(Object controller) {
        return controller("", controller);
    }

    public HTTPServer controller(String globalPrefix, Object controller) {
        routeBinder.bind(globalPrefix, controller);
        return this;
    }

    public HTTPServer port(int port) {
        server.setPort(port);
        return this;
    }

    public HTTPServer start() {
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
                logger.log(Level.SEVERE, ex2, () -> "An error occured in the exception handler!");
            }
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

}
