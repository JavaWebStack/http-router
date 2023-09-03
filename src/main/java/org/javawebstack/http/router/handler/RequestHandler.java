package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;

public interface RequestHandler {

    Object handle(Exchange exchange);

}
