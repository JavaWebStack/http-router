package org.javawebstack.http.router.router;

import org.javawebstack.http.router.Exchange;
import org.javawebstack.http.router.HTTPMethod;
import org.javawebstack.http.router.websocket.WebSocket;

import java.util.Map;

public class DefaultRouteAutoInjector implements RouteAutoInjector {
    public static final DefaultRouteAutoInjector INSTANCE = new DefaultRouteAutoInjector();

    public Object getValue(Exchange exchange, Map<String, Object> extraArgs, Class<?> type) {
        if (Exchange.class.isAssignableFrom(type))
            return exchange;
        if (WebSocket.class.isAssignableFrom(type) && extraArgs.containsKey("websocket"))
            return extraArgs.get("websocket");
        if (type.equals(HTTPMethod.class))
            return exchange.getMethod();
        return null;
    }
}
