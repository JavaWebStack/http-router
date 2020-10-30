package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;

public class DefaultNotFoundHandler implements RequestHandler {
    public Object handle(Exchange exchange) {
        exchange.status(404);
        return "Page not found!";
    }
}
