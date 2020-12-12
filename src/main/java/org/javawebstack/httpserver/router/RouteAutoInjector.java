package org.javawebstack.httpserver.router;

import org.javawebstack.httpserver.Exchange;

public interface RouteAutoInjector {

    Object getValue(Exchange exchange, Class<?> type);

}
