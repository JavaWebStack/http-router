package eu.bebendorf.ajwf.handler;

import eu.bebendorf.ajwf.Exchange;

public class DefaultNotFoundHandler implements RequestHandler {
    public Object handle(Exchange exchange) {
        exchange.status(404);
        return "Page not found!";
    }
}
