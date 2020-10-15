package eu.bebendorf.ajwf.handler;

import eu.bebendorf.ajwf.Exchange;

public interface RequestInterceptor {
    boolean intercept(Exchange exchange);
}