package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;

public class DefaultNotFoundHandler implements RequestHandler {
    public Object handle(Exchange exchange) {
        exchange.status(404);
        return "Page not found!";
    }
}
