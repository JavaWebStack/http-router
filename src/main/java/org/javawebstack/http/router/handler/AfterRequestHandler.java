package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;

public interface AfterRequestHandler {
    Object handleAfter(Exchange exchange, Object response);
}