package org.javawebstack.httpserver.transformer.route;

import org.javawebstack.httpserver.Exchange;

public interface RouteParamTransformFunction {
    Object transform(Exchange exchange, Object source);
}
