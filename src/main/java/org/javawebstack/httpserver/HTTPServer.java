package org.javawebstack.httpserver;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.javawebstack.graph.GraphMapper;
import org.javawebstack.httpserver.helper.JettyNoLog;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.router.RouteBinder;
import org.javawebstack.httpserver.transformer.route.DefaultRouteParamTransformer;
import org.javawebstack.httpserver.router.Route;
import org.javawebstack.httpserver.transformer.route.RouteParamTransformer;
import org.javawebstack.httpserver.transformer.route.RouteParamTransformerProvider;
import org.javawebstack.httpserver.transformer.response.ResponseTransformer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.javawebstack.httpserver.handler.*;
import org.javawebstack.httpserver.websocket.InternalWebSocketAdapter;
import org.javawebstack.httpserver.websocket.InternalWebSocketRequestHandler;
import org.javawebstack.injector.Injector;
import org.reflections.Reflections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private final List<RequestHandler> middleware = new ArrayList<>();
    private final List<AfterRequestHandler> after = new ArrayList<>();
    private Server server;
    private int port = 80;
    private final List<RequestInterceptor> beforeInterceptors = new ArrayList<>();
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd HH:mm:ss").disableHtmlEscaping().create();
    private GraphMapper graphMapper = new GraphMapper();
    private Injector injector = null;
    private org.eclipse.jetty.websocket.server.WebSocketHandler webSocketHandler;

    public HTTPServer(){
        routeParamTransformers.add(DefaultRouteParamTransformer.INSTANCE);
    }

    public HTTPServer logger(Logger logger){
        this.logger = logger;
        return this;
    }

    public HTTPServer graph(GraphMapper graphMapper){
        this.graphMapper = graphMapper;
        return this;
    }

    public GraphMapper getGraphMapper() {
        return graphMapper;
    }

    public Logger getLogger(){
        return logger;
    }

    public HTTPServer beforeInterceptor(RequestInterceptor handler){
        beforeInterceptors.add(handler);
        return this;
    }

    public HTTPServer get(String pattern, RequestHandler... handlers){
        return route(HttpMethod.GET, pattern, handlers);
    }

    public HTTPServer post(String pattern, RequestHandler... handlers){
        return route(HttpMethod.POST, pattern, handlers);
    }

    public HTTPServer put(String pattern, RequestHandler... handlers){
        return route(HttpMethod.PUT, pattern, handlers);
    }

    public HTTPServer delete(String pattern, RequestHandler... handlers){
        return route(HttpMethod.DELETE, pattern, handlers);
    }

    public HTTPServer webSocket(String pattern, WebSocketHandler handler){
        return route(HttpMethod.WEBSOCKET, pattern, new InternalWebSocketRequestHandler(handler));
    }

    public HTTPServer route(HttpMethod method, String pattern, RequestHandler... handlers){
        routes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public HTTPServer notFound(RequestHandler handler){
        notFoundHandler = handler;
        return this;
    }

    public HTTPServer middleware(RequestHandler handler){
        middleware.add(handler);
        return this;
    }

    public HTTPServer after(AfterRequestHandler handler){
        after.add(handler);
        return this;
    }

    public HTTPServer routeParamTransformer(RouteParamTransformer transformer){
        routeParamTransformers.add(transformer);
        return this;
    }

    public HTTPServer responseTransformer(ResponseTransformer transformer){
        responseTransformers.add(transformer);
        return this;
    }

    public HTTPServer exceptionHandler(ExceptionHandler handler){
        exceptionHandler = handler;
        return this;
    }

    public HTTPServer controller(Class<?> parentClass, Package p){
        return controller("", parentClass, p);
    }

    public HTTPServer controller(String globalPrefix, Class<?> parentClass, Package p){
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass).forEach(c -> {
            try {
                Object controller = c.newInstance();
                controller(globalPrefix, controller);
            } catch (InstantiationException | IllegalAccessException e) {}
        });
        return this;
    }

    public HTTPServer controller(Object controller){
        return controller("", controller);
    }

    public HTTPServer controller(String globalPrefix, Object controller){
        if(injector != null)
            injector.inject(controller);
        routeBinder.bind(globalPrefix, controller);
        return this;
    }

    public HTTPServer injector(Injector injector){
        this.injector = injector;
        return this;
    }

    public HTTPServer gson(Gson gson){
        this.gson = gson;
        return this;
    }

    public HTTPServer port(int port){
        this.port = port;
        return this;
    }

    public HTTPServer start(){
        Log.setLog(new JettyNoLog());
        server = new Server(port);
        server.setHandler(new HttpHandler());
        webSocketHandler = new org.eclipse.jetty.websocket.server.WebSocketHandler(){
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(InternalWebSocketAdapter.class);
            }
        };
        webSocketHandler.setServer(server);
        try {
            server.start();
            webSocketHandler.start();
            logger.info("HTTP-Server started on port "+port);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public void join() {
        try {
            server.join();
        }catch (InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }

    public void stop(){
        try {
            server.stop();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public void execute(Exchange exchange){
        try {
            for(RequestInterceptor ic : beforeInterceptors){
                if(ic.intercept(exchange)){
                    exchange.close();
                    return;
                }
            }
            for(Route route : routes){
                exchange.pathVariables = route.match(exchange);
                if(exchange.pathVariables == null)
                    continue;
                for(RequestHandler handler : middleware){
                    Object response = handler.handle(exchange);
                    if(response != null){
                        for(AfterRequestHandler afterHandler : after){
                            response = afterHandler.handleAfter(exchange, response);
                        }
                        exchange.write(transformResponse(response));
                        exchange.close();
                        return;
                    }
                }
                for(RequestHandler handler : route.getHandlers()){
                    Object response = handler.handle(exchange);
                    if(response != null){
                        for(AfterRequestHandler afterHandler : after){
                            response = afterHandler.handleAfter(exchange, response);
                        }
                        exchange.write(transformResponse(response));
                        exchange.close();
                        return;
                    }
                }
                if(exchange.getMethod() != HttpMethod.WEBSOCKET)
                    exchange.close();
                return;
            }
            exchange.write(transformResponse(notFoundHandler.handle(exchange)));
        }catch(Throwable ex){
            try {
                exchange.write(exceptionHandler.handleBytes(exchange, ex));
            }catch (Throwable ex2){
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

    public byte[] transformResponse(Object object){
        for(ResponseTransformer t : responseTransformers){
            byte[] res = t.transformBytes(object);
            if(res != null)
                return res;
        }
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }

    public Gson getGson() {
        return gson;
    }

    public Injector getInjector(){
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

}
