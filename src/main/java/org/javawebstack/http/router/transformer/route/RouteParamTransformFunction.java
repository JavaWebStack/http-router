package org.javawebstack.http.router.transformer.route;

import org.javawebstack.http.router.Exchange;

public interface RouteParamTransformFunction {
    Object transform(Exchange exchange, Object source);
}
