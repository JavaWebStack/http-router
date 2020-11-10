package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.Exchange;

import java.util.logging.Level;

public interface ExceptionHandler {
    Object handle(Exchange exchange, Throwable ex);
    default byte[] handleBytes(Exchange exchange, Throwable ex){
        return exchange.getServer().transformResponse(handle(exchange, ex));
    }
    class DefaultExceptionHandler implements ExceptionHandler {
        public Object handle(Exchange exchange, Throwable ex) {
            exchange.getServer().getLogger().log(Level.SEVERE, ex, () -> "An internal server error occured!");
            return "An internal server error occured! Please contact the server administrator in case you think this is a problem.";
        }
    }
}