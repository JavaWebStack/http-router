package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;

public interface AfterRequestHandler {
    Object handleAfter(Exchange exchange, Object response);
}