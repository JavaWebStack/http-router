package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;

public interface RequestHandler {

    Object handle(Exchange exchange);

}
