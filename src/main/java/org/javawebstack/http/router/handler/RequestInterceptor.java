package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;

public interface RequestInterceptor {
    boolean intercept(Exchange exchange);
}