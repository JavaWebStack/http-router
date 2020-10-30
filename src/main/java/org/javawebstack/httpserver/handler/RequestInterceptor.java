package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;

public interface RequestInterceptor {
    boolean intercept(Exchange exchange);
}