package org.javawebstack.httpserver.websocket;

import org.eclipse.jetty.server.Request;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.handler.RequestHandler;
import org.javawebstack.httpserver.handler.WebSocketHandler;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

public class InternalWebSocketRequestHandler implements RequestHandler {
    private final WebSocketHandler handler;
    public InternalWebSocketRequestHandler(WebSocketHandler handler){
        this.handler = handler;
    }
    public Object handle(Exchange exchange) {
        String id = UUID.randomUUID().toString();
        exchange.rawResponse().setHeader("X-Server-WSID", id);
        InternalWebSocketAdapter.webSockets.put(id, new WebSocket(exchange, handler));
        try {
            exchange.getServer().getInternalWebSocketHandler().handle(exchange.getPath(), (Request) exchange.rawRequest(), exchange.rawRequest(), exchange.rawResponse());
        } catch (IOException | ServletException ignored) {
            ignored.printStackTrace();
        }
        return null;
    }
}
