package eu.bebendorf.ajwf.handler;

import eu.bebendorf.ajwf.Exchange;

public interface AfterRequestHandler {
    Object handleAfter(Exchange exchange, Object response);
}