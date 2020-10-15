package eu.bebendorf.ajwf;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.bebendorf.ajwf.inject.Injector;
import eu.bebendorf.ajwf.handler.*;
import eu.bebendorf.ajwf.helper.HttpMethod;
import eu.bebendorf.ajwf.router.RouteBinder;
import eu.bebendorf.ajwf.transformer.route.DefaultRouteParamTransformer;
import eu.bebendorf.ajwf.router.Route;
import eu.bebendorf.ajwf.transformer.route.RouteParamTransformer;
import eu.bebendorf.ajwf.transformer.route.RouteParamTransformerProvider;
import eu.bebendorf.ajwf.transformer.response.ResponseTransformer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.reflections.Reflections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebService implements RouteParamTransformerProvider {

    private List<Route> routes = new ArrayList<>();
    private List<RouteParamTransformer> routeParamTransformers = new ArrayList<>();
    private List<ResponseTransformer> responseTransformers = new ArrayList<>();
    private RequestHandler notFoundHandler = new DefaultNotFoundHandler();
    private ExceptionHandler exceptionHandler = new ExceptionHandler.DefaultExceptionHandler();
    private List<RequestHandler> middleware = new ArrayList<>();
    private List<AfterRequestHandler> after = new ArrayList<>();
    private Server server;
    private int port = 80;
    private List<RequestInterceptor> beforeInterceptors = new ArrayList<>();
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private Injector injector = null;

    public WebService(){
        routeParamTransformers.add(DefaultRouteParamTransformer.INSTANCE);
    }

    public WebService beforeInterceptor(RequestInterceptor handler){
        beforeInterceptors.add(handler);
        return this;
    }

    public WebService get(String pattern, RequestHandler... handlers){
        return route(HttpMethod.GET, pattern, handlers);
    }

    public WebService post(String pattern, RequestHandler... handlers){
        return route(HttpMethod.POST, pattern, handlers);
    }

    public WebService put(String pattern, RequestHandler... handlers){
        return route(HttpMethod.PUT, pattern, handlers);
    }

    public WebService delete(String pattern, RequestHandler... handlers){
        return route(HttpMethod.DELETE, pattern, handlers);
    }

    public WebService route(HttpMethod method, String pattern, RequestHandler... handlers){
        routes.add(new Route(this, method, pattern, Arrays.asList(handlers)));
        return this;
    }

    public WebService notFound(RequestHandler handler){
        notFoundHandler = handler;
        return this;
    }

    public WebService middleware(RequestHandler handler){
        middleware.add(handler);
        return this;
    }

    public WebService after(AfterRequestHandler handler){
        after.add(handler);
        return this;
    }

    public WebService routeParamTransformer(RouteParamTransformer transformer){
        routeParamTransformers.add(transformer);
        return this;
    }

    public WebService responseTransformer(ResponseTransformer transformer){
        responseTransformers.add(transformer);
        return this;
    }

    public WebService exceptionHandler(ExceptionHandler handler){
        exceptionHandler = handler;
        return this;
    }

    public WebService controller(Class<?> parentClass, Package p){
        return controller("", parentClass, p);
    }

    public WebService controller(String globalPrefix, Class<?> parentClass, Package p){
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass).forEach(c -> {
            try {
                Object controller = c.newInstance();
                controller(globalPrefix, controller);
            } catch (InstantiationException | IllegalAccessException e) {}
        });
        return this;
    }

    public WebService controller(Object controller){
        return controller("", controller);
    }

    public WebService controller(String globalPrefix, Object controller){
        if(injector != null)
            injector.inject(controller);
        RouteBinder.bind(this, globalPrefix, controller);
        return this;
    }

    public WebService injector(Injector injector){
        this.injector = injector;
        return this;
    }

    public WebService gson(Gson gson){
        this.gson = gson;
        return this;
    }

    public WebService port(int port){
        this.port = port;
        return this;
    }

    public WebService start(){
        server = new Server(port);
        server.setHandler(new HttpHandler());
        try {
            server.start();
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
                exchange.pathVariables = route.match(exchange.getMethod(), exchange.getPath());
                if(exchange.pathVariables == null)
                    continue;
                for(RequestHandler handler : middleware){
                    Object response = handler.handle(exchange);
                    if(response != null){
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
                exchange.close();
                return;
            }
            exchange.write(transformResponse(notFoundHandler.handle(exchange)));
        }catch(Throwable ex){
            exchange.write(exceptionHandler.handleBytes(exchange, ex));
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
            execute(new Exchange(WebService.this, httpServletRequest, httpServletResponse));
        }
    }

}
