package eu.bebendorf.ajwf.handler;

import eu.bebendorf.ajwf.Exchange;

public interface RequestHandler {

    Object handle(Exchange exchange);

}
