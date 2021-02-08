package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.helper.HttpMethod;

public class DefaultRouteAutoInjector implements RouteAutoInjector {
    public static final DefaultRouteAutoInjector INSTANCE = new DefaultRouteAutoInjector();

    public Object getValue(Exchange exchange, Class<?> type) {
        if (Exchange.class.isAssignableFrom(type))
            return exchange;
        if (type.equals(HttpMethod.class))
            return exchange.getMethod();
        return null;
    }
}
