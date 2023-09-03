package org.javawebstack.http.router.router;

import org.javawebstack.http.router.Exchange;

import java.util.Map;

public interface RouteAutoInjector {

    Object getValue(Exchange exchange, Map<String, Object> extraArgs, Class<?> type);

}
