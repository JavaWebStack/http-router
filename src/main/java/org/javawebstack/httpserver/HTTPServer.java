package org.javawebstack.httpserver;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.javawebstack.abstractdata.AbstractMapper;
import org.javawebstack.httpserver.handler.*;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.helper.JettyNoLog;
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
import org.javawebstack.httpserver.websocket.InternalWebSocketAdapter;
import org.javawebstack.httpserver.websocket.InternalWebSocketRequestHandler;
import org.javawebstack.injector.Injector;
import org.reflections.Reflections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private Server server;
    private int port = 80;
    private final List<RequestInterceptor> beforeInterceptors = new ArrayList<>();
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd HH:mm:ss").disableHtmlEscaping().create();
    private AbstractMapper abstractMapper = new AbstractMapper();
    private Injector injector = null;
    private org.eclipse.jetty.websocket.server.WebSocketHandler webSocketHandler;
    private List<RouteAutoInjector> routeAutoInjectors = new ArrayList<>();
    private final Map<String, RequestHandler> beforeMiddleware = new HashMap<>();
    private final Map<String, AfterRequestHandler> afterMiddleware = new HashMap<>();

    public HTTPServer() {
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
        if (injector != null)
            injector.inject(handler);
        beforeInterceptors.add(handler);
        return this;
    }

    public HTTPServer routeAutoInjector(RouteAutoInjector injector) {
        if (this.injector != null)
            this.injector.inject(injector);
        routeAutoInjectors.add(injector);
        return this;
    }

    public HTTPServer get(String pattern, RequestHandler... handlers) {
        return route(HttpMethod.GET, pattern, handlers);
    }

    public HTTPServer beforeGet(String pattern, RequestHandler... handlers) {
        return beforeRoute(HttpMethod.GET, pattern, handlers);
    }

    public HTTPServer afterGet(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HttpMethod.GET, pattern, handlers);
    }

    public HTTPServer post(String pattern, RequestHandler... handlers) {
        return route(HttpMethod.POST, pattern, handlers);
    }

    public HTTPServer beforePost(String pattern, RequestHandler... handlers) {
        return beforeRoute(HttpMethod.POST, pattern, handlers);
    }

    public HTTPServer afterPost(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HttpMethod.POST, pattern, handlers);
    }

    public HTTPServer put(String pattern, RequestHandler... handlers) {
        return route(HttpMethod.PUT, pattern, handlers);
    }

    public HTTPServer beforePut(String pattern, RequestHandler... handlers) {
        return beforeRoute(HttpMethod.PUT, pattern, handlers);
    }

    public HTTPServer afterPut(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HttpMethod.PUT, pattern, handlers);
    }

    public HTTPServer delete(String pattern, RequestHandler... handlers) {
        return route(HttpMethod.DELETE, pattern, handlers);
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
        if (injector != null)
            injector.inject(handler);
        return get(pathPrefix + (pathPrefix.endsWith("/") ? "" : "/") + "{*:path}", handler);
    }

    public HTTPServer beforeDelete(String pattern, RequestHandler... handlers) {
        return beforeRoute(HttpMethod.DELETE, pattern, handlers);
    }

    public HTTPServer afterDelete(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HttpMethod.DELETE, pattern, handlers);
    }

    public HTTPServer route(HttpMethod method, String pattern, RequestHandler... handlers) {
        if (injector != null) {
            for (RequestHandler handler : handlers)
                injector.inject(handler);
        }
        routes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer beforeRoute(HttpMethod method, String pattern, RequestHandler... handlers) {
        if (injector != null) {
            for (RequestHandler handler : handlers)
                injector.inject(handler);
        }
        beforeRoutes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer afterRoute(HttpMethod method, String pattern, AfterRequestHandler... handlers) {
        if (injector != null) {
            for (AfterRequestHandler handler : handlers)
                injector.inject(handler);
        }
        afterRoutes.add(new Route(this, method, pattern, null).setAfterHandlers(Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer route(HttpMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HttpMethod method : methods)
            route(method, pattern, handlers);
        return this;
    }

    public HTTPServer beforeRoute(HttpMethod[] methods, String pattern, RequestHandler... handlers) {
        for (HttpMethod method : methods)
            beforeRoute(method, pattern, handlers);
        return this;
    }

    public HTTPServer afterRoute(HttpMethod[] methods, String pattern, AfterRequestHandler... handlers) {
        for (HttpMethod method : methods)
            afterRoute(method, pattern, handlers);
        return this;
    }

    public HTTPServer any(String pattern, RequestHandler... handlers) {
        return route(HttpMethod.values(), pattern, handlers);
    }

    public HTTPServer beforeAny(String pattern, RequestHandler... handlers) {
        return beforeRoute(HttpMethod.values(), pattern, handlers);
    }

    public HTTPServer afterAny(String pattern, AfterRequestHandler... handlers) {
        return afterRoute(HttpMethod.values(), pattern, handlers);
    }

    public HTTPServer webSocket(String pattern, WebSocketHandler handler) {
        if (injector != null)
            injector.inject(handler);
        return route(HttpMethod.WEBSOCKET, pattern, new InternalWebSocketRequestHandler(handler));
    }

    public HTTPServer middleware(String name, RequestHandler handler) {
        if (injector != null)
            injector.inject(handler);
        beforeMiddleware.put(name, handler);
        return this;
    }

    public HTTPServer middleware(String name, AfterRequestHandler handler) {
        if (injector != null)
            injector.inject(handler);
        afterMiddleware.put(name, handler);
        return this;
    }

    public HTTPServer notFound(RequestHandler handler) {
        if (injector != null)
            injector.inject(handler);
        notFoundHandler = handler;
        return this;
    }

    public HTTPServer routeParamTransformer(RouteParamTransformer transformer) {
        if (injector != null)
            injector.inject(transformer);
        routeParamTransformers.add(transformer);
        return this;
    }

    public HTTPServer responseTransformer(ResponseTransformer transformer) {
        if (injector != null)
            injector.inject(transformer);
        responseTransformers.add(transformer);
        return this;
    }

    public HTTPServer exceptionHandler(ExceptionHandler handler) {
        if (injector != null)
            injector.inject(handler);
        exceptionHandler = handler;
        return this;
    }

    public HTTPServer controller(Class<?> parentClass, Package p) {
        return controller("", parentClass, p);
    }

    public HTTPServer controller(String globalPrefix, Class<?> parentClass, Package p) {
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass).forEach(c -> {
            try {
                Object controller = c.newInstance();
                controller(globalPrefix, controller);
            } catch (InstantiationException | IllegalAccessException e) {
            }
        });
        return this;
    }

    public HTTPServer controller(Object controller) {
        return controller("", controller);
    }

    public HTTPServer controller(String globalPrefix, Object controller) {
        if (injector != null)
            injector.inject(controller);
        routeBinder.bind(globalPrefix, controller);
        return this;
    }

    public HTTPServer injector(Injector injector) {
        this.injector = injector;
        return this;
    }

    public HTTPServer gson(Gson gson) {
        this.gson = gson;
        return this;
    }

    public HTTPServer port(int port) {
        this.port = port;
        return this;
    }

    public HTTPServer start() {
        Log.setLog(new JettyNoLog());
        server = new Server(port);
        server.setHandler(new HttpHandler());
        webSocketHandler = new org.eclipse.jetty.websocket.server.WebSocketHandler() {
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(InternalWebSocketAdapter.class);
            }
        };
        webSocketHandler.setServer(server);
        try {
            server.start();
            webSocketHandler.start();
            logger.info("HTTP-Server started on port " + port);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void execute(Exchange exchange) {
        try {
            Object response = null;
            try {
                for (RequestInterceptor ic : beforeInterceptors) {
                    if (ic.intercept(exchange)) {
                        exchange.close();
                        return;
                    }
                }
                for (Route route : beforeRoutes) {
                    exchange.pathVariables = route.match(exchange);
                    if (exchange.pathVariables == null)
                        continue;
                    for (RequestHandler handler : route.getHandlers()) {
                        try {
                            response = handler.handle(exchange);
                        } catch (Throwable ex) {
                            response = exceptionHandler.handle(exchange, ex);
                        }
                    }
                    exchange.pathVariables = null;
                }
                exchange.pathVariables = null;
                if (response == null) {
                    routes:
                    for (Route route : routes) {
                        exchange.pathVariables = route.match(exchange);
                        if (exchange.pathVariables == null)
                            continue;
                        for (RequestHandler handler : route.getHandlers()) {
                            response = handler.handle(exchange);
                            if (exchange.getMethod() == HttpMethod.WEBSOCKET)
                                return;
                            if (response != null)
                                break routes;
                        }
                        exchange.pathVariables = null;
                    }
                }
            } catch (Throwable ex) {
                response = exceptionHandler.handle(exchange, ex);
            }
            if (response == null)
                response = notFoundHandler.handle(exchange);
            exchange.pathVariables = null;
            for (Route route : afterRoutes) {
                exchange.pathVariables = route.match(exchange);
                if (exchange.pathVariables == null)
                    continue;
                for (AfterRequestHandler handler : route.getAfterHandlers())
                    response = handler.handleAfter(exchange, response);
                exchange.pathVariables = null;
            }
            if (response != null)
                exchange.write(transformResponse(exchange, response));
            if (exchange.getMethod() != HttpMethod.WEBSOCKET)
                exchange.close();
            return;
        } catch (Throwable ex) {
            try {
                exchange.write(transformResponse(exchange, exceptionHandler.handle(exchange, ex)));
            } catch (Throwable ex2) {
                logger.log(Level.SEVERE, ex2, () -> "An error occured in the exception handler!");
            }
        }
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
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }

    public Gson getGson() {
        return gson;
    }

    public Injector getInjector() {
        return injector;
    }

    private class HttpHandler extends AbstractHandler {
        public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            execute(new Exchange(HTTPServer.this, httpServletRequest, httpServletResponse));
        }
    }

    public org.eclipse.jetty.websocket.server.WebSocketHandler getInternalWebSocketHandler() {
        return webSocketHandler;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }
}
