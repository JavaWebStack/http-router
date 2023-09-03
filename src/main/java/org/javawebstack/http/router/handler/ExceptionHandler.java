package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.Exchange;

import java.util.logging.Level;

public interface ExceptionHandler {
    Object handle(Exchange exchange, Throwable ex);

    class DefaultExceptionHandler implements ExceptionHandler {
        public Object handle(Exchange exchange, Throwable ex) {
            exchange.getRouter().getLogger().log(Level.SEVERE, ex, () -> "An internal server error occured!");
            return "An internal server error occured! Please contact the server administrator in case you think this is a problem.";
        }
    }
}