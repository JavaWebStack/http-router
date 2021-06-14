package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.websocket.WebSocket;

import java.util.Map;

public class DefaultRouteAutoInjector implements RouteAutoInjector {
    public static final DefaultRouteAutoInjector INSTANCE = new DefaultRouteAutoInjector();

    public Object getValue(Exchange exchange, Map<String, Object> extraArgs, Class<?> type) {
        if (Exchange.class.isAssignableFrom(type))
            return exchange;
        if(WebSocket.class.isAssignableFrom(type) && extraArgs.containsKey("websocket"))
            return extraArgs.get("websocket");
        if (type.equals(HttpMethod.class))
            return exchange.getMethod();
        return null;
    }
}
