package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;

import java.util.Map;

public interface RouteAutoInjector {

    Object getValue(Exchange exchange, Map<String, Object> extraArgs, Class<?> type);

}
